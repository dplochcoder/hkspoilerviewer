package hollow.knight.gui;

import com.google.gson.JsonObject;

public final class Point {
  private final double x;
  private final double y;

  public Point(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public static double distance(Point p1, Point p2) {
    double dx = p1.x() - p2.x();
    double dy = p1.y() - p2.y();
    return Math.sqrt(dx * dx + dy * dy);
  }

  public double x() {
    return x;
  }

  public double y() {
    return y;
  }

  public Point translated(double dx, double dy) {
    return new Point(x + dx, y + dy);
  }

  public static Point interp(Point p1, double f, Point p2) {
    double dx = f * (p2.x() - p1.x());
    double dy = f * (p2.y() - p1.y());
    return p1.translated(dx, dy);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("x", x);
    obj.addProperty("y", y);
    return obj;
  }

  public static Point fromJson(JsonObject obj) {
    return new Point(obj.get("x").getAsDouble(), obj.get("y").getAsDouble());
  }
}
