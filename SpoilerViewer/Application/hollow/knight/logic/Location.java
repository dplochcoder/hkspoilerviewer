package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.gui.TransitionData;

@AutoValue
public abstract class Location {
  private static final ImmutableMap<String, String> SCENE_OVERRIDES = ImmutableMap.of(
      "Geo_Rock-Crossroads_Tram", "Crossroads_46", "Start", "Start", "Bench-Godhome_Roof", "",
      "Journal_Entry-Weathered_Mask", "GG_Land_Of_Storms", "Journal_Entry-Void_Idol_1",
      "GG_Workshop", "Journal_Entry-Void_Idol_2", "GG_Workshop", "Journal_Entry-Void_Idol_3",
      "GG_Workshop", "Bench-Upper_Tram", "Room_Tram_RG", "Bench-Lower_Tram", "Room_Tram");

  private static final ImmutableSet<String> SHOPS = ImmutableSet.of("Egg_Shop", "Grubfather",
      "Iselda", "Leg_Eater", "Salubra", "Seer", "Sly", "Sly_(Key)");

  public static ImmutableSet<String> shops() {
    return SHOPS;
  }

  public abstract String name();

  public final String displayName(TransitionData transitionData) {
    return isTransition() ? transitionData.alias(name()) : name();
  }

  public abstract boolean isTransition();

  public abstract String scene();

  public final boolean isShop() {
    return SHOPS.contains(name());
  }

  public static Location parse(RoomLabels rooms, ImmutableMap<String, String> logicMap,
      JsonObject obj, boolean isTransition) throws ParseException {
    JsonObject logicObj = obj;
    if (logicObj.has("logic")) {
      logicObj = logicObj.get("logic").getAsJsonObject();
    }

    String name = logicObj.get("Name").getAsString();

    String scene;
    if (obj.has("LocationDef") && !obj.get("LocationDef").isJsonNull()) {
      JsonElement sceneName = obj.get("LocationDef").getAsJsonObject().get("SceneName");
      scene = sceneName.isJsonNull() ? "Unknown" : sceneName.getAsString();
    } else {
      scene = inferScene(rooms, name, logicMap.get(name));
    }

    return new AutoValue_Location(name, isTransition, scene);
  }

  private static final String PROXY_SUFFIX = "_Proxy";

  private static String inferScene(RoomLabels rooms, String name, String logic)
      throws ParseException {
    if (SCENE_OVERRIDES.containsKey(name)) {
      return SCENE_OVERRIDES.get(name);
    }

    if (name.contains("[")) {
      String sName = name.substring(0, name.indexOf('['));
      if (rooms.allScenes().contains(sName)) {
        return sName;
      }
    }
    if (logic == null) {
      return "UNKNOWN";
    }

    // TODO: Just get scene names.
    Set<String> potentialScenes = lexScenes(logic);

    Set<String> both = new HashSet<>(Sets.intersection(potentialScenes, rooms.allScenes()));
    if (both.size() == 1) {
      return both.iterator().next();
    } else {
      return "UNKNOWN";
    }
  }

  private static Set<String> lexScenes(String logic) {
    Set<String> out = new HashSet<>();

    StringBuilder sb = new StringBuilder();
    for (char ch : logic.toCharArray()) {
      if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' || ch <= 'Z') || (ch >= '0' && ch <= '9')) {
        sb.append(ch);
      } else {
        lexScene(sb.toString(), out);
        sb = new StringBuilder();
      }
    }

    lexScene(sb.toString(), out);
    return out;
  }

  private static void lexScene(String t, Set<String> out) {
    if (t.isEmpty()) {
      return;
    }

    if (t.endsWith(PROXY_SUFFIX)) {
      out.add(t.substring(0, t.length() - PROXY_SUFFIX.length()));
    } else {
      out.add(t);
    }
  }
}
