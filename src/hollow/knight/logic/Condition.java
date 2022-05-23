package hollow.knight.logic;

import java.util.stream.Stream;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;

/**
 * A generic expression for whether something is reachable, given the current State of obtains and
 * locations.
 * 
 * <p>
 * Conditions are interned and immutable. To re-evaluate complex conditions efficiently given known
 * assumptions about logic progression, use a ConditionGraph.
 */
public abstract class Condition {
  private static final Interner<Condition> INTERNER = Interners.newWeakInterner();

  private final int hashCode;

  protected Condition(int hashCode) {
    this.hashCode = hashCode;
  }

  // Tests whether this condition evaluates to true.
  public abstract boolean test(TermMap values);

  // Invoke appropriate methods on `builder` to index this Condition.
  // Never invoked on a Condition already evaluating to true.
  public abstract void index(ConditionGraph.Builder builder);

  // Generic terms in this condition possibly related to location; used to infer scenes.
  // May contain duplicates, not efficient.
  public abstract Stream<Term> locationTerms();

  @Override
  public final int hashCode() {
    return hashCode;
  }

  @Override
  public abstract boolean equals(Object o);

  public final Condition intern() {
    return INTERNER.intern(this);
  }

  private static final class ConstantCondition extends Condition {
    private final boolean value;

    private ConstantCondition(boolean value) {
      super(ConstantCondition.class.hashCode() ^ Boolean.hashCode(value));
      this.value = value;
    }

    @Override
    public boolean test(TermMap values) {
      return value;
    }

    @Override
    public void index(ConditionGraph.Builder builder) {}

    @Override
    public Stream<Term> locationTerms() {
      return Stream.of();
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

  private static final Condition CAN_REPLENISH_GEO =
      TermGreaterThanCondition.of(Term.canReplenishGeo(), 0);

  public static Condition canReplenishGeo() {
    return CAN_REPLENISH_GEO;
  }
}
