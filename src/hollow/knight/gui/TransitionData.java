package hollow.knight.gui;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;

public final class TransitionData {

  private static final class GateData {
    private final String name;
    private final String alias;

    GateData(String name, JsonObject obj) {
      this.name = name;
      this.alias = obj.get("Alias").getAsString();
    }

    public String name() {
      return name;
    }

    public String alias() {
      return alias;
    }
  }

  private static final class SceneData {
    private final String alias;
    private final ListMultimap<String, GateData> gatesByDir;
    private final Map<String, GateData> gatesByName;

    SceneData(JsonObject obj) {
      this.alias = obj.get("Alias").getAsString();
      this.gatesByDir = ArrayListMultimap.create();
      this.gatesByName = new HashMap<>();

      JsonObject g = obj.get("Gates").getAsJsonObject();
      for (String dir : g.keySet()) {
        JsonObject dg = g.get(dir).getAsJsonObject();
        for (String gate : dg.keySet()) {
          GateData data = new GateData(gate, dg.get(gate).getAsJsonObject());
          gatesByDir.put(dir, data);
          gatesByName.put(gate, data);
        }
      }
    }

    public String alias() {
      return alias;
    }

    public GateData getGate(String name) {
      return gatesByName.get(name);
    }
  }

  private final ImmutableMap<String, SceneData> scenes;

  private TransitionData(Map<String, SceneData> scenes) {
    this.scenes = ImmutableMap.copyOf(scenes);
  }

  public String alias(String transitionName) {
    Gate gate = Gate.parse(transitionName);

    SceneData scene = scenes.get(gate.sceneName());
    if (scene == null) {
      return transitionName;
    }

    GateData data = scene.getGate(gate.gateName());
    if (data == null) {
      return transitionName;
    }

    return scene.alias() + "[" + data.alias() + "]";
  }

  public static TransitionData load() throws ParseException {
    JsonObject obj =
        JsonUtil.loadResource(TransitionData.class, "transition_data.json").getAsJsonObject();

    Map<String, SceneData> scenes = new HashMap<>();
    for (String scene : obj.keySet()) {
      scenes.put(scene, new SceneData(obj.get(scene).getAsJsonObject()));
    }
    return new TransitionData(scenes);
  }

}
