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
  public static class Context {
    private final TermMap values;
    private final NotchCosts notchCosts;
    private final DarknessOverrides darknessOverrides;

    public Context(TermMap values, NotchCosts notchCosts, DarknessOverrides darknessOverrides) {
      this.values = values;
      this.notchCosts = notchCosts;
      this.darknessOverrides = darknessOverrides;
    }

    public TermMap values() {
      return values;
    }

    public NotchCosts notchCosts() {
      return notchCosts;
    }

    public DarknessOverrides darkness() {
      return darknessOverrides;
    }
  }

  public static class MutableContext extends Context {
    public MutableContext(MutableTermMap values, NotchCosts notchCosts,
        DarknessOverrides darknessOverrides) {
      super(values, notchCosts, darknessOverrides);
    }

    @Override
    public MutableTermMap values() {
      return (MutableTermMap) super.values();
    }

    public void set(Term t, int value) {
      values().set(t, value);
    }

    public int get(Term t) {
      return values().get(t);
    }

    public void add(Term t, int value) {
      values().add(t, value);;
    }
  }

  private static final Interner<Condition> INTERNER = Interners.newWeakInterner();

  private final int hashCode;

  protected Condition(int hashCode) {
    this.hashCode = hashCode;
  }

  // Tests whether this condition evaluates to true.
  public abstract boolean test(Context ctx);

  public final boolean test(TermMap values, NotchCosts notchCosts, DarknessOverrides darkness) {
    return test(new Context(values, notchCosts, darkness));
  }

  // Invoke appropriate methods on `builder` to index this Condition.
  // Never invoked on a Condition already evaluating to true.
  public abstract void index(ConditionGraph.Builder builder);

  // Returns true if this Condition is currently false and will never evaluate to true under
  // ordinary progression. (For instance, a check on initial conditions.)
  // Never invoked on a Condition already evaluating to true.
  public abstract boolean permanentlyFalse(ConditionGraph.IndexContext ctx);

  // Generic terms in this condition possibly related to location; used to infer scenes.
  // May contain duplicates, not efficient.
  public abstract Stream<Term> locationTerms();

  public abstract String debugString();

  public abstract String debugEvaluation(Context ctx);

  public final String debugEvaluation(TermMap values, NotchCosts notchCosts,
      DarknessOverrides darkness) {
    return debugEvaluation(new Context(values, notchCosts, darkness));
  }

  @Override
  public final String toString() {
    return "Condition[" + debugString() + "]";
  }

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
    public boolean test(Context ctx) {
      return value;
    }

    @Override
    public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
      return true;
    }

    @Override
    public void index(ConditionGraph.Builder builder) {}

    @Override
    public Stream<Term> locationTerms() {
      return Stream.of();
    }

    @Override
    public String debugString() {
      return value ? "TRUE" : "FALSE";
    }

    @Override
    public String debugEvaluation(Context ctx) {
      return debugString();
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
