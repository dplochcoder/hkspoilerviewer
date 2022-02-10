package hollow.knight.logic;

import java.util.Set;
import com.google.common.collect.ImmutableSet;

/**
 * A generic expression for whether something is reachable, given the current State of obtains and
 * locations.
 */
public abstract class Condition {
  private final ImmutableSet<Term> terms;
  
  protected Condition(Set<Term> terms) {
    this.terms = ImmutableSet.copyOf(terms);
  }

  public abstract boolean test(State state);

  public final ImmutableSet<Term> terms() {
    return terms;
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
  };

  private static final Condition ALWAYS_TRUE = new ConstantCondition(true);
  public static Condition alwaysTrue() {
    return ALWAYS_TRUE;
  }

  private static final Condition ALWAYS_FALSE = new ConstantCondition(false);
  public static Condition alwaysFalse() {
    return ALWAYS_FALSE;
  }
}
