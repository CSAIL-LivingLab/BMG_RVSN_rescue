package locationserver;

/**
 * a way to express a Direction (remember - a Location has a
 * direction).  This implementation is immutable.
 */

public class Direction implements java.io.Serializable {
  // internal representation
  public final double x, y, z;
  
  // constructor
  public Direction(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }
}
