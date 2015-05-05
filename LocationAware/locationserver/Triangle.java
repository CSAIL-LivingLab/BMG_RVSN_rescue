package locationserver;
import java.util.*;
import java.io.*;

public class Triangle implements Serializable {
    public Vector nodes;
    final public Position p1, p2, p3;
    final double z;

    Triangle(double x0, double y0, 
	     double x1, double y1, 
	     double x2, double y2,
	     double z) { // we need to include a z value!
	this.z = z;
	this.p1 = new Position(x0, y0, z);
	this.p2 = new Position(x1, y1, z);
	this.p3 = new Position(x2, y2, z);
	nodes = new Vector();
    }
    
    boolean commonEdge(Triangle t) {
	if (this.p1.equals(t.p1) &&
	    (this.p2.equals(t.p2) || this.p3.equals(t.p3) ||
	     this.p3.equals(t.p2) || this.p2.equals(t.p3)))
	    return true;
	if (this.p1.equals(t.p2) &&
	    (this.p2.equals(t.p1) || this.p2.equals(t.p3) ||
	     this.p3.equals(t.p1) || this.p3.equals(t.p3)))
	    return true;
	if (this.p1.equals(t.p3) &&
	    (this.p2.equals(t.p1) || this.p2.equals(t.p2) ||
	     this.p3.equals(t.p1) || this.p3.equals(t.p2)))
	    return true;
	if (this.p2.equals(t.p1) &&
	    (this.p1.equals(t.p2) || this.p1.equals(t.p3) ||
	     this.p3.equals(t.p2) || this.p3.equals(t.p3)))
	    return true;
	if (this.p2.equals(t.p2) &&
	    (this.p1.equals(t.p1) || this.p1.equals(t.p3) ||
	     this.p3.equals(t.p1) || this.p3.equals(t.p3)))
	    return true;
	if (this.p2.equals(t.p3) &&
	    (this.p1.equals(t.p1) || this.p1.equals(t.p2) ||
	     this.p3.equals(t.p1) || this.p3.equals(t.p2)))
	    return true;
	else
	    return false;
    }
    
    public Position[] getcommonEdge(Triangle t) {
	// assumes common edge exists
	Position point[] = new Position[2];
	
	if (this.p1.equals(t.p1) ||
	    this.p1.equals(t.p2) ||
	    this.p1.equals(t.p3)) {
	    point[0] = this.p1;
	    if (this.p2.equals(t.p1) ||
		this.p2.equals(t.p2) ||
		this.p2.equals(t.p3))
		point[1] = this.p2;
	    else
		point[1] = this.p3;
	} else {
	    point[0] = this.p2;
	    point[1] = this.p3;
	}
	return point;
    }

    // distance from line(a,b) to point
    double distance(Position a, Position b, Position point) {
	double t, denom, diffX, diffY;
	
	t = (point.x - a.x)*(b.x - a.x) + (point.y - a.y)*(b.y - a.y); // dot product
	denom = (Math.pow((b.x - a.x),2) + Math.pow((b.y - a.y),2)); // line length squared
	
	if(denom > 0) t = t/denom; // point projected on line(a,b) = a + t(b-a)
	else t = 0;
	
	if(t < 0) {
	    diffX = a.x - point.x;
	    diffY = a.y - point.y;
	} else if(t > 1) {
	    diffX = b.x - point.x;
	    diffY = b.y - point.y;
	} else {
	    diffX = (a.x + t*(b.x-a.x)) - point.x;
	    diffY = (a.y + t*(b.y-a.y)) - point.y;
	}
	return (diffX*diffX + diffY*diffY);
    }
    
    public double closestEdge(Position point) {
	double distance;
	double minDistance = Double.MAX_VALUE;
	Position[] trVerts = {p1, p2, p3};
	
	for(int j = 0; j < 3; j++) {
	    Position a = trVerts[j];
	    Position b = trVerts[(j+1)%3];
	    distance = distance(a, b, point);
	    if (distance < minDistance) {
		minDistance = distance;
	    }
	}
	return minDistance;
    }
}
