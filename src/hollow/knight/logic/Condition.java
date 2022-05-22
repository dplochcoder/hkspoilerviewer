package hollow.knight.logic;

import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * A generic expression for whether something is reachable, given the current State of obtains and
 * locations.
 */
public abstract class Condition {
  private static final Interner<Condition> INTERNER = Interners.newWeakInterner();

  private final ImmutableSet<Term> terms;

  protected Condition(Set<Term> terms) {
    this.terms = ImmutableSet.copyOf(terms);
  }

  public abstract boolean test(State state);

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object o);

  public final ImmutableSet<Term> terms() {
    return terms;
  }

  public final Condition intern() {
    return INTERNER.intern(this);
  }

  private static final class ConstantCondition extends Condition {
    private final boolean value;

    private ConstantCondition(boolean value) {
      super(ImmutableSet.of());
      this.value = value;
    }

    @Override
    public boolean test(State state) {
      return value;
    }

    @Override
    public int hashCode() {
      return Boolean.hashCode(value) + ConstantCondition.class.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      // Only two instances of this class exist, so we can use identity equals.
      return this == o;
    }
  };

  private static final Condition ALWAYS_TRUE = new ConstantCondition(true).intern();

  public static Condition alwaysTrue() {
    return ALWAYS_TRUE;
  }

  private static final Condition ALWAYS_FALSE = new ConstantCondition(false).intern();

  public static Condition alwaysFalse() {
    return ALWAYS_FALSE;
  }
}
