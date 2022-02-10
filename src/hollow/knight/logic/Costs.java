package hollow.knight.logic;

import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public final class Costs {
  private final ImmutableList<Cost> costs;
  
  private Costs(List<Cost> costs) {
    this.costs = ImmutableList.copyOf(costs);
  }
  
  public boolean canBePaid(State state) {
    return costs.stream().allMatch(c -> c.canBePaid(state));
  }
  
  public String suffixString() {
    if (costs.isEmpty()) return "";
    return costs.stream().map(Cost::debugString).collect(Collectors.joining(", ", " (", ")"));
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

  public static Costs none() { return NONE; }
}