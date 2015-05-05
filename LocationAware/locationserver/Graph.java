/**
 * Implemented by: Emily Whiting
 * Last edited: February 18, 2006
 *
 * Node-edge graph built using triangle adjacencies. Nodes are placed
 * at midpoints of shared triangle edges (aka interior triangle edges).
 * For each portal, a node is placed at its centroid, and is associated
 * with the triangle with the closest edge. Edges connect all nodes
 * incident on the same triangle.
 *
*/

package locationserver;
import java.util.*;
import java.io.*;

public class Graph implements Serializable {

    public Vector edges;
    public Vector nodes;

    public Graph() {}
    public Graph(Space s) {
	edges = new Vector();
	nodes = new Vector();
	buildfromTriangles(s);
    }

    // get closest portal node, does not check source/dest
    public Node getPortalNode(Position p) {
	double minDistance = Double.MAX_VALUE;
	Node answer = null;
	for(int i = 0; i < nodes.size(); i++) {
	    Node curr = (Node)nodes.get(i);
	    if(curr.isPortal()) {
		double distance = curr.pos().distance(p);
		if(distance < minDistance) {
		    minDistance = distance;
		    answer = curr;
		}
	    }
	} return answer;
    }
    
    // get closest portal node with matching source/dest spaces
    public Node getPortalNode(Portal p) {
	Position pos = p.getApproximatePosition();
	double minDistance = Double.MAX_VALUE;
	Node answer = null;

	for(int i = 0; i < nodes.size(); i++) {
	    Node curr = (Node)nodes.get(i);
	    if(curr.isPortal()) {
		Portal currP = curr.getPortal();
		if((currP.spaces[0] == p.spaces[0] &&
		    currP.spaces[1] == p.spaces[1]) ||
	
		   (currP.spaces[0] == p.spaces[1] &&
		    currP.spaces[1] == p.spaces[0])) {

		    double distance = curr.pos().distance(pos);
		    if(distance < minDistance) {
			minDistance = distance;
			answer = curr;
		    }
		}
	    }
	} return answer;
    }

   /**
    * Combines graph G with this graph. Links are made by adding edges
    * between corresponding portal nodes. "s" is the space that G belongs to.
    */
    public void add(Graph G, Space s) {
	for (int i = 0; i < this.nodes.size(); i++) {
	    Node oldNode = (Node)this.nodes.get(i);
	    
	    if(oldNode.isPortal()) {
		// find dest space of this portal
		Portal oldPortal = oldNode.getPortal();
		Space destSpace = oldPortal.spaces[1];

		if(destSpace.equals(s)) {
		    // Add link between matching portals
		    // Note: this means the path traversed portals PAIRS
		    Node newNode = G.getPortalNode(oldPortal);
		    this.edges.add(new Edge(oldNode, newNode));
		}
	    }
	}
	this.nodes.addAll(G.nodes);
	this.edges.addAll(G.edges);
    }

    // convert list of vertex coordinates into triangle objects
    private Vector getTriangles(Space space) {
	double[] triangles = space.getTriangles();
	Vector triangleVec = new Vector();

	for (int i = 0; i < triangles.length; i += 6) {
	    double x0 = triangles[i];   double y0 = triangles[i+1];
	    double x1 = triangles[i+2]; double y1 = triangles[i+3];
	    double x2 = triangles[i+4]; double y2 = triangles[i+5];
	    Triangle nextTriangle = new Triangle(x0, y0, x1, y1, x2, y2,
						 space.getZ());
	    triangleVec.add(nextTriangle);
	}
	return triangleVec;
    }


    // adds node at midpoint of every shared triangle edge
    private void setTriangleNodes(Vector triangleVec) {
	for (int i = 0; i < triangleVec.size(); i++) {
	    // find the triangles adjacent to this one
	    Triangle currTri = (Triangle)triangleVec.get(i);

	    for (int j = i+1; j < triangleVec.size(); j++) {
		Triangle nextTri = (Triangle)triangleVec.get(j);
		if (currTri.commonEdge(nextTri)) {

		    // find the common edge
		    Position point[] = currTri.getcommonEdge(nextTri);
		    //find the midpoint of the shared edge
		    Position midpoint = new Position((point[0].x + point[1].x)/2,
						     (point[0].y + point[1].y)/2,
						     (point[0].z + point[1].z)/2);
		    // add it to the graph
		    Node node = new Node(midpoint);
		    currTri.nodes.add(node);
		    nextTri.nodes.add(node);
		    nodes.add(node);
		}
	    }
	}
    }

    // adds node for every portal
    private void setPortalNodes(Vector portals, Vector triangleVec) {
	for (int i = 0; i < portals.size(); i++) {
	    Portal portal = (Portal)portals.get(i);
	    Position portalPosition = portal.getApproximatePosition();
	    double minDistance = Double.MAX_VALUE;
	    Triangle portalTri = null;

	    // loop finds the triangle closest to the current portal
	    for (int j = 0; j < triangleVec.size(); j++) {
		Triangle nextTriangle = (Triangle)triangleVec.get(j);
		double distance = nextTriangle.closestEdge(portalPosition);
		if (distance < minDistance) {
		    minDistance = distance;
		    portalTri = nextTriangle;
		}		
	    }
	    Node node = new Node(portalPosition);
	    node.tagPortal(portal);
	    portalTri.nodes.add(node);
	    nodes.add(node);
	}
    }

