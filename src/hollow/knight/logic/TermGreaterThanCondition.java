package hollow.knight.logic;

import java.util.Objects;
import java.util.stream.Stream;

/** Tests if a specific Term has a value greater than X, where X is 0 by default. */
public final class TermGreaterThanCondition extends Condition {

  private final Term term;
  private final int greater;

  private TermGreaterThanCondition(Term term, int greater) {
    super(Objects.hash(term, greater));
    this.term = term;
    this.greater = greater;
  }

  public static TermGreaterThanCondition of(Term term) {
    return of(term, 0);
  }

  public static TermGreaterThanCondition of(Term term, int greater) {
    return (TermGreaterThanCondition) (new TermGreaterThanCondition(term, greater)).intern();
  }

  @Override
  public boolean test(Context ctx) {
    return ctx.values().get(term) > greater;
  }

  public Term term() {
    return term;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    builder.indexTermCondition(term, greater + 1, this);
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of(term);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermGreaterThanCondition)) {
      return false;
    }

    TermGreaterThanCondition that = (TermGreaterThanCondition) o;
    return term.equals(that.term) && greater == that.greater;
  }
}
