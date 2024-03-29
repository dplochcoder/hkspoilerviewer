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
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;

public final class TransitionVisualizerPlacements implements SaveInterface {
  private final LinkedHashSet<ScenePlacement> scenePlacements = new LinkedHashSet<>();
  private final SetMultimap<String, ScenePlacement> scenePlacementsByName = HashMultimap.create();

  public TransitionVisualizerPlacements() {}

  public boolean isEmpty() {
    return scenePlacements.isEmpty();
  }

  public ScenePlacement addPlacement(String scene, Point point) {
    ScenePlacement p = new ScenePlacement(scene, point);
    addPlacementInternal(p);
    return p;
  }

  private void addPlacementInternal(ScenePlacement p) {
    scenePlacements.add(p);
    scenePlacementsByName.put(p.scene(), p);
  }

  private void removePlacementInternal(ScenePlacement p) {
    scenePlacements.remove(p);
    scenePlacementsByName.remove(p.scene(), p);
  }

  public Stream<ScenePlacement> allScenePlacements() {
    return scenePlacements.stream();
  }

  public Iterable<ScenePlacement> allScenePlacementsReversed() {
    List<ScenePlacement> reversed = new ArrayList<>(scenePlacements);
    Collections.reverse(reversed);
    return reversed;
  }

  public Stream<ScenePlacement> placementsForScene(String scene) {
    return scenePlacementsByName.get(scene).stream();
  }

  public void reset(TransitionVisualizerPlacements other) {
    clear();
    other.allScenePlacements().forEach(this::addPlacementInternal);
  }

  public void removePlacement(ScenePlacement p) {
    removePlacementInternal(p);
  }

  public void clear() {
    scenePlacements.clear();
    scenePlacementsByName.clear();
  }

  @Override
  public String saveName() {
    return "TransitionVisualizerPlacements";
  }

  @Override
  public JsonElement save() {
    JsonObject obj = new JsonObject();

    JsonArray arr = new JsonArray();
    scenePlacements.forEach(p -> arr.add(p.toJson()));
    obj.add("ScenePlacements", arr);

    return obj;
  }

  @Override
  public void open(Version version, StateContext ctx, JsonElement json) {
    clear();
    if (json == null) {
      return;
    }

    JsonArray arr = json.getAsJsonObject().get("ScenePlacements").getAsJsonArray();
    arr.forEach(p -> addPlacementInternal(ScenePlacement.fromJson(p.getAsJsonObject())));
  }
}
