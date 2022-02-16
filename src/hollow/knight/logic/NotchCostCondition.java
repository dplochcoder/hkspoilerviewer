package hollow.knight.logic;

import java.util.List;
import java.util.stream.IntStream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public final class NotchCostCondition extends Condition {

  private final ImmutableList<Integer> charmIds;

  public NotchCostCondition(List<Integer> charmIds) {
    super(ImmutableSet.of(Term.notches()));
    this.charmIds = ImmutableList.copyOf(charmIds);
  }

  private IntStream notchCosts(Items items) {
    return charmIds.stream().mapToInt(items::notchCost);
  }

  @Override
  public boolean test(State state) {
    if (charmIds.isEmpty())
      return true;

    int notches = state.get(Term.notches());
    Items items = state.ctx().items();
    int cost = notchCosts(items).sum() - notchCosts(items).max().getAsInt();

    return notches > cost;
  }
}
