package hollow.knight.gui;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.RoomLabels;

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

  private static final ImmutableMap<String, Color> TITLE_AREA_COLORS =
      ImmutableMap.<String, Color>builder().put("Abyss", new Color(31, 31, 31))
          .put("Hive", new Color(59, 52, 38)).put("Isma's Grove", new Color(34, 61, 49))
          .put("Mantis Village", new Color(47, 49, 36)).put("Soul Sanctum", new Color(35, 35, 46))
          .build();


  private static final ImmutableMap<String, Color> MAP_AREA_COLORS = ImmutableMap
      .<String, Color>builder().put("Ancient Basin", new Color(47, 47, 47))
      .put("City of Tears", new Color(43, 46, 59)).put("Crystal Peak", new Color(54, 48, 57))
      .put("Deepnest", new Color(41, 45, 50)).put("Dirtmouth", new Color(47, 47, 47))
      .put("Fog Canyon", new Color(61, 51, 59)).put("Forgotten Crossroads", new Color(44, 50, 57))
      .put("Fungal Wastes", new Color(57, 59, 49)).put("Greenpath", new Color(51, 60, 48))
      .put("Howling Cliffs", new Color(31, 31, 31)).put("Kingdom's Edge", new Color(54, 50, 47))
      .put("Queen's Gardens", new Color(37, 45, 38)).put("Resting Grounds", new Color(57, 56, 41))
      .put("Royal Waterways", new Color(39, 60, 60)).put("White Palace", new Color(80, 80, 80))
      .build();

  public static final class SceneData {
    private final String alias;
    private final ListMultimap<String, GateData> gatesByDir;
    private final Map<String, GateData> gatesByName;

    private final double width;
    private final double height;

    private final Color color;

    private static final double BASE_DIMENSION = 150.0;
    private static final double BONUS_DIMENSION = 50.0;

    private double calculateDimension(String... dirs) {
      int count = Arrays.stream(dirs).mapToInt(d -> gatesByDir.get(d).size()).max().getAsInt();

      return BASE_DIMENSION + Math.min(count, 1) * BONUS_DIMENSION;
    }

    private double calculateWidth() {
      return calculateDimension("Door", "Top", "Bot");
    }

    private double calculateHeight() {
      return calculateDimension("Left", "Right");
    }

    SceneData(RoomLabels roomLabels, String scene, JsonObject obj) {
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

      if (obj.has("Color")) {
        // FIXME: Parse hex code
        this.color = Color.white;
      } else {
        this.color = TITLE_AREA_COLORS.getOrDefault(roomLabels.get(scene, RoomLabels.Type.TITLE),
            MAP_AREA_COLORS.getOrDefault(roomLabels.get(scene, RoomLabels.Type.MAP), Color.GRAY));
      }
    }

    public String alias() {
      return alias;
    }

    public GateData getGate(String name) {
      return gatesByName.get(name);
    }

    public Color color() {
      return color;
    }

    public Color edgeColor() {
      return new Color(color.getRed() / 2 + 128, color.getGreen() / 2 + 128,
          color.getBlue() / 2 + 128);
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

  public ImmutableSet<String> scenes() {
    return scenes.keySet();
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

  public static TransitionData load(RoomLabels roomLabels) throws ParseException {
    JsonObject obj =
        JsonUtil.loadResource(TransitionData.class, "transition_data.json").getAsJsonObject();

    Map<String, SceneData> scenes = new HashMap<>();
    for (String scene : obj.keySet()) {
      scenes.put(scene, new SceneData(roomLabels, scene, obj.get(scene).getAsJsonObject()));
    }
    return new TransitionData(scenes);
  }

}
