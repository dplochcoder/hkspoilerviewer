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

  private IntStream notchCosts(State state) {
    return charmIds.stream().mapToInt(state.items()::notchCost);
  }

  @Override
  public boolean test(State state) {
    if (charmIds.isEmpty())
      return true;

    int notches = state.get(Term.notches());
    int cost = notchCosts(state).sum() - notchCosts(state).max().getAsInt();

    return notches > cost;
  }
}
