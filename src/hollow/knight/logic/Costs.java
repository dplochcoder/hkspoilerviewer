package hollow.knight.logic;

import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MoreCollectors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public final class Costs {
  private final ImmutableList<Cost> costs;

  private Costs(List<Cost> costs) {
    this.costs = ImmutableList.copyOf(costs);
  }

  public boolean canBePaid(boolean canPayGeo, TermMap values) {
    return costs.stream().allMatch(c -> c.canBePaid(canPayGeo, values));
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

  private static final Costs NONE = new Costs(ImmutableList.of());

  public static Costs none() {
    return NONE;
  }
}
