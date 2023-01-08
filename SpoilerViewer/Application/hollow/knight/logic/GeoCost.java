package hollow.knight.logic;

import java.util.Objects;
import com.google.gson.JsonObject;

public final class GeoCost implements Cost {
  private final int value;

  private GeoCost(int value) {
    this.value = value;
  }

  public static GeoCost create(int value) {
    return new GeoCost(value);
  }

  @Override
  public Term term() {
    return Term.geo();
  }

  @Override
  public int value() {
    return value;
  }

  @Override
  public int geoCost() {
    return value;
  }

  @Override
  public String debugString() {
    return value + " geo";
  }

  @Override
  public JsonObject toRawSpoilerJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("$type", "RandomizerMod.RC.LogicGeoCost, RandomizerMod");
    obj.addProperty("CanReplenishGeoWaypoint", "Can_Replenish_Geo");
    obj.addProperty("GeoAmount", value);
    return obj;
  }

  @Override
  public JsonObject toICDLJson() throws ICDLException {
    JsonObject obj = new JsonObject();
    obj.addProperty("$type", "ItemChanger.GeoCost, ItemChanger");
    obj.addProperty("amount", value);
    obj.addProperty("Paid", false);
    obj.addProperty("DiscountRate", 1.0);
    return obj;
  }

  @Override
  public int hashCode() {
    return Objects.hash(GeoCost.class, value);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GeoCost)) {
      return false;
    }

    GeoCost c = (GeoCost) o;
    return value == c.value;
  }
}
