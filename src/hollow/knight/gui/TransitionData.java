package hollow.knight.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
    private final Optional<Gate> vanillaTarget;

    GateData(String name, JsonObject obj) {
      this.name = name;
      this.alias = obj.get("Alias").getAsString();

      if (obj.has("VanillaTarget")) {
        JsonObject t = obj.get("VanillaTarget").getAsJsonObject();
        Gate gate = Gate.create(t.get("Scene").getAsString(), t.get("Gate").getAsString());
        vanillaTarget = Optional.of(gate);
      } else {
        vanillaTarget = Optional.empty();
      }
    }

    public String name() {
      return name;
    }

    public String alias() {
      return alias;
    }

    public Optional<Gate> vanillaTarget() {
      return vanillaTarget;
    }
  }

  public static final class SceneData {
    private final String alias;
    private final ListMultimap<String, GateData> gatesByDir;
    private final Map<String, GateData> gatesByName;

    private final double width;
    private final double height;

    private double calculateDimension(String... dirs) {
      int count = Arrays.stream(dirs).mapToInt(d -> gatesByDir.get(d).size()).max().getAsInt();

      return 100.0 + Math.min(count, 1) * 40.0;
    }

    private double calculateWidth() {
      return calculateDimension("Door", "Top", "Bot");
    }

    private double calculateHeight() {
      return calculateDimension("Left", "Right");
    }

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

      if (obj.has("Width")) {
        this.width = obj.get("Width").getAsDouble();
      } else {
        this.width = calculateWidth();
      }

      if (obj.has("Height")) {
        this.height = obj.get("Height").getAsDouble();
      } else {
        this.height = calculateHeight();
      }
    }

    public String alias() {
      return alias;
    }

    public GateData getGate(String name) {
      return gatesByName.get(name);
    }

    public double width() {
      return width;
    }

    public double height() {
      return height;
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

  public SceneData sceneData(String scene) {
    return scenes.get(scene);
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
