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
  private static final ImmutableMap<String, String> SCENE_OVERRIDES =
      ImmutableMap.of("Geo_Rock-Crossroads_Tram", "Crossroads_46", "Start", "Start",
          "Bench-Godhome_Roof", "", "Journal_Entry-Weathered_Mask", "GG_Land_Of_Storms",
          "Journal_Entry-Void_Idol_1", "GG_Workshop", "Journal_Entry-Void_Idol_2", "GG_Workshop",
          "Journal_Entry-Void_Idol_3", "GG_Workshop");

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

  public abstract Condition accessCondition();

  public abstract String scene();

  public final boolean isShop() {
    return SHOPS.contains(name());
  }

  public static Location parse(RoomLabels rooms, JsonObject obj, boolean isTransition)
      throws ParseException {
    JsonObject logicObj = obj;
    if (logicObj.has("logic")) {
      logicObj = logicObj.get("logic").getAsJsonObject();
    }

    String name = logicObj.get("Name").getAsString();
    Condition locAccess = ConditionParser.parse(logicObj.get("Logic").getAsString());

    String scene;
    if (obj.has("LocationDef")) {
      JsonElement sceneName = obj.get("LocationDef").getAsJsonObject().get("SceneName");
      scene = sceneName.isJsonNull() ? "Unknown" : sceneName.getAsString();
    } else {
      scene = inferScene(rooms, name, locAccess);
    }

    return new AutoValue_Location(name, isTransition, locAccess, scene);
  }

  private static final String PROXY_SUFFIX = "_Proxy";

  private static String inferScene(RoomLabels rooms, String name, Condition accessCondition)
      throws ParseException {
    Set<String> potentialScenes = new HashSet<>();
    for (Term t : accessCondition.locationTerms().collect(ImmutableSet.toImmutableSet())) {
      if (t.name().contains("[")) {
        String sName = t.name().substring(0, t.name().indexOf('['));
        if (sName.endsWith(PROXY_SUFFIX)) {
          sName = sName.substring(0, sName.length() - PROXY_SUFFIX.length());
        }
        potentialScenes.add(sName);
      } else {
        potentialScenes.add(t.name());
      }
    }

    Set<String> both = new HashSet<>(Sets.intersection(potentialScenes, rooms.allScenes()));
    if (both.size() == 1) {
      return both.iterator().next();
    } else if (both.isEmpty()
        && accessCondition.locationTerms().anyMatch(t -> t.name().startsWith("Defeated_Any_"))) {
      return "Unknown";
    } else if (!SCENE_OVERRIDES.containsKey(name)) {
      throw new ParseException("Unknown scene: " + name);
    } else {
      return SCENE_OVERRIDES.get(name);
    }
  }
}
