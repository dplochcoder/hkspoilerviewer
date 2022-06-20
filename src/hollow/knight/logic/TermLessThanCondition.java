package hollow.knight.logic;

import java.util.Objects;
import java.util.stream.Stream;

/** Tests if a specific Term has a value less than X. */
public final class TermLessThanCondition extends Condition {

  private final Term term;
  private final int less;

  private TermLessThanCondition(Term term, int less) {
    super(Objects.hash(term, less));
    this.term = term;
    this.less = less;
  }

  public static TermLessThanCondition of(Term term, int less) {
    return (TermLessThanCondition) (new TermLessThanCondition(term, less)).intern();
  }

  @Override
  public boolean test(Context ctx) {
    return ctx.values().get(term) < less;
  }

  @Override
  public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
    // Less-than conditions should never be part of indexed logic, since they can transition true ->
    // false.
    throw new UnsupportedOperationException("Less-than operations should not be indexed");
  }

  public Term term() {
    return term;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    // Less-than conditions should never be part of indexed logic, since they can transition true ->
    // false.
    throw new UnsupportedOperationException("Less-than operations should not be indexed");
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of(term);
  }

  @Override
  public String debugString() {
    return term.name() + "<" + less;
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + ctx.values().get(term) + "<" + less + ")=" + test(ctx);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermLessThanCondition)) {
      return false;
    }

    TermLessThanCondition that = (TermLessThanCondition) o;
    return term.equals(that.term) && less == that.less;
  }
}
