package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public final class Disjunction extends Condition {
  private final ImmutableSet<Condition> operands;

  private Disjunction(Set<Condition> operands) {
    super(
        operands.stream().flatMap(c -> c.terms().stream()).collect(ImmutableSet.toImmutableSet()));

    Preconditions.checkArgument(operands.size() > 1);
    this.operands = ImmutableSet.copyOf(operands);
  }

  public static Condition of(Condition c1, Condition c2) {
    Set<Condition> operands = new HashSet<>();
    if (c1 instanceof Disjunction) {
      operands.addAll(((Disjunction) c1).operands);
    } else {
      operands.add(c1);
    }
    if (c2 instanceof Disjunction) {
      operands.addAll(((Disjunction) c2).operands);
    } else {
      operands.add(c2);
    }

    if (operands.size() == 1) {
      return operands.iterator().next();
    } else {
      return new Disjunction(operands).intern();
    }
  }

  @Override
  public boolean test(State state) {
    return operands.stream().anyMatch(c -> c.test(state));
  }

  @Override
  public int hashCode() {
    return Disjunction.class.hashCode() ^ operands.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Disjunction)) {
      return false;
    }
    return operands.equals(((Disjunction) o).operands);
  }
}
