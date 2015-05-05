package locationserver;

/** this is a class defining Position - immutable

*/
import java.io.*;

public class Position implements Serializable {
  public final double x,y,z;
  
  public Position(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  public boolean equals(Object o) {
      Position p = (Position) o; /*
      if (this.x == p.x &&
	  this.y == p.y &&
	  this.z == p.z)*/
      if (Math.abs(this.x - p.x) < 0.1 &&
	  Math.abs(this.y - p.y) < 0.1 &&
	  Math.abs(this.z - p.z) < 0.1)
	  return true;
      else
	  return false;
  }

  public String toString() {
    return new String(this.x + ", " + this.y + ", " + this.z);
  }
  
  public double distance(Position p) {
    double deltaX = Math.pow((this.x - p.x), 2);
    double deltaY = Math.pow((this.y - p.y), 2);
    double deltaZ = Math.pow((this.z - p.z), 2);
    return Math.sqrt(deltaX + deltaY + deltaZ);
  }

  public static double dotProd(Position p1, Position p2) {
      return ((p1.x * p2.x) + (p1.y * p2.y) + (p1.z * p2.z));
  }

  public static Position normalize(Position p) {
      double length = Math.sqrt((Math.pow(p.x,2) + Math.pow(p.y,2) + Math.pow(p.z,2)));
      return scale(p, 1.0/length);
  }

  public static Position diff(Position p1, Position p2) {
      return new Position(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z);
  }

  public static Position scale(Position p, double scale) {
      return new Position(p.x*scale, p.y*scale, p.z*scale);
  }

}
