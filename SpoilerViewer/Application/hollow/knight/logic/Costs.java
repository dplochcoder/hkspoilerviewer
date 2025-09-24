package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

public final class Costs {
  private static final Costs NONE = new Costs(ImmutableSet.of());

  public static Costs none() {
    return NONE;
  }

  private final ImmutableSet<Cost> costs;

  public Costs(Set<Cost> costs) {
    this.costs = ImmutableSet.copyOf(costs);
  }

  public Costs(Cost cost) {
    this(ImmutableSet.of(cost));
  }

  public boolean isNone() {
    return costs.isEmpty();
  }

  public ImmutableSet<Cost> costs() {
    return costs;
  }

  public String suffixString() {
    if (costs.isEmpty()) {
      return "";
    }
    return costs.stream().map(Cost::debugString).collect(Collectors.joining(", ", " (", ")"));
  }

  public int getGeoCost() {
    return costs.stream().mapToInt(Cost::geoCost).sum();
  }

  public int getCostTerm(Term term) {
    return costs.stream().mapToInt(c -> c.termCost(term)).sum();
  }

  public JsonArray toRawSpoilerJson() {
    JsonArray arr = new JsonArray();
    costs.forEach(c -> arr.add(c.toRawSpoilerJson()));
    return arr;
  }

  public JsonElement toICDLJson() throws ICDLException {
    if (costs.isEmpty()) {
      return JsonNull.INSTANCE;
    } else if (costs.size() == 1) {
      return costs.iterator().next().toICDLJson();
    } else {
      JsonObject multi = new JsonObject();
      multi.addProperty("$type", "ItemChanger.MultiCost, ItemChanger");
      multi.addProperty("Paid", false);
      multi.addProperty("DiscountRate", 1.0);

      JsonArray arr = new JsonArray();
      for (Cost c : costs) {
        arr.add(c.toICDLJson());
      }
      multi.add("Costs", arr);

      return multi;
    }
  }

  public static Costs parse(JsonArray costs) throws ParseException {
    ImmutableSet.Builder<Cost> builder = ImmutableSet.builder();
    for (JsonElement elem : costs) {
      Cost cost = Cost.parse(elem.getAsJsonObject());
      builder.add(cost);
    }

    return new Costs(builder.build());
  }

  public static Costs defaultCosts(String location) {
    switch (location) {
      case "Iselda":
      case "Leg_Eater":
      case "Sly":
      case "Sly_(Key)":
        return new Costs(GeoCost.create(1));
      case "BugPrince-Iselda_(Requires_Maps)":
        return new Costs(ImmutableSet.of(GeoCost.create(1), TermCost.create(Term.maps(), 1)));
      case "Salubra":
        return new Costs(ImmutableSet.of(GeoCost.create(1), TermCost.create(Term.charms(), 1)));
      case "Grubfather":
        return new Costs(TermCost.create(Term.grubs(), 1));
      case "Seer":
        return new Costs(TermCost.create(Term.essence(), 1));
      case "Egg_Shop":
        return new Costs(TermCost.create(Term.rancidEggs(), 1));
      case "Crossroads_Stag":
        return new Costs(GeoCost.create(50));
      case "Queen's_Station_Stag":
        return new Costs(GeoCost.create(120));
      case "Greenpath_Stag":
        return new Costs(GeoCost.create(140));
      case "Elevator_Pass":
        return new Costs(GeoCost.create(150));
      case "City_Storerooms_Stag":
        return new Costs(GeoCost.create(200));
      case "Queen's_Gardens_Stag":
        return new Costs(GeoCost.create(200));
      case "Distant_Village_Stag":
        return new Costs(GeoCost.create(250));
      case "Hidden_Station_Stag":
        return new Costs(GeoCost.create(300));
      case "King's_Station_Stag":
        return new Costs(GeoCost.create(300));
      case "Stag_Nest_Stag":
        return new Costs(GeoCost.create(300));
      case "Unbreakable_Greed":
        return new Costs(GeoCost.create(450));
      case "Unbreakable_Heart":
        return new Costs(GeoCost.create(600));
      case "Unbreakable_Strength":
        return new Costs(GeoCost.create(750));
      case "Dash_Slash":
        return new Costs(GeoCost.create(800));
      default:
        return none();
    }
  }

  @Override
  public int hashCode() {
    return Costs.class.hashCode() ^ costs.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Costs)) {
      return false;
    }

    Costs c = (Costs) o;
    return costs.equals(c.costs);
  }
}
