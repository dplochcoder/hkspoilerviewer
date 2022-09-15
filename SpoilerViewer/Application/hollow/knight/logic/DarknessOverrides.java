package hollow.knight.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class DarknessOverrides {
  public enum Darkness {
    BRIGHT(0, "Bright"), SEMI_DARK(1, "SemiDark"), DARK(2, "Dark");

    private final int intValue;
    private final String jsonName;

    Darkness(int intValue, String jsonName) {
      this.intValue = intValue;
      this.jsonName = jsonName;
    }

    public int intValue() {
      return intValue;
    }

    public String jsonName() {
      return jsonName;
    }

    private static final ImmutableMap<String, Darkness> JSON_MAP = Maps.uniqueIndex(
        Arrays.stream(values()).collect(ImmutableList.toImmutableList()), Darkness::jsonName);

    public static Darkness parse(String d) throws ParseException {
      if (!JSON_MAP.containsKey(d)) {
        throw new ParseException("Unknown darkness: " + d);
      }
      return JSON_MAP.get(d);
    }
  }

  private final ImmutableMap<String, Darkness> darknessByScene;

  private DarknessOverrides(Map<String, Darkness> darknessByScene) {
    this.darknessByScene = ImmutableMap.copyOf(darknessByScene);
  }

  public boolean isEmpty() {
    return darknessByScene.isEmpty();
  }

  public Darkness darknessLevel(String scene) {
    return darknessByScene.getOrDefault(scene, Darkness.BRIGHT);
  }

  public JsonElement toJson() {
    if (isEmpty()) {
      return JsonNull.INSTANCE;
    }

    JsonObject ret = new JsonObject();
    JsonObject map = new JsonObject();
    darknessByScene.forEach((s, d) -> map.addProperty(s, d.jsonName()));;
    ret.add("DarknessOverrides", map);
    return ret;
  }

  public static DarknessOverrides parse(JsonObject obj) throws ParseException {
    // Load from IC or DarknessSpoiler.json
    if (obj != null && obj.has("mods")) {
      JsonArray arr = obj.get("mods").getAsJsonObject().get("Modules").getAsJsonArray();
      obj = null;
      for (JsonElement mod : arr) {
        if (mod.getAsJsonObject().get("$type").getAsString().contains("DarknessRandomizerModule")) {
          obj = mod.getAsJsonObject();
          break;
        }
      }
    }
    if (obj != null && obj.has("DarknessOverrides")) {
      obj = obj.get("DarknessOverrides").getAsJsonObject();
    }

    Map<String, Darkness> map = new HashMap<>();
    if (obj != null) {
      for (String scene : obj.keySet()) {
        map.put(scene, Darkness.parse(obj.get(scene).getAsString()));
      }
    }
    return new DarknessOverrides(map);
  }
}
