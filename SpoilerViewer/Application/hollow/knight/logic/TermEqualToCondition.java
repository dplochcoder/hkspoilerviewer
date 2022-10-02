package hollow.knight.logic;

import java.util.Objects;
import java.util.stream.Stream;

public final class TermEqualToCondition extends Condition {
  private final Term term;
  private final int value;

  private TermEqualToCondition(Term term, int value) {
    super(Objects.hash(term, value));
    this.term = term;
    this.value = value;
  }

  public static TermEqualToCondition of(Term term, int value) {
    return (TermEqualToCondition) (new TermEqualToCondition(term, value)).intern();
  }

  @Override
  public boolean test(Context ctx) {
    return ctx.values().get(term) == value;
  }

  @Override
  public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
    // Equal-to conditions are always based on initial state.
    return true;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    // An equal-to condition is always based on initial state, and should never change.
    // So we don't index it.
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of(term);
  }

  @Override
  public String debugString() {
    return term.name() + "=" + value;
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + ctx.values().get(term) + "=" + value + ")=" + test(ctx);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermEqualToCondition)) {
      return false;
    }

    TermEqualToCondition that = (TermEqualToCondition) o;
    return term.equals(that.term) && value == that.value;
  }
}