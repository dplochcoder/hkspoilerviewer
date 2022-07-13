package hollow.knight.gui;

public final class Rect {
  private final Point center;
  private final double width;
  private final double height;

  public Rect(Point center, double width, double height) {
    this.center = center;
    this.width = width;
    this.height = height;
  }

  public Point center() {
    return center;
  }

  public double x1() {
    return center.x() - width / 2;
  }

  public double x2() {
    return center.x() + width / 2;
  }

  public double y1() {
    return center.y() - height / 2;
  }

  public double y2() {
    return center.y() + height / 2;
  }

  public double width() {
    return width;
  }

  public double height() {
    return height;
  }

  public boolean contains(Point p) {
    return p.x() >= x1() && p.x() <= x2() && p.y() >= y1() && p.y() <= y2();
  }

  public boolean intersects(Rect r) {
    return x1() <= r.x2() && x2() >= r.x1() && y1() <= r.y2() && y2() >= r.y1();
  }

  public boolean contains(Rect r) {
    return x1() <= r.x1() && x2() >= r.x2() && y1() <= r.y1() && y2() >= r.y2();
  }

  public static Rect containing(Point p1, Point p2) {
    double x1 = Math.min(p1.x(), p2.x());
    double x2 = p1.x() + p2.x() - x1;
    double y1 = Math.min(p1.y(), p2.y());
    double y2 = p1.y() + p2.y() - y1;

    return new Rect(new Point((x1 + x2) / 2, (y1 + y2) / 2), x2 - x1, y2 - y1);
  }
}
