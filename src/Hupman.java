/*
 * Matthew Clark
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.sqrt;

class Node {
	private ArrayList<Node> adjList = new ArrayList<>();
	private Point nodePos;
	private boolean visited = false;
	private Node parent = null;

	//A* variables
	private int gCost = 0;
	private int hCost = 0;

	Node(Point pos) {
		nodePos = pos;
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

	Point getPos() {
		return nodePos;
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
	private double weight = 0;
	private boolean dead = false;
	private int uneatenSteps = 0;
	private int totalSteps = 0;

	State() {}

	State(State oldState) {
		locHupman = new Point(oldState.getHupmanLocation());
		locAllGhosts = new ArrayList<>(oldState.getGhostLocations());
		locAllPellets = new ArrayList<>(oldState.getPelletLocations());
		turnNum = oldState.getTurn();
		dead = oldState.getDead();
		uneatenSteps = oldState.getUneatenSteps();
		totalSteps = oldState.getSteps();
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
	public void removeGhost(int index) {
		locAllGhosts.remove(index);
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
			if (locAllPellets.get(i).equals(pelletPoint)) {
				locAllPellets.remove(i);
				i = locAllPellets.size();
			}
		}
	}
	public ArrayList<Point> getPelletLocations() {
		return locAllPellets;
	}

	public void nextTurn() {
		turnNum++;
		if (turnNum > locAllGhosts.size()) {
			turnNum = 0;
		}
	}
	public void setTurn(int newTurn) {
		turnNum = newTurn;
	}
	public int getTurn() {
		return turnNum;
	}
	public int getPrevTurn() {
		if (turnNum - 1 < 0) {
			return locAllGhosts.size();
		} else {
			return turnNum - 1;
		}
	}

	public void setWeight(double newWeight) {
		weight = newWeight;
	}
	public double getWeight() {
		return weight;
	}

	public void setDead(boolean isDead) {
		dead = isDead;
	}
	public boolean getDead() {
		return dead;
	}

	public void setUneatenSteps(int steps) {
		uneatenSteps = steps;
	}
	public int getUneatenSteps() {
		return uneatenSteps;
	}

	public void resetSteps() {
		totalSteps = 0;
	}
	public void addStep() {
		totalSteps++;
	}
	public int getSteps() {
		return totalSteps;
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
	long timePrev = 0;

	//score
	private int totalSteps = 0;
	private int pelletsEaten = 0;

	//weight constants
	private static final int WT_HAS_PELLET 		= 0;
	private static final int WT_HAS_GHOST 		= 1;
	private static final int WT_DIST_PELLETS 	= 2;
	private static final int WT_DIST_GHOSTS 	= 3;
	private static final int WT_TOTAL_STEPS 	= 4;

	Hupman() {
		loadFile();
		createGraph();
		resetPelletNodes();

		setPreferredSize(new Dimension(windowWidth, windowHeight));

		repaint();

		//set dead so we can start the game
		currentState.setDead(true);

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
						ArrayList<Point> locGhosts = currentState.getGhostLocations();
						boolean ghostThere = false;
						Point gridPoint = new Point(gridX, gridY);
						for (int i = 0; i < locGhosts.size(); i++) {
							if (locGhosts.get(i).equals(gridPoint)) {
								//break loop and remove ghost instead of adding
								currentState.removeGhost(i);
								ghostThere = true;
								break;
							}
						}

						//if ghost not there, add ghost
						if (!ghostThere) {
							currentState.addGhost(gridPoint);
						}

						//repaint ghost removal or addition
						paintImmediately(0, 0, windowHeight, windowWidth);
					}
				}
			}
		});
	}

	public void start() {
		//only run if the game was over
		if (currentState.getDead()) {
			//set initial pellet positions
			resetPelletNodes();
			resetVisitedNodes();
			totalSteps = 0;

			//reset current state
			currentState.setDead(false);
			currentState.setUneatenSteps(0);
			currentState.resetSteps();
			currentState.setWeight(0);

			Object[] options = {"Function 1",
					"Function 2"};
			int func = JOptionPane.showOptionDialog(this, "Which evaluation function do you want to use?", "Evaluation Function",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			//start in new thread to enable clicking on GUI exit
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					State testState = new State();
					double ghostChance = 1.0;
					int depth = 10;
					int pelletsLeft = currentState.getPelletLocations().size();
					while ((testState = takeTurn(currentState, depth, func, ghostChance)) != null &&
							(pelletsLeft = currentState.getPelletLocations().size()) != 0) {
						//set the current state to this state
						currentState = testState;

						//paint new state
						paintImmediately(0, 0, windowWidth, windowHeight);

						//wait before taking next turn
						try {
							long timeNow = System.currentTimeMillis() % 1000;
							long sleepTime = (200 - (timeNow - timePrev)) / (currentState.getGhostLocations().size() + 1);
							if (sleepTime > 0) Thread.sleep(sleepTime);
						} catch (Exception ex) {
							ex.printStackTrace();
						}
						timePrev = System.currentTimeMillis() % 1000;
					}

					//GAME HAS ENDED
					//update scores
					totalSteps = currentState.getSteps();
					pelletsEaten = numPellets - pelletsLeft;

					//print if hupman won or died
					if (pelletsLeft == 0) {
						System.out.println("Hupman won!");
					} else {
						System.out.println("Hupman died!");
					}

					//print score
					System.out.println("Score:\n\tPellets: " + pelletsEaten + "\n\tSteps: " + totalSteps + "\n");

					//set game to ended
					currentState.setDead(true);
				}
			});
			t.start();
		}
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);

		Graphics2D g2 = (Graphics2D) g;

		g.setColor(Color.BLACK);
		g.fillRect(0, 0, windowWidth, windowHeight);

		BasicStroke wall = new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

		g.setColor(Color.BLUE);
		g2.setStroke(wall);
		g2.draw(new Rectangle2D.Float(gridOffset, gridOffset, gridSize * arrMaze[0].length, gridSize * arrMaze.length));
		for (int i = 0; i < numRows; i++) {
			for (int j = 0; j < numCols; j++) {
				int xPos = j * gridSize + gridOffset;
				int yPos = i * gridSize + gridOffset;
				if (arrMaze[i][j] == 1 || arrMaze[i][j] == 3) {
					g2.draw(new Line2D.Float(xPos, yPos, xPos + gridSize, yPos));
				}
				if (arrMaze[i][j] == 2 || arrMaze[i][j] == 3) {
					g2.draw(new Line2D.Float(xPos + gridSize, yPos, xPos + gridSize, yPos + gridSize));
				}
			}
		}

		ArrayList<Point> locAllPellets = currentState.getPelletLocations();
		if (locAllPellets.size() > 0) {
			g.setColor(Color.WHITE);
			for (int i = 0; i < locAllPellets.size(); i++) {
				int xPos = (int)locAllPellets.get(i).getX() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				int yPos = (int)locAllPellets.get(i).getY() * gridSize + gridSize / 2 - pelletRadius + gridOffset;
				g.fillOval(xPos, yPos, pelletRadius * 2, pelletRadius * 2);
			}
		}

		Point locHupman = currentState.getHupmanLocation();
		if (locHupman != null) {
			g.setColor(Color.YELLOW);
			int xPos = (int)locHupman.getX() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
			int yPos = (int)locHupman.getY() * gridSize + gridSize / 2 - hupmanRadius + gridOffset;
			g.fillOval(xPos, yPos, hupmanRadius * 2, hupmanRadius * 2);
		}

		ArrayList<Point> locAllGhosts = currentState.getGhostLocations();
		if (locAllGhosts.size() > 0) {
			g.setColor(Color.RED);
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
		addNode(new Node(new Point(0, 0)), 0, 0);
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
				tempNode = new Node(new Point(currX - 1, currY));
				addNode(tempNode, currX - 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//right
		if (currX < numCols - 1 && wallType != 2 && wallType != 3) {
			String sKey = (currX+1) + "-" + currY;
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(new Point(currX + 1, currY));
				addNode(tempNode, currX + 1, currY);
			}

			thisNode.addNode(tempNode);
		}
		//up
		if (currY > 0 && wallType != 1 && wallType != 3) {
			String sKey = currX + "-" + (currY-1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(new Point(currX, currY - 1));
				addNode(tempNode, currX, currY - 1);
			}

			thisNode.addNode(tempNode);
		}
		//down
		if (currY < numRows - 1 && arrMaze[currY+1][currX] != 1 && arrMaze[currY+1][currX] != 3) {
			String sKey = currX + "-" + (currY+1);
			Node tempNode = mapNodes.get(sKey);
			if (tempNode == null) {
				tempNode = new Node(new Point(currX, currY + 1));
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
		ArrayList<Node> openList = new ArrayList<>();
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
						Point adjPos = nodeAdj.getPos();
						Point targetPos = target.getPos();
						int hCost = Math.abs(adjPos.x - targetPos.x) + Math.abs(adjPos.y - targetPos.y);

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

	private double getWeightOne(State testState, int weightType) {
		double weight = 0;

		Point locHupman = testState.getHupmanLocation();

		if (weightType == WT_HAS_PELLET) {
			ArrayList<Point> locPellets = testState.getPelletLocations();
			for (int j = 0; j < locPellets.size(); j++) {
				if (locPellets.get(j).equals(locHupman)) {
					weight += 100.0 * (1.0 / locPellets.size());
					testState.setUneatenSteps(0);
					testState.removePellet(locPellets.get(j));
				}
			}
		} else if (weightType == WT_HAS_GHOST) {
			ArrayList<Point> locGhosts = testState.getGhostLocations();
			for (int j = 0; j < locGhosts.size(); j++) {
				if (locGhosts.get(j).equals(locHupman)) {
					weight -= 500.0 / testState.getSteps();
					testState.setDead(true);
				}
			}
		} else if (weightType == WT_DIST_PELLETS) {
			Node startNode = mapNodes.get(locHupman.x + "-" + locHupman.y);
			ArrayList<Point> locPellets = testState.getPelletLocations();
			for (int i = 0; i < locPellets.size(); i++) {
				Point locPellet = locPellets.get(i);
				Node endNode = mapNodes.get(locPellet.x + "-" + locPellet.y);
				ArrayList<Node> path = getPath(startNode, endNode);

				weight += 5.0 / Math.pow(path.size(), 2) * Math.pow(testState.getSteps(), 1.4)
						* (1.0 / locPellets.size());
			}
		} else if (weightType == WT_DIST_GHOSTS) {
			Node startNode = mapNodes.get(locHupman.x + "-" + locHupman.y);
			ArrayList<Point> locGhosts = testState.getGhostLocations();
			for (int i = 0; i < locGhosts.size(); i++) {
				Point locGhost = locGhosts.get(i);
				Node endNode = mapNodes.get(locGhost.x + "-" + locGhost.y);
				ArrayList<Node> path = getPath(startNode, endNode);
				weight -= 50.0 / Math.pow(path.size(), 1.4);
			}
		} else if (weightType == WT_TOTAL_STEPS) {
			weight -= (testState.getPrevTurn() == 0) ? Math.pow(testState.getSteps(), 1.5) : 0;
		}

		return weight;
	}

	private double getWeightTwo(State testState, int weightType) {
		double weight = 0;

		Point locHupman = testState.getHupmanLocation();

		if (weightType == WT_HAS_PELLET) {
			ArrayList<Point> locPellets = testState.getPelletLocations();
			for (int j = 0; j < locPellets.size(); j++) {
				if (locPellets.get(j).equals(locHupman)) {
					weight += 2000.0 / locPellets.size() * Math.sqrt(testState.getUneatenSteps());
					testState.setUneatenSteps(0);
					testState.removePellet(locPellets.get(j));
				}
			}
		} else if (weightType == WT_HAS_GHOST) {
			ArrayList<Point> locGhosts = testState.getGhostLocations();
			for (int j = 0; j < locGhosts.size(); j++) {
				if (locGhosts.get(j).equals(locHupman)) {
					weight -= 1000.0 * (testState.getPrevTurn() == 0 ? Math.floor(1.0 / Math.max(testState.getUneatenSteps()
							* 2 * testState.getSteps(), 1)) : 1.0);
					testState.setDead(true);
				}
			}
		} else if (weightType == WT_DIST_PELLETS) {
			Node startNode = mapNodes.get(locHupman.x + "-" + locHupman.y);
			ArrayList<Point> locPellets = testState.getPelletLocations();
			for (int i = 0; i < locPellets.size(); i++) {
				Point locPellet = locPellets.get(i);
				Node endNode = mapNodes.get(locPellet.x + "-" + locPellet.y);
				ArrayList<Node> path = getPath(startNode, endNode);

				weight += 2000.0 / locPellets.size() / Math.pow(path.size(), 2) * Math.sqrt(testState.getUneatenSteps());
			}
		} else if (weightType == WT_DIST_GHOSTS) {
			Node startNode = mapNodes.get(locHupman.x + "-" + locHupman.y);
			ArrayList<Point> locGhosts = testState.getGhostLocations();
			for (int i = 0; i < locGhosts.size(); i++) {
				Point locGhost = locGhosts.get(i);
				Node endNode = mapNodes.get(locGhost.x + "-" + locGhost.y);
				ArrayList<Node> path = getPath(startNode, endNode);
				double power = testState.getUneatenSteps() * 2;
				weight -= 250.0 * (testState.getPrevTurn() == 0 ? Math.floor(1.0 / Math.pow(path.size(), Math.sqrt(power)))
						: 1.0 / Math.pow(path.size(), 1.4));
			}
		} else if (weightType == WT_TOTAL_STEPS) {
			//weight -= (testState.getPrevTurn() == 0) ? Math.pow(testState.getSteps(), 1.5) : 0;
		}

		return weight;
	}

	private State min(ArrayList<State> arrStates, double minProb) {
		int minIndex = 0;
		for (int i = 1; i < arrStates.size(); i++) {
			if (arrStates.get(i).getWeight() < arrStates.get(minIndex).getWeight()) {
				minIndex = i;
			}
		}

		//randomize the minIndex
		double rand = Math.random();
		if (rand >= minProb) {
			double width = (1.0 - minProb) / (arrStates.size() - 1);
			for (int i = 0; i < arrStates.size() - 1; i++) {
				if ((minProb + (i+1) * width) > rand) {
					//set new state index (taking into account if it's the actual "best")
					if (minIndex == i) {
						minIndex = i + 1;
					} else {
						minIndex = i;
					}

					//break the loop
					i = arrStates.size();
				}
			}
		}

		return arrStates.get(minIndex);
	}

	private State max(ArrayList<State> arrStates) {
		State maxState = arrStates.get(0);
		for (int i = 1; i < arrStates.size(); i++) {
			if (arrStates.get(i).getWeight() > maxState.getWeight()) {
				maxState = arrStates.get(i);
			}
		}

		return maxState;
	}

	private State minimax(State testState, int depth, boolean doMax, double minProb, int evalFunction) {
		int turn = testState.getTurn();
		State weightState = null;

		Point testPos = null;
		if (doMax) {
			testPos = testState.getHupmanLocation();
		} else {
			testPos = testState.getGhostLocations().get(turn - 1);
		}
		String key = testPos.x + "-" + testPos.y;
		Node testNode = mapNodes.get(key);

		ArrayList<State> subStates = new ArrayList<>();

		if (depth > 0) {
			for (int i = 0; i < testNode.getAdjacentNodes().size(); i++) {
				Node adjNode = testNode.getAdjacentNodes().get(i);
				State adjState = new State(testState);

				if (turn == 0) {
					adjState.setHupmanLocation(adjNode.getPos());
					adjState.addStep();
					adjState.setUneatenSteps(adjState.getUneatenSteps() + 1);
				} else {
					adjState.setGhostLocation(adjNode.getPos(), turn - 1);
				}

				//decrease depth for hupman turn & ghost turn, but not ALL ghost turns
				int nextDepth = depth;
				if (turn == 0 || turn == 1) {
					nextDepth -= 1;
				}

				//make the next state another player's turn & get weights of all their subnodes
				adjState.nextTurn();
				subStates.add(minimax(adjState, nextDepth, adjState.getTurn() == 0, minProb, evalFunction));

				//update weights for this node
				double weight = 0;
				if (evalFunction == 1) {
					weight += getWeightOne(adjState, WT_HAS_PELLET);	//if state has pellet
					weight += getWeightOne(adjState, WT_HAS_GHOST);		//if state has ghost
				} else if (evalFunction == 2) {
					weight += getWeightTwo(adjState, WT_HAS_PELLET);	//if state has pellet
					weight += getWeightTwo(adjState, WT_HAS_GHOST);		//if state has ghost
				}

				adjState.setWeight(adjState.getWeight() + weight);
			}

			if (doMax) weightState = max(subStates);
			else {
				//hupman always thinks the ghost will choose the best
				double hupmanProb = (currentState.getTurn() == 0) ? 1.0 : minProb;
				weightState = min(subStates, hupmanProb);
			}
		}
		else {
			weightState = new State(testState);

			if (turn == 0) {
				weightState.setHupmanLocation(testNode.getPos());
				weightState.addStep();
				weightState.setUneatenSteps(weightState.getUneatenSteps() + 1);
			} else {
				weightState.setGhostLocation(testNode.getPos(), turn - 1);
			}

			//get weights of final node
			float weight = 0;
			if (evalFunction == 1) {
				weight += getWeightOne(weightState, WT_HAS_PELLET);		//if state has pellet
				weight += getWeightOne(weightState, WT_HAS_GHOST);		//distance to other pellets2
				weight += getWeightOne(weightState, WT_DIST_PELLETS);	//if state has ghost
				weight += getWeightOne(weightState, WT_DIST_GHOSTS);	//distance to other ghosts
				weight += getWeightOne(weightState, WT_TOTAL_STEPS);	//distance to other ghosts
			} else if (evalFunction == 2) {
				weight += getWeightTwo(weightState, WT_HAS_PELLET);		//if state has pellet
				weight += getWeightTwo(weightState, WT_HAS_GHOST);		//distance to other pellets2
				weight += getWeightTwo(weightState, WT_DIST_PELLETS);	//if state has ghost
				weight += getWeightTwo(weightState, WT_DIST_GHOSTS);	//distance to other ghosts
				weight += getWeightTwo(weightState, WT_TOTAL_STEPS);	//distance to other ghosts
			}

			weightState.setWeight(weight);
		}

		if (testState == currentState) {
			return weightState;
		} else {
			testState.setWeight(weightState.getWeight());
			return testState;
		}
	}

	//SUCCESSOR FUNCTION
	//this function takes the current state and applies minimax to it, which returns the state at which
	//hupman has the "best" chance for a better score.
	//"func" is the evaluation function to use (0 or 1)
	//"ghostChance" is the chance of the ghosts using the "best" move
	private State takeTurn(State thisState, int depth, int func, double ghostChance) {
		//whether to maximize or minimize the
		boolean doMax = (currentState.getTurn() == 0);

		//get next hupman/ghost states
		State testState = new State(currentState);
		testState = minimax(currentState, depth, doMax, ghostChance, func + 1);

		//return null is the game has ended (dead or eaten all pellets)
		if (testState.getDead()) {
			return null;
		} else {
			return testState;
		}
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
