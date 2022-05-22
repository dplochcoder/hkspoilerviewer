package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public final class Conjunction extends Condition {
  private final ImmutableSet<Condition> operands;

  private Conjunction(Set<Condition> operands) {
    super(
        operands.stream().flatMap(c -> c.terms().stream()).collect(ImmutableSet.toImmutableSet()));

    Preconditions.checkArgument(operands.size() > 1);
    this.operands = ImmutableSet.copyOf(operands);
  }

  public static Condition of(Condition c1, Condition c2) {
    Set<Condition> operands = new HashSet<>();
    if (c1 instanceof Conjunction) {
      operands.addAll(((Conjunction) c1).operands);
    } else {
      operands.add(c1);
    }
    if (c2 instanceof Conjunction) {
      operands.addAll(((Conjunction) c2).operands);
    } else {
      operands.add(c2);
    }

    if (operands.size() == 1) {
      return operands.iterator().next();
    } else {
      return new Conjunction(operands).intern();
    }
  }

  @Override
  public boolean test(State state) {
    return operands.stream().allMatch(c -> c.test(state));
  }

  @Override
  public int hashCode() {
    return Conjunction.class.hashCode() ^ operands.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Conjunction)) {
      return false;
    }
    return operands.equals(((Conjunction) o).operands);
  }
}
