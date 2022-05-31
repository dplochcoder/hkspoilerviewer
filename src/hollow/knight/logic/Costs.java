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
  private final Condition condition;

  public Costs(Set<Cost> costs) {
    this.costs = ImmutableSet.copyOf(costs);
    this.condition = costs.isEmpty() ? Condition.alwaysTrue()
        : Conjunction
            .of(costs.stream().map(Cost::asCondition).collect(ImmutableSet.toImmutableSet()));
  }

  public Costs(Cost cost) {
    this(ImmutableSet.of(cost));
  }

  public ImmutableSet<Cost> costs() {
    return costs;
  }

  public Condition asCondition() {
    return condition;
  }

  public String suffixString() {
    if (costs.isEmpty()) {
      return "";
    }
    return costs.stream().map(Cost::debugString).collect(Collectors.joining(", ", " (", ")"));
  }

  public boolean hasGeoCost() {
    return costs.stream().anyMatch(c -> c.type() == Cost.Type.GEO);
  }

  public int getGeoCost() {
    return costs.stream().filter(c -> c.type() == Cost.Type.GEO).mapToInt(Cost::value).sum();
  }

  public boolean hasCostTerm(Term term) {
    return costs.stream().anyMatch(c -> c.hasCostTerm(term));
  }

  public int getCostTerm(Term term) {
    return costs.stream().filter(c -> c.hasCostTerm(term)).mapToInt(Cost::value).sum();
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

  public static Costs parse(JsonArray costs) {
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
        return new Costs(Cost.createGeo(1));
      case "Salubra":
        return new Costs(ImmutableSet.of(Cost.createGeo(1), Cost.createTerm(Term.charms(), 1)));
      case "Grubfather":
        return new Costs(Cost.createTerm(Term.grubs(), 1));
      case "Seer":
        return new Costs(Cost.createTerm(Term.essence(), 1));
      case "Egg_Shop":
        return new Costs(Cost.createTerm(Term.rancidEggs(), 1));
      case "Unbreakable_Greed":
        return new Costs(Cost.createGeo(450));
      case "Unbreakable_Heart":
        return new Costs(Cost.createGeo(600));
      case "Unbreakable_Strength":
        return new Costs(Cost.createGeo(750));
      case "Crossroads_Stag":
        return new Costs(Cost.createGeo(50));
      case "Queen's_Station_Stag":
        return new Costs(Cost.createGeo(120));
      case "Greenpath_Stag":
        return new Costs(Cost.createGeo(140));
      case "City_Storerooms_Stag":
        return new Costs(Cost.createGeo(200));
      case "Queen's_Gardens_Stag":
        return new Costs(Cost.createGeo(200));
      case "Distant_Village_Stag":
        return new Costs(Cost.createGeo(250));
      case "Hidden_Station_Stag":
        return new Costs(Cost.createGeo(300));
      case "King's_Station_Stag":
        return new Costs(Cost.createGeo(300));
      case "Dash_Slash":
        return new Costs(Cost.createGeo(800));
      case "Vessel_Fragment-Basin":
        return new Costs(Cost.createGeo(3000));
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
