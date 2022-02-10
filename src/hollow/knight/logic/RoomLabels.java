package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.JsonObject;
import hollow.knight.util.JsonUtil;

public final class RoomLabels {
  public static enum Type {
    MAP, TITLE,
  }

  private final ImmutableMap<String, ImmutableMap<Type, String>> locToAreaNames;
  private final ImmutableSetMultimap<Type, String> allAreaNames;

  private RoomLabels(Map<String, ImmutableMap<Type, String>> locToAreaNames) {
    this.locToAreaNames = ImmutableMap.copyOf(locToAreaNames);

    ImmutableSetMultimap.Builder<Type, String> builder = ImmutableSetMultimap.builder();
    for (ImmutableMap<Type, String> map : this.locToAreaNames.values()) {
      builder.putAll(map.entrySet());
    }
    this.allAreaNames = builder.build();
  }

  public ImmutableSet<String> allScenes() {
    return locToAreaNames.keySet();
  }

  public ImmutableSet<String> allLabels(Type label) {
    return allAreaNames.get(label);
  }

  private static ImmutableMap<Type, String> EMPTY_MAP = ImmutableMap.of();

  public String get(String scene, Type label) {
    return locToAreaNames.getOrDefault(scene, EMPTY_MAP).getOrDefault(label, "");
  }

  public static RoomLabels load() throws ParseException {
    JsonObject obj = JsonUtil.loadResource(RoomLabels.class, "rooms.json").getAsJsonObject();

    Map<String, ImmutableMap<Type, String>> locToAreaNames = new HashMap<>();

    for (String key : obj.keySet()) {
      JsonObject loc = obj.get(key).getAsJsonObject();
      String locName = loc.get("SceneName").getAsString();
      String mapName = loc.get("MapArea").getAsString();
      String titleName = loc.get("TitledArea").getAsString();

      locToAreaNames.put(locName, ImmutableMap.of(Type.MAP, mapName, Type.TITLE, titleName));
    }

    return new RoomLabels(locToAreaNames);
  }
}
