package hollow.knight.gui;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Ints;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.RoomLabels;

public final class TransitionData {

  public static final class GateData {
    private final String scene;
    private final String name;
    private final String alias;
    private final double xProp;
    private final double yProp;
    private final Optional<Gate> vanillaTarget;

    GateData(String scene, String name, JsonObject obj, double xProp, double yProp) {
      this.scene = scene;
      this.name = name;
      this.alias = obj.get("Alias").getAsString();

      if (obj.has("X-Prop")) {
        this.xProp = obj.get("X-Prop").getAsDouble();
      } else {
        this.xProp = xProp;
      }

      if (obj.has("Y-Prop")) {
        this.yProp = obj.get("Y-Prop").getAsDouble();
      } else {
        this.yProp = yProp;
      }

      if (obj.has("VanillaTarget")) {
        JsonObject t = obj.get("VanillaTarget").getAsJsonObject();
        Gate gate = Gate.create(t.get("Scene").getAsString(), t.get("Gate").getAsString());
        vanillaTarget = Optional.of(gate);
      } else {
        vanillaTarget = Optional.empty();
      }
    }

    public String scene() {
      return scene;
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

    public double xProp() {
      return xProp;
    }

    public double yProp() {
      return yProp;
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
    private final Optional<Point> vanillaPlacement;
    private final ImmutableListMultimap<String, GateData> gatesByDir;
    private final ImmutableMap<String, GateData> gatesByName;

    private final double width;
    private final double height;

    private final Color color;

    private static final double BASE_DIMENSION = 150.0;
    private static final double TRANSITION_SCALING = 50.0;

    private double calculateDimension(String... dirs) {
      int count = Arrays.stream(dirs).mapToInt(d -> gatesByDir.get(d).size()).max().getAsInt();

      return BASE_DIMENSION + Math.max(count, 1) * TRANSITION_SCALING;
    }

    private double calculateWidth() {
      return calculateDimension("Door", "Top", "Bot");
    }

    private double calculateHeight() {
      return calculateDimension("Left", "Right");
    }

    private static Color parseColor(String hex) throws ParseException {
      if (!hex.startsWith("#") || (hex.length() != 4 && hex.length() != 7)) {
        throw new ParseException("Invalid color: " + hex);
      }

      Integer rgb = Ints.tryParse(hex.substring(1), 16);
      if (rgb == null) {
        throw new ParseException("Invalid color: " + hex);
      }

      int r, g, b;
      if (hex.length() == 4) {
        b = (rgb & 0xf) * 17;
        g = ((rgb >> 4) & 0xf) * 17;
        r = ((rgb >> 8) & 0xf) * 17;
      } else {
        b = rgb & 0xff;
        g = (rgb >> 8) & 0xff;
        r = (rgb >> 16) & 0xff;
      }
      return new Color(r, g, b);
    }

    private static double defaultXProp(String dir, int index, int n) {
      if (dir.equals("Top") || dir.equals("Bot") || dir.equals("Door")) {
        double reduce = dir.equals("Door") ? 0.8 : 1.0;
        return reduce * (-0.5 + (index + 1.0) / (n + 1.0));
      } else if (dir.equals("Left")) {
        return -0.5;
      } else {
        Verify.verify(dir.equals("Right"));
        return 0.5;
      }
    }

    private static double defaultYProp(String dir, int index, int n) {
      if (dir.equals("Left") || dir.equals("Right")) {
        return -0.5 + (index + 1.0) / (n + 1.0);
      } else if (dir.equals("Top")) {
        return -0.5;
      } else if (dir.equals("Door")) {
        return 0.0;
      } else {
        Verify.verify(dir.equals("Bot"));
        return 0.5;
      }
    }

    SceneData(RoomLabels roomLabels, String scene, JsonObject obj) throws ParseException {
      this.alias = obj.get("Alias").getAsString();

      if (obj.has("VanillaPlacement")) {
        this.vanillaPlacement =
            Optional.of(Point.fromJson(obj.get("VanillaPlacement").getAsJsonObject()));
      } else {
        this.vanillaPlacement = Optional.empty();
      }

      ListMultimap<String, GateData> gatesByDir = ArrayListMultimap.create();
      Map<String, GateData> gatesByName = new HashMap<>();

      JsonObject g = obj.get("Gates").getAsJsonObject();
      for (String dir : g.keySet()) {
        JsonObject dg = g.get(dir).getAsJsonObject();
        int index = 0;
        for (String gate : dg.keySet()) {
          GateData data = new GateData(scene, gate, dg.get(gate).getAsJsonObject(),
              defaultXProp(dir, index, dg.keySet().size()),
              defaultYProp(dir, index, dg.keySet().size()));
          gatesByDir.put(dir, data);
          gatesByName.put(gate, data);
          ++index;
        }
      }

      this.gatesByDir = ImmutableListMultimap.copyOf(gatesByDir);
      this.gatesByName = ImmutableMap.copyOf(gatesByName);

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
        this.color = parseColor(obj.get("Color").getAsString());
      } else {
        this.color = TITLE_AREA_COLORS.getOrDefault(roomLabels.get(scene, RoomLabels.Type.TITLE),
            MAP_AREA_COLORS.getOrDefault(roomLabels.get(scene, RoomLabels.Type.MAP), Color.GRAY));
      }
    }

    public String alias() {
      return alias;
    }

    public Optional<Point> vanillaPlacement() {
      return vanillaPlacement;
    }

    public ImmutableCollection<GateData> allGates() {
      return gatesByName.values();
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

  private ImmutableMap<String, SceneData> scenes;
  private ImmutableSet<Gate> sources;
  private ImmutableSet<Gate> targets;

  private TransitionData(Map<String, SceneData> scenes) {
    this.scenes = ImmutableMap.copyOf(scenes);
    this.sources = scenes.values().stream().flatMap(s -> s.allGates().stream())
        .filter(g -> g.vanillaTarget().isPresent()).map(g -> Gate.create(g.scene(), g.name()))
        .collect(ImmutableSet.toImmutableSet());
    this.targets = scenes.values().stream().flatMap(s -> s.allGates().stream())
        .filter(g -> g.vanillaTarget().isPresent()).map(g -> g.vanillaTarget().get())
        .collect(ImmutableSet.toImmutableSet());
  }

  public void refresh(RoomLabels roomLabels) throws ParseException {
    TransitionData next = load(roomLabels);
    this.scenes = next.scenes;
    this.sources = next.sources;
    this.targets = next.targets;
  }

  public ImmutableSet<String> scenes() {
    return scenes.keySet();
  }

  public boolean isSource(Gate gate) {
    return sources.contains(gate);
  }

  public boolean isTarget(Gate gate) {
    return targets.contains(gate);
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
