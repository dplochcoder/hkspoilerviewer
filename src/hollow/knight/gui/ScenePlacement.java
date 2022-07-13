package hollow.knight.gui;

import com.google.gson.JsonObject;
import hollow.knight.gui.TransitionData.GateData;
import hollow.knight.gui.TransitionData.SceneData;

public final class ScenePlacement {
  private static final double TRANSITION_SIZE = 30;

  private final String scene;
  private Point point;

  public ScenePlacement(String scene, Point point) {
    this.scene = scene;
    this.point = point;
  }

  public String scene() {
    return scene;
  }

  public Point point() {
    return point;
  }

  public double x() {
    return point.x();
  }

  public double y() {
    return point.y();
  }

  public void translate(double dx, double dy) {
    point = point.translated(dx, dy);
  }

  public void update(Point p) {
    point = p;
  }

  public Rect getRect(TransitionData data) {
    SceneData sceneData = data.sceneData(scene);
    return new Rect(point, sceneData.width(), sceneData.height());
  }

  public Rect getTransitionRect(String gate, TransitionData data) {
    SceneData sceneData = data.sceneData(scene);
    GateData gateData = sceneData.getGate(gate);

    double tx = x() + gateData.xProp() * sceneData.width();
    double ty = y() + gateData.yProp() * sceneData.height();
    return new Rect(new Point(tx, ty), TRANSITION_SIZE, TRANSITION_SIZE);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("Scene", scene);
    obj.add("Loc", point.toJson());
    return obj;
  }

  public static ScenePlacement fromJson(JsonObject obj) {
    return new ScenePlacement(obj.get("Scene").getAsString(),
        Point.fromJson(obj.get("Loc").getAsJsonObject()));
  }
}
