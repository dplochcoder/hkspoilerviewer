package hollow.knight.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class LogicEdits implements StateContext.Mutable {
  private final Set<String> addedWaypoints;
  private final Map<String, String> logicEdits;

  public LogicEdits() {
    this.addedWaypoints = new HashSet<>();
    this.logicEdits = new HashMap<>();
  }

  public boolean isNew(String name) {
    return addedWaypoints.contains(name);
  }

  public boolean isEdited(String name) {
    return logicEdits.containsKey(name);
  }

  public ImmutableSet<String> allLogicNames(StateContext ctx) {
    JsonArray arr = ctx.rawSpoilerJson().get("LM").getAsJsonObject().get("Logic").getAsJsonArray();
    List<String> allNames = new ArrayList<>();
    for (JsonElement elem : arr) {
      allNames.add(elem.getAsJsonObject().get("name").getAsString());
    }
    addedWaypoints.forEach(allNames::add);
    logicEdits.keySet().forEach(allNames::add);
    return ImmutableSet.copyOf(allNames);
  }

  private String getBaseLogic(StateContext ctx, String name) {
    for (JsonElement elem : ctx.rawSpoilerJson().get("LM").getAsJsonObject().get("Logic")
        .getAsJsonArray()) {
      if (elem.getAsJsonObject().get("name").getAsString().equals(name)) {
        return elem.getAsJsonObject().get("logic").getAsString();
      }
    }

    return "";
  }

  public void addWaypoint(StateContext ctx, String name) {
    if (allLogicNames(ctx).contains(name)) {
      return;
    }

    addedWaypoints.add(name);
    logicEdits.put(name, "ANY");
  }

  public String getLogic(StateContext ctx, String name) {
    if (logicEdits.containsKey(name)) {
      return logicEdits.get(name);
    }

    return getBaseLogic(ctx, name);
  }

  public void saveLogic(StateContext ctx, String name, String value) {
    String base = getBaseLogic(ctx, name);
    if (value.equals(base)) {
      clearLogic(name);
    } else {
      logicEdits.put(name, value);
      if (base.isEmpty()) {
        addedWaypoints.add(name);
      }
    }
  }

  public void clearLogic(String name) {
    addedWaypoints.remove(name);
    logicEdits.remove(name);
  }

  public JsonArray addedWaypointsArray() {
    JsonArray arr = new JsonArray();
    addedWaypoints.forEach(arr::add);
    return arr;
  }

  public JsonObject logicEditsDict() {
    JsonObject obj = new JsonObject();
    logicEdits.forEach((k, v) -> obj.addProperty(k, v));
    return obj;
  }

  public void updateLM(JsonObject lm) {
    JsonArray logicArr = lm.get("Logic").getAsJsonArray();
    List<String> toAdd = new ArrayList<>();
    for (JsonElement elem : logicArr) {
      JsonObject obj = elem.getAsJsonObject();
      String name = obj.get("name").getAsString();
      if (logicEdits.containsKey(name)) {
        obj.addProperty("logic", logicEdits.get(name));
      } else {
        toAdd.add(name);
      }
    }

    for (String name : toAdd) {
      JsonObject obj = new JsonObject();
      obj.addProperty("name", name);
      obj.addProperty("logic", logicEdits.get(name));
    }

    JsonArray waypoints = lm.get("Waypoints").getAsJsonArray();
    addedWaypoints.forEach(waypoints::add);
  }

  @Override
  public String saveName() {
    return "ICDLogicEdits";
  }

  @Override
  public JsonObject save() {
    JsonObject obj = new JsonObject();
    obj.add("addedWaypoints", addedWaypointsArray());
    obj.add("logicEdits", logicEditsDict());
    return obj;
  }

  @Override
  public void load(JsonObject json) {
    this.addedWaypoints.clear();
    this.logicEdits.clear();

    json.get("addedWaypoints").getAsJsonArray().forEach(e -> addedWaypoints.add(e.getAsString()));
    json.get("logicEdits").getAsJsonObject().entrySet()
        .forEach(e -> logicEdits.put(e.getKey(), e.getValue().getAsString()));
  }
}
