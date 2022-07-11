package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.gui.TransitionData.SceneData;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;

public final class TransitionVisualizerPlacements implements SaveInterface {
  // Point on canvas, relative to (0, 0) central origin.
  public static final class Point {
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

  public static final class Placement {
    private final String scene;
    private Point point;

    public Placement(String scene, Point point) {
      this.scene = scene;
      this.point = point;
    }

    public String scene() {
      return scene;
    }

    public Point point() {
      return point;
    }

    public boolean isInRange(TransitionData data, Point point) {
      SceneData sceneData = data.sceneData(scene);
      double w = sceneData.width();
      double h = sceneData.height();

      return point.x() >= this.point.x() - w / 2 && point.x() <= this.point.x() + w / 2
          && point.y() >= this.point.y() - h / 2 && point.y() <= this.point.y() + h / 2;
    }

    public JsonObject toJson() {
      JsonObject obj = new JsonObject();
      obj.addProperty("Scene", scene);
      obj.add("Loc", point.toJson());
      return obj;
    }

    public static Placement fromJson(JsonObject obj) {
      return new Placement(obj.get("Scene").getAsString(),
          Point.fromJson(obj.get("Loc").getAsJsonObject()));
    }
  }

  private final LinkedHashSet<Placement> placements = new LinkedHashSet<>();
  private final SetMultimap<String, Placement> placementsByScene = HashMultimap.create();

  public TransitionVisualizerPlacements() {}

  public void addPlacement(String scene, Point point) {
    addPlacementInternal(new Placement(scene, point));
  }

  private void addPlacementInternal(Placement p) {
    placements.add(p);
    placementsByScene.put(p.scene(), p);
  }

  public Optional<Placement> getBestPlacement(TransitionData data, Point point) {
    // Find first reverse match.
    List<Placement> list = new ArrayList<>(placements);
    Collections.reverse(list);

    return list.stream().filter(p -> p.isInRange(data, point)).findFirst();
  }

  @Override
  public String saveName() {
    return "TransitionVisualizerPlacements";
  }

  @Override
  public JsonElement save() {
    JsonObject obj = new JsonObject();

    JsonArray arr = new JsonArray();
    placements.forEach(p -> arr.add(p.toJson()));
    obj.add("Placements", arr);

    return obj;
  }

  @Override
  public void open(Version version, StateContext ctx, JsonElement json) {
    placements.clear();
    placementsByScene.clear();
    if (json == null) {
      return;
    }

    JsonArray arr = json.getAsJsonObject().get("Placements").getAsJsonArray();
    arr.forEach(p -> addPlacementInternal(Placement.fromJson(p.getAsJsonObject())));
  }
}
