package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public abstract class CommutativeCondition extends Condition {
  protected final ImmutableSet<Condition> operands;

  protected CommutativeCondition(Set<Condition> operands) {
    super(operands.stream().flatMap(c -> c.locationTerms().stream())
        .collect(ImmutableSet.toImmutableSet()));

    Preconditions.checkArgument(operands.size() > 1);
    this.operands = ImmutableSet.copyOf(operands);
  }

  public abstract boolean isDisjunction();

  protected static <T extends CommutativeCondition> Condition of(Set<Condition> operands,
      Class<T> clazz, Function<Set<Condition>, T> factory) {
    Set<Condition> flatOperands = new HashSet<>();
    for (Condition c : operands) {
      if (c.getClass().equals(clazz)) {
        flatOperands.addAll(((CommutativeCondition) c).operands);
      } else {
        flatOperands.add(c);
      }
    }

    if (operands.size() == 1) {
      return operands.iterator().next();
    } else {
      return factory.apply(operands).intern();
    }
  }

  protected static <T extends CommutativeCondition> Condition of(Condition c1, Condition c2,
      Class<T> clazz, Function<Set<Condition>, T> factory) {
    Set<Condition> set = new HashSet<>();
    set.add(c1);
    set.add(c2);
    return of(set, clazz, factory);
  }

  @Override
  public final int hashCode() {
    return getClass().hashCode() ^ operands.hashCode();
  }

  @Override
  public final boolean equals(Object o) {
    if (!(o instanceof CommutativeCondition)) {
      return false;
    }
    if (!o.getClass().equals(getClass())) {
      return false;
    }
    return operands.equals(((CommutativeCondition) o).operands);
  }

}
