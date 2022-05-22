package hollow.knight.logic;

import java.util.Set;
import java.util.stream.IntStream;
import com.google.common.collect.ImmutableSet;

public final class NotchCostCondition extends Condition {

  private final ImmutableSet<Integer> charmIds;

  public NotchCostCondition(Set<Integer> charmIds) {
    super(ImmutableSet.of(Term.notches()));
    this.charmIds = ImmutableSet.copyOf(charmIds);
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

  @Override
  public int hashCode() {
    return NotchCostCondition.class.hashCode() ^ charmIds.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NotchCostCondition)) {
      return false;
    }

    return charmIds.equals(((NotchCostCondition) o).charmIds);
  }

}
