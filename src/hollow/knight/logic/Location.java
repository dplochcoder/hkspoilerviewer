package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@AutoValue
public abstract class Location {
  private static final ImmutableMap<String, String> SCENE_OVERRIDES = ImmutableMap.of(
      "Mask_Shard-Grey_Mourner", "Room_Mansion", "Vessel_Fragment-Basin", "Abyss_04", "Dash_Slash",
      "Room_nailmaster_03", "Geo_Rock-Crossroads_Tram", "Crossroads_46", "Start", "Start");

  public abstract String name();

  public abstract Condition accessCondition();

  public abstract String scene();

  public static Location create(RoomLabels rooms, String name, Condition accessCondition)
      throws ParseException {
    return new AutoValue_Location(name, accessCondition, inferScene(rooms, name, accessCondition));
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
