package hollow.knight.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.StateContext;

public abstract class ItemCategoryFilter {

  private final String name;

  protected ItemCategoryFilter(String name) {
    this.name = name;
  }

  public final String name() {
    return name;
  }

  public abstract boolean accept(StateContext ctx, ItemCheck itemCheck);

  public static ItemCategoryFilter parse(JsonObject obj) throws ParseException {
    String name = obj.get("Name").getAsString();

    JsonElement items = obj.get("Items");
    if (items != null) {
      return ExplicitItemCategoryFilter.parse(name, items.getAsJsonArray());
    }

    JsonElement effectTerm = obj.get("EffectTerm");
    if (effectTerm != null) {
      return EffectItemCategoryFilter.parse(name, effectTerm.getAsString());
    }

    JsonElement pools = obj.get("Pools");
    if (pools != null) {
      return PoolsCategoryFilter.parse(name, pools.getAsJsonArray());
    }

    throw new ParseException("Unsupported filter: " + name);
  }

}
