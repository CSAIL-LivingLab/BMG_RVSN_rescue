package locationserver;

public class Location {
  private Position position;
  private UnitDescriptor units;

  //the following two variables will usually be null
  private Direction direction;
  private Direction normal;
  
  public Location(){}

  public Location (Position p) {
    this.position = p;
  }

  public Location (double x, double y, double z) {
    this.position = new Position(x, y, z);
  }
  Position getPosition() {
    return this.position;
  }
  UnitDescriptor getUnits() {
    return this.units;
  }
  Direction getDirection() {
    return this.direction;
  }
  Direction getNormal() {
    return this.normal;
  }
}
