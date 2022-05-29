package hollow.knight.logic;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.gson.JsonObject;

@AutoValue
public abstract class ItemCheck {
  public abstract CheckId id();

  public abstract Location location();

  public abstract Item item();

  // TODO: Costs apply to the check only in shops.
  // For non-shops, they apply to the Location. Fix this.
  public abstract Costs costs();

  public abstract boolean vanilla();

  @Memoized
  public Condition condition() {
    return Conjunction.of(location().accessCondition(), costs().asCondition());
  }

  public final JsonObject toJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", id().id());
    obj.addProperty("location", location().name());
    obj.add("item", item().toJson());
    obj.add("costs", costs().toRawSpoilerJson());
    obj.addProperty("vanilla", vanilla());
    return obj;
  }

  public static ItemCheck fromJson(ItemChecks checks, JsonObject json) {
    return create(CheckId.of(json.get("id").getAsInt()),
        checks.getLocation(json.get("location").getAsString()),
        Item.fromJson(checks, json.get("item")), Costs.parse(json.get("costs").getAsJsonArray()),
        json.get("vanilla").getAsBoolean());
  }

  public static ItemCheck create(CheckId id, Location loc, Item item, Costs costs,
      boolean vanilla) {
    return new AutoValue_ItemCheck(id, loc, item, costs, vanilla);
  }

  // Always compare ItemChecks by identity for efficiency.
  @Override
  public final boolean equals(Object o) {
    return this == o;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }
}
