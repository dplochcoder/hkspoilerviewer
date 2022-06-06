package hollow.knight.logic;

import java.util.List;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;

public final class NotchCostCondition extends Condition {

  private final ImmutableList<Integer> charmIds;

  protected NotchCostCondition(List<Integer> charmIds) {
    super(NotchCostCondition.class.hashCode() ^ charmIds.hashCode());
    this.charmIds = ImmutableList.copyOf(charmIds);
  }

  private int notchCost(NotchCosts notchCosts) {
    int sum = 0;
    int max = 0;
    for (int charmId : charmIds) {
      int cost = notchCosts.notchCost(charmId);
      sum += cost;
      max = Math.max(max, cost);
    }
    return sum - max;
  }

  @Override
  public boolean test(Condition.Context ctx) {
    return ctx.values().get(Term.notches()) > notchCost(ctx.notchCosts());
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    builder.indexTermCondition(Term.notches(), notchCost(builder.ctx().notchCosts()) + 1, this);
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of();
  }

  @Override
  public String debugString() {
    return "NOTCHES > $NotchCost" + charmIds;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NotchCostCondition)) {
      return false;
    }

    return charmIds.equals(((NotchCostCondition) o).charmIds);
  }

  public static NotchCostCondition of(List<Integer> charmIds) {
    return (NotchCostCondition) (new NotchCostCondition(charmIds).intern());
  }
}
