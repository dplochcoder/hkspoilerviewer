package hollow.knight.logic;

import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public final class Costs {
  private final ImmutableList<Cost> costs;
  private final Condition condition;

  public Costs(List<Cost> costs) {
    this.costs = ImmutableList.copyOf(costs);
    this.condition = costs.isEmpty() ? Condition.alwaysTrue()
        : Conjunction
            .of(costs.stream().map(Cost::asCondition).collect(ImmutableSet.toImmutableSet()));
  }

  public Costs(Cost cost) {
    this(ImmutableList.of(cost));
  }

  public Condition asCondition() {
    return condition;
  }

  public String suffixString() {
    if (costs.isEmpty())
      return "";
    return costs.stream().map(Cost::debugString).collect(Collectors.joining(", ", " (", ")"));
  }

  public boolean hasCostTerm(Term term) {
    return costs.stream().anyMatch(c -> c.hasCostTerm(term));
  }

  public int getCostTerm(Term term) {
    return costs.stream().filter(c -> c.hasCostTerm(term)).collect(MoreCollectors.onlyElement())
        .value();
  }

  public static Costs parse(JsonArray costs) {
    ImmutableList.Builder<Cost> builder = ImmutableList.builder();
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
      case "Salubra":
      case "Sly":
      case "Sly_(Key)":
        return new Costs(Cost.createGeo(1));
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
      default:
        return none();
    }
  }

  private static final Costs NONE = new Costs(ImmutableList.of());

  public static Costs none() {
    return NONE;
  }
}
