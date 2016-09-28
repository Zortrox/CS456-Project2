/*
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

class Node {
	private ArrayList<Node> adjList = new ArrayList<>();
	private int xGrid;
	private int yGrid;
	private boolean visited = false;
	private Node parent = null;

	//A* variables
	private int gCost = 0;
	private int hCost = 0;

	Node(int x, int y) {
		xGrid = x;
		yGrid = y;
	}

	void addNode(Node node) {
		adjList.add(node);
	}

	void setVisited(boolean v) {
		visited = v;
	}

	boolean getVisited() {
		return visited;
	}

	void setParent(Node p) {
		parent = p;
	}

	Node getParent() {
		return parent;
	}

	ArrayList<Node> getAdjacentNodes() {
		return adjList;
	}

	int getX() {
		return xGrid;
	}

	int getY() {
		return yGrid;
	}

	//A* functions
	void setGCost(int g) {
		gCost = g;
	}

	int getGCost() {
		return gCost;
	}

	void setHCost(int h) {
		hCost = h;
	}

	int getFCost() {
		return hCost + gCost;
	}
}

class State {
	private Point locHupman = null;
	private ArrayList<Point> locAllGhosts = new ArrayList<>();
	private ArrayList<Point> locAllPellets = new ArrayList<>();
	private int turnNum = 0;

	State() {}

	State(Point locH, ArrayList<Point> locAG, ArrayList<Point> locAP, int turn) {
		locHupman = locH;
		locAllGhosts = locAG;
		locAllPellets = locAP;
		turnNum = turn;
	}

	public void setHupmanLocation(Point newPos) {
		locHupman = newPos;
	}
	public Point getHupmanLocation() {
		return locHupman;
	}

	public void addGhost(Point ghostPoint) {
		locAllGhosts.add(ghostPoint);
	}
	public void setGhostLocation(Point newPos, int ghostNum) {
		locAllGhosts.set(ghostNum, newPos);
	}
	public ArrayList<Point> getGhostLocations() {
		return locAllGhosts;
	}

	public void clearPellets() {
		locAllPellets.clear();
	}
	public void addPellet(Point pelletPoint) {
		locAllPellets.add(pelletPoint);
	}
	public void removePellet(Point pelletPoint) {
		for (int i = 0; i < locAllPellets.size(); i++) {
			if (locAllPellets.get(i) == pelletPoint) {
				locAllPellets.remove(i);
				i = locAllPellets.size();
			}
		}
	}
	public ArrayList<Point> getPelletLocations() {
		return locAllPellets;
	}

	public void setTurn(int newTurn) {
		turnNum = newTurn;
	}
	public int getTurn() {
		return turnNum;
	}
}

public class Hupman extends JPanel{

	private int numRows, numCols, numPellets;
	private int[][] arrMaze;
	private int[][] arrPellets;
	private Map<String, Node> mapNodes = new HashMap<>();
	private boolean bFileRead = false;
	private int windowWidth = 600;
	private int windowHeight = 600;
	private int gridOffset = 100;
	private int gridSize = 20;
	private int pelletRadius = gridSize / 5;
	private int hupmanRadius = gridSize / 3;
	private State currentState = new State();

	Hupman() {
		loadFile();
		createGraph();
		resetPelletNodes();

		setPreferredSize(new Dimension(windowWidth, windowHeight));

		repaint();

		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				Point mousePos = e.getPoint();

				int gridX = (mousePos.x - gridOffset) / gridSize;
				int gridY = (mousePos.y - gridOffset) / gridSize;

				if (gridX >= 0 && gridX < numCols && gridY >= 0 && gridY < numRows) {
					if (e.getButton() == MouseEvent.BUTTON1) {
						currentState.setHupmanLocation(new Point(gridX, gridY));
						paintImmediately(0, 0, windowHeight, windowWidth);
					}
					else if (e.getButton() == MouseEvent.BUTTON3) {
						currentState.addGhost(new Point(gridX, gridY));
						paintImmediately(0, 0, windowHeight, windowWidth);
					}
				}
			}
		});
	}

	public void start() {
		//set initial pellet positions
		resetPelletNodes();
		resetVisitedNodes();

		takeTurn(currentState);
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		//g2.clearRect(0, 0, windowWidth, windowHeight);

		g.setColor(Color.BLACK);
		g.drawRect(gridOffset, gridOffset, gridSize * arrMaze[0].length, gridSize * arrMaze.length);
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				int xPos = j * gridSize + gridOffset;
				int yPos = i * gridSize + gridOffset;
				if (arrMaze[i][j] == 1 || arrMaze[i][j] == 3) {
					g.drawLine(xPos, yPos, xPos + gridSize, yPos);
				}
				if (arrMaze[i][j] == 2 || arrMaze[i][j] == 3) {
					g.drawLine(xPos + gridSize, yPos, xPos + gridSize, yPos + gridSize);
				}
			}
		}

		ArrayList<Point> locAllPellets = currentState.getPelletLocations();
		if (locAllPellets.size() > 0) {
			g.setColor(Color.red);
			for (int i = 0; i < locAllPellets.size(); i++) {
				int xPos = (int)locAllPellets.get(i).getX() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				int yPos = (int)locAllPellets.get(i).getY() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				g.fillOval(xPos, yPos, pelletRadius * 2, pelletRadius * 2);
			}
		}

		Point locHupman = currentState.getHupmanLocation();
		if (locHupman != null) {
			g.setColor(Color.orange);
			int xPos = (int)locHupman.getX() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
			int yPos = (int)locHupman.getY() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
			g.fillOval(xPos, yPos, hupmanRadius * 2, hupmanRadius * 2);
		}

		ArrayList<Point> locAllGhosts = currentState.getGhostLocations();
		if (locAllGhosts.size() > 0) {
			g.setColor(Color.blue);
			for (int i = 0; i < locAllGhosts.size(); i++) {
				int xPos = (int)locAllGhosts.get(i).getX() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
				int yPos = (int)locAllGhosts.get(i).getY() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
				g.fillOval(xPos, yPos, hupmanRadius * 2, hupmanRadius * 2);
			}
		}
	}

	private void loadFile() {
		String filename;
		BufferedReader reader = null;
		do {
			filename = JOptionPane.showInputDialog("Enter maze filename: ");

			try {
				String[] comp;
				reader = new BufferedReader(new FileReader(filename));

				//create the maze array
				comp = reader.readLine().split("\\s+");
				numRows = Integer.parseInt(comp[0]);
				numCols = Integer.parseInt(comp[1]);

				int scaleFactor = Math.max(numRows, numCols);
				int limitingFactor = Math.min(windowHeight, windowWidth);
				gridSize = (limitingFactor - 2 * gridOffset) / scaleFactor;
				pelletRadius = gridSize / 5;
				hupmanRadius = gridSize / 3;

				arrMaze = new int[numRows][numCols];
				//wall types at grid positions
				for (int i = 0; i < numRows; i++) {
					comp = reader.readLine().split("\\s+");
					for (int j = 0; j < numCols; j++) {
						arrMaze[i][j] = Integer.parseInt(comp[j]);
					}
				}

				//remove empty lines
				while ((comp = reader.readLine().split("\\s+")).length == 0);

				numPellets = Integer.parseInt(comp[0]);
				arrPellets = new int[numPellets][2];
				for (int i = 0; i < numPellets; i++) {
					comp = reader.readLine().split("\\s+");
					arrPellets[i][0] = Integer.parseInt(comp[1]);	//col as x
					arrPellets[i][1] = Integer.parseInt(comp[0]);	//row as y
				}

				bFileRead = true;
			} catch (IOException ex) {
				System.out.println("This file doesn't exist.  Choose a different file.");
				//ex.printStackTrace();
			} finally {
				try {
					if (reader != null) reader.close();
				} catch (IOException ex) {
					//ex.printStackTrace();
				}
			}
		}
		while (!bFileRead);
	}

	private void createGraph() {
		addNode(new Node(0, 0), 0, 0);
	}

	private void addNode(Node thisNode, int currX, int currY) {
		mapNodes.put(currX + "-" + currY, thisNode);
		int wallType = arrMaze[currY][currX];

		//ADD CHILDREN
		//left
		if (currX > 0 && arrMaze[currY][currX-1] != 2 && arrMaze[currY][currX-1] != 3) {
			String sKey = (currX-1) + "-" + currY;
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX - 1, currY);
				addNode(tempNode, currX - 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//right
		if (currX < numCols - 1 && wallType != 2 && wallType != 3) {
			String sKey = (currX+1) + "-" + currY;
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX + 1, currY);
				addNode(tempNode, currX + 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//up
		if (currY > 0 && wallType != 1 && wallType != 3) {
			String sKey = currX + "-" + (currY-1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX, currY - 1);
				addNode(tempNode, currX, currY - 1);
			}

			thisNode.addNode(tempNode);
		}
		//down
		if (currY < numRows - 1 && arrMaze[currY+1][currX] != 1 && arrMaze[currY+1][currX] != 3) {
			String sKey = currX + "-" + (currY+1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(currX, currY + 1);
				addNode(tempNode, currX, currY + 1);
			}

			thisNode.addNode(tempNode);
		}
	}

	private void resetPelletNodes() {
		currentState.clearPellets();
		for (int i = 0; i<numPellets; i++) {
			int x = arrPellets[i][0];
			int y = arrPellets[i][1];
			currentState.addPellet(new Point(x, y));
		}
	}

	private void resetVisitedNodes() {
		for (Map.Entry<String, Node> entry : mapNodes.entrySet())
		{
			entry.getValue().setVisited(false);
			entry.getValue().setParent(null);
		}
	}

	//get reverse path from end node to initial node
	private ArrayList<Node> reversePath(Node endNode, Node initNode) {
		ArrayList<Node> path = new ArrayList<>();

		Node tempNode = endNode;
		do {
			path.add(tempNode);
			tempNode = tempNode.getParent();
		}
		while (tempNode != null && tempNode != initNode);

		Collections.reverse(path);
		return path;
	}

	//finds path to target node
	private ArrayList<Node> getPath(Node start, Node target) {
		ArrayList<Node> path = new ArrayList<>();
		ArrayList<Node> openList = new ArrayList<Node>();
		openList.add(start); //add starting node to open list

		Node thisNode = null;

		boolean done = false;
		while (!done) {
			thisNode = null;

			//get node with lowest f cost from openList
			int lowestF = -1;
			for (int i = 0; i < openList.size(); i++) {
				int newF = openList.get(i).getFCost();
				if (lowestF < 0 || newF < lowestF) {
					lowestF = newF;
					thisNode = openList.get(i);
				}
			}

			thisNode.setVisited(true);
			openList.remove(thisNode); //delete current node from open list

			//found goal
			if (thisNode == target) {
				//get path to node + ending node
				done = true;
				path = reversePath(thisNode, start);
			}

			//continue if node wasn't the goal node
			if (!done) {
				//for all adjacent nodes:
				ArrayList<Node> arrAdj = thisNode.getAdjacentNodes();
				for (int i = 0; i < arrAdj.size(); i++) {
					Node nodeAdj = arrAdj.get(i);

					//if not in the openList, add it
					if (!openList.contains(nodeAdj) && !nodeAdj.getVisited()) {
						int hCost = Math.abs(nodeAdj.getX() - target.getX()) + Math.abs(nodeAdj.getY() - target.getY());

						nodeAdj.setParent(thisNode);
						nodeAdj.setHCost(hCost);
						nodeAdj.setGCost(thisNode.getGCost() + 1);
						openList.add(nodeAdj);


					}
					//else if costs are cheaper, keep it open and lower g cost
					else {
						if (nodeAdj.getGCost() > thisNode.getGCost() + 1) {
							nodeAdj.setVisited(false);
							nodeAdj.setParent(thisNode);
							nodeAdj.setGCost(thisNode.getGCost() + 1);
							openList.add(nodeAdj);
						}
					}
				}

				//no path exists
				if (openList.isEmpty()) {
					done = true;
					path = null;
				}
			}
		}

		//reset visited nodes
		resetVisitedNodes();

		return path;
	}

	public State minimax(State testState, int depth) {

		return new State();
	}

	public void takeTurn(State currState) {
		currState = minimax(currState, 5);
	}

	public static void main(String[] args) {
		JFrame frame = new JFrame("Hupman");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(200, 200);
		frame.setVisible(true);

		Hupman hup = new Hupman();
		frame.getContentPane().add(hup);

		JButton btnStart = new JButton("Start");
		btnStart .addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				hup.start();
			}
		});
		frame.add(btnStart, BorderLayout.NORTH);
		btnStart.setVisible(true);

		frame.pack();
	}
}