    /* Connects regular (non portal) nodes.
     */
    private void setInteriorEdges(Vector interiorNodes, Space space) {
	if (interiorNodes.size() == 2) {

	    // new edge between two nodes
	    Edge edge = new Edge((Node)interiorNodes.get(0), (Node)interiorNodes.get(1));
	    edges.add(edge);
		
	} else if (interiorNodes.size() > 2) {

	    // each node has an edge to every other node incident on the same triangle
	    for (int j = 0; j < interiorNodes.size(); j++) {
		for (int k = j+1; k < interiorNodes.size(); k++) {
		    edges.add(new Edge((Node)interiorNodes.get(j),
				       (Node)interiorNodes.get(k)));
		}
	    }
	}
    }

    /* Connects each portal node to all other nodes in same triangle (both
     * portal and non-portal)
    */
    private void setPortalEdges(Vector interiorNodes, Vector portalNodes) {
	for (int k = 0; k < portalNodes.size(); k++) { 
	    Node curr = (Node)portalNodes.get(k);
	    
	    // connect to each regular interior node
	    for (int j = 0; j < interiorNodes.size(); j++) {
		Edge portalEdge = new Edge(curr, (Node)interiorNodes.get(j));
		edges.add(portalEdge);
	    }

	    // connect to each portal node
	    for (int m = k+1; m < portalNodes.size(); m++) {
		Edge portalEdge = new Edge(curr, (Node)portalNodes.get(m));
		edges.add(portalEdge);
	    }
	}
    }

    /* Function for building graph from triangles in delaunay
     * triangulation of space. Graph vertices are placed on midpoints of
     * all shared triangles edges and at all portal positions.
     */
    private void buildfromTriangles(Space space) {

	// get list of triangles for the space
	//Vector triangleVec = space.getTriangles();
	Vector triangleVec = getTriangles(space);
	Vector portalVec = (Vector)space.getPortals();

	// add node at midpoint of every shared triangle edge
	// and at every portal
	setTriangleNodes(triangleVec);
	setPortalNodes(portalVec, triangleVec);

	// Graph nodes exist on the midpoints of triangle edges.
	// Now create the edges inside the triangles to connect the nodes.
	//
	for (int i = 0; i < triangleVec.size(); i++) {
	    Triangle currTri = (Triangle)triangleVec.get(i);
	    Vector interiorNodes = new Vector();
	    Vector portalNodes = new Vector();

	    for (int j = 0; j < currTri.nodes.size(); j++) {
		Node currNode = (Node)currTri.nodes.get(j);
		if(currNode.isPortal())
		    portalNodes.add(currNode);
		else 
		    interiorNodes.add(currNode);
	    }
	    setInteriorEdges(interiorNodes, space);
	    setPortalEdges(interiorNodes, portalNodes);
	}
    }

    ///////////////////////////////////////////////////////////
    // HELPER CLASSES FOR GRAPH ADT
    ///////////////////////////////////////////////////////////

    public class Edge implements Serializable {
	private Node nodeA;
	private Node nodeB;
	private double weight;

	public Edge() {}
	public Edge(Node a, Node b) {
	    nodeA = a;
	    nodeB = b;
	    nodeA.addEdge(this);
	    nodeB.addEdge(this);
	    weight = a.pos().distance(b.pos());
	}
	public double weight() { return weight; }
	public Node A() { return nodeA; }
	public Node B() { return nodeB; }
    }
    
    public class Node implements Serializable {
	private Portal portal;
	private boolean portalTag;
	private Position position;
	private Vector adjEdges;
	
	public Node() {}
	public Node(Position p) {
	    portalTag = false;
	    portal = null;
	    position = p;
	    adjEdges = new Vector();
	}
	public Position pos() { return position; }
	public void tagPortal(Portal p) {
	    portalTag = true;
	    portal = p;
	}
	public boolean isPortal() { return portalTag; }
	public Portal getPortal() { return portal; }
	public void addEdge(Edge e) { adjEdges.add(e); }
	public Vector getEdges() { return adjEdges; }
	public Vector getAdjNodesWithWeights() {
	    Vector answer = new Vector();
	    for(int i = 0; i < adjEdges.size(); i++) {
		Object[] object = new Object[2];
		Edge E = (Edge)adjEdges.get(i);
		object[0] = new Double(E.weight());
		if(this == E.A()) object[1] = E.B();
		else object[1] = E.A();
		answer.add(object);
	    }
	    return answer;
	}
	public Vector getAdjNodes() {
	    Vector answer = new Vector();
	    for(int i = 0; i < adjEdges.size(); i++) {
		Edge E = (Edge)adjEdges.get(i);
		if(this == E.A()) answer.add(E.B());
		else answer.add(E.A());
	    } return answer;
	}
    }

    // ========================================================
}
