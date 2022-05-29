package hollow.knight.logic;

import java.util.Objects;
import com.google.common.base.Verify;
import com.google.gson.JsonObject;

public final class Cost {
  public static enum Type {
    GEO, TERM,
  }

  private final Type type;
  private final int value;
  private final Term term;

  private Cost(Type type, int value, Term term) {
    this.type = type;
    this.value = value;
    this.term = term;
  }

  public static Cost createGeo(int value) {
    return new Cost(Type.GEO, value, null);
  }

  public static Cost createTerm(Term term, int value) {
    return new Cost(Type.TERM, value, term);
  }

  public Condition asCondition() {
    if (type == Type.GEO) {
      return Condition.canReplenishGeo();
    } else {
      return TermGreaterThanCondition.of(term, value - 1);
    }
  }

  public Type type() {
    return type;
  }

  public int value() {
    return value;
  }

  public Term term() {
    Verify.verify(type() == Type.TERM);
    return term;
  }

  public boolean hasCostTerm(Term term) {
    return type == Type.TERM && this.term.equals(term);
  }

  public String debugString() {
    if (type == Type.GEO) {
      return value + " geo";
    } else {
      return value + " " + term.name().toLowerCase();
    }
  }

  public JsonObject toRawSpoilerJson() {
    JsonObject obj = new JsonObject();
    if (type == Type.GEO) {
      obj.addProperty("GeoAmount", value);
    } else {
      obj.addProperty("term", term.name());
      obj.addProperty("threshold", value);
    }
    return obj;
  }

  public JsonObject toICDLJson() throws ICDLException {
    JsonObject obj = new JsonObject();
    if (type == Type.GEO) {
      obj.addProperty("$type", "ItemChanger.GeoCost, ItemChanger");
      obj.addProperty("amount", value);
    } else if (term.equals(Term.rancidEggs())) {
      obj.addProperty("$type", "ItemChanger.Modules.CumulativeRancidEggCost, ItemChanger");
      obj.addProperty("Total", value);
    } else {
      obj.addProperty("$type", "ItemChanger.PDIntCost, ItemChanger");
      obj.addProperty("amount", value);
      obj.addProperty("op", "Ge");

      if (term.equals(Term.charms())) {
        obj.addProperty("fieldName", "charmsOwned");
        obj.addProperty("uiText", "Once you own " + value + " charms, I'll gladly sell it to you.");
      } else if (term.equals(Term.essence())) {
        obj.addProperty("fieldName", "dreamOrbs");
        obj.addProperty("uiText", "Requires " + value + " Essence");
      } else if (term.equals(Term.grubs())) {
        obj.addProperty("fieldName", "grubsCollected");
        obj.addProperty("uiText", "Requires " + value + " Grubs");
      } else if (term.equals(Term.scream())) {
        obj.addProperty("fieldName", "screamLevel");
        obj.addProperty("uiText", "Requires " + (value == 1 ? "Howling Wraiths" : "Abyss Shriek"));
      } else {
        throw new ICDLException("Cannot handle cost term: " + term.name());
      }
    }

    obj.addProperty("Paid", false);
    obj.addProperty("DiscountRate", 1.0);
    return obj;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, term, value);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Cost)) {
      return false;
    }

    Cost c = (Cost) o;
    return type == c.type && Objects.equals(term, c.term) && value == c.value;
  }

  public static Cost parse(JsonObject obj) {
    if (obj.get("term") != null) {
      Term term = Term.create(obj.get("term").getAsString());
      int threshold = obj.get("threshold").getAsInt();
      return new Cost(Type.TERM, threshold, term);
    }

    int geo = obj.get("GeoAmount").getAsInt();
    return new Cost(Type.GEO, geo, null);
  }
}
