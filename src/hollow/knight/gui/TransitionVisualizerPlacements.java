package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
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

    public double x() {
      return point.x();
    }

    public double y() {
      return point.y();
    }

    public void translate(double dx, double dy) {
      point = point.translated(dx, dy);
    }

    public Rect getRect(TransitionData data) {
      SceneData sceneData = data.sceneData(scene);
      return new Rect(point, sceneData.width(), sceneData.height());
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

  public Stream<Placement> allPlacements() {
    return placements.stream();
  }

  public Iterable<Placement> allPlacementsReversed() {
    List<Placement> reversed = new ArrayList<>(placements);
    Collections.reverse(reversed);
    return reversed;
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
