package hollow.knight.gui;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.StateContext;

public class PoolsCategoryFilter extends ItemCategoryFilter {

  private final ImmutableSet<String> pools;

  protected PoolsCategoryFilter(String name, Set<String> locations) {
    super(name);
    this.pools = ImmutableSet.copyOf(locations);
  }

  @Override
  public boolean accept(StateContext ctx, ItemCheck itemCheck) {
    return pools.contains(ctx.pools().getPool(itemCheck.item().term()));
  }

  public static PoolsCategoryFilter parse(String name, JsonArray array) {
    Set<String> pools = new HashSet<>();
    for (JsonElement elem : array) {
      pools.add(elem.getAsString());
    }

    return new PoolsCategoryFilter(name, pools);
  }

}
