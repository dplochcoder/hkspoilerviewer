package hollow.knight.logic;

import java.util.Objects;
import com.google.gson.JsonObject;

public final class TermCost implements Cost {
  private final Term term;
  private final int value;

  private TermCost(Term term, int value) {
    this.term = term;
    this.value = value;
  }

  public static TermCost create(Term term, int value) {
    return new TermCost(term, value);
  }

  @Override
  public Term term() {
    return term;
  }

  @Override
  public int value() {
    return value;
  }

  @Override
  public int termCost(Term term) {
    return this.term.equals(term) ? value : 0;
  }

  @Override
  public String debugString() {
    return value + " " + term.name().toLowerCase();
  }

  @Override
  public JsonObject toRawSpoilerJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("$type", "RandomizerCore.Logic.SimpleCost, RandomizerCore");
    obj.addProperty("term", term.name());
    obj.addProperty("threshold", value);
    return obj;
  }

  @Override
  public JsonObject toICDLJson() throws ICDLException {
    JsonObject obj = new JsonObject();
    if (term.equals(Term.rancidEggs())) {
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
    return Objects.hash(TermCost.class, term, value);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermCost)) {
      return false;
    }

    TermCost c = (TermCost) o;
    return Objects.equals(term, c.term) && value == c.value;
  }
}
