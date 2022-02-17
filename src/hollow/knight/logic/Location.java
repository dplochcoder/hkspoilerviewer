package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

public final class Location {
  private static final ImmutableMap<String, String> SCENE_OVERRIDES = ImmutableMap.of(
      "Mask_Shard-Grey_Mourner", "Room_Mansion", "Vessel_Fragment-Basin", "Abyss_04", "Dash_Slash",
      "Room_nailmaster_03", "Geo_Rock-Crossroads_Tram", "Crossroads_46", "Start", "Start");

  private final String name;
  private final Condition access;
  private final String scene;

  private static String inferScene(String name, Condition access, RoomLabels rooms)
      throws ParseException {
    Set<String> potentialScenes = new HashSet<>();
    for (Term t : access.terms()) {
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

  public Location(String name, Condition access, RoomLabels rooms) throws ParseException {
    this.name = name;
    this.access = access;
    this.scene = inferScene(name, access, rooms);
  }

  public String name() {
    return name;
  }

  public boolean canAccess(State state) {
    return access.test(state);
  }

  public ImmutableSet<Term> terms() {
    return access.terms();
  }

  public String scene() {
    return scene;
  }
}
