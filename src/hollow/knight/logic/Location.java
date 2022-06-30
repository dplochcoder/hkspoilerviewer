package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import hollow.knight.gui.SceneNicknames;

@AutoValue
public abstract class Location {
  private static final ImmutableMap<String, String> SCENE_OVERRIDES = ImmutableMap
      .of("Geo_Rock-Crossroads_Tram", "Crossroads_46", "Start", "Start", "Bench-Godhome_Roof", "");

  private static final ImmutableSet<String> SHOPS =
      ImmutableSet.of("Egg_Shop", "Grubfather", "Iselda", "Leg_Eater", "Salubra", "Seer", "Sly",
          "Sly_(Key)", "Unbreakable_Greed", "Unbreakable_Heart", "Unbreakable_Strength");

  public static ImmutableSet<String> shops() {
    return SHOPS;
  }

  public abstract String name();

  public final String displayName(SceneNicknames sceneNicknames) {
    return isTransition() ? sceneNicknames.nickname(name()) : name();
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
      scene = obj.get("LocationDef").getAsJsonObject().get("SceneName").getAsString();
    } else {
      scene = inferScene(rooms, name, locAccess);
    }

    return new AutoValue_Location(name, isTransition, locAccess, scene);
  }

  private static String inferScene(RoomLabels rooms, String name, Condition accessCondition)
      throws ParseException {
    Set<String> potentialScenes = new HashSet<>();
    for (Term t : accessCondition.locationTerms().collect(ImmutableSet.toImmutableSet())) {
      if (t.name().contains("[")) {
        potentialScenes.add(t.name().substring(0, t.name().indexOf('[')));
      } else {
        potentialScenes.add(t.name());
      }
    }

    Set<String> both = new HashSet<>(Sets.intersection(potentialScenes, rooms.allScenes()));
    if (both.size() == 1) {
      return both.iterator().next();
    } else if (!SCENE_OVERRIDES.containsKey(name)) {
      throw new ParseException("Unknown scene: " + name);
    } else {
      return SCENE_OVERRIDES.get(name);
    }
  }
}
