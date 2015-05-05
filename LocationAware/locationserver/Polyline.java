package locationserver;

/**
 * a polyline class.
 */

import java.util.*;
import java.io.*;

public class Polyline implements Serializable {
  /* using a vector guarantees correct order */
  private Vector positions;

  public Polyline() {
    this.positions = new Vector();
  }

  public void addPoint(Position nextPosition) {
    this.positions.add(nextPosition);
  }
  
  public Position[] getPoints() {
    Position[] pos = new Position[this.positions.size()];
    for (int i = 0; i < this.positions.size(); i++) {
      pos[i] = (Position) this.positions.get(i);
    }
    return pos;
  }

  public int numPoints() {
    return this.positions.size();
  }
   
  public void echo() {
    for (int i = 0; i < this.positions.size(); i++) {
      System.out.println("Position["+i+"]: " + (Position) positions.get(i));
    }
  }

  /**
   * Returns the length of this polyline as a double value. This
   * quantity has no notion of units, but for any given map all
   * polylines will have consistent lengths. Distance between points
   * is computed using the L2 norm.
   */
  public double length() {
    double length = 0;
    Position p1 = null;
    Position p2 = null;
    if (this.positions.size() < 2) { return 0; } // case: no segments
    for (int i = 0; i < this.positions.size() - 1; i++) {
      p1 = (Position) this.positions.get(i);
      p2 = (Position) this.positions.get(i+1);
      length = length + p1.distance(p2);
    }
    return length;
  }
  
  /**
   * Append a polyline to this polyline
   */  
  public void append(Polyline p) {
    Position[] newPos = p.getPoints();
    for (int i = 0; i < newPos.length; i++) {
      this.addPoint(newPos[i]);
    }
  }
}
