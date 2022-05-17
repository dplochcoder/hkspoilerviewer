package hollow.knight.gui;

import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.StateContext;

public class LocationCategoryFilter extends ItemCategoryFilter {

  private final ImmutableSet<String> locations;

  protected LocationCategoryFilter(String name, Set<String> locations) {
    super(name);
    this.locations = ImmutableSet.copyOf(locations);
  }

  @Override
  public boolean accept(StateContext ctx, ItemCheck itemCheck) {
    return locations.contains(itemCheck.location().name());
  }

  public static LocationCategoryFilter parse(String name, JsonArray locations) {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (JsonElement elem : locations) {
      builder.add(elem.getAsString());
    }
    return new LocationCategoryFilter(name, builder.build());
  }
}
