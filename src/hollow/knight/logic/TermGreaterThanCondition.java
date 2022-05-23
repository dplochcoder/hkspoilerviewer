package hollow.knight.logic;

import java.util.Objects;
import com.google.common.collect.ImmutableSet;

/** Tests if a specific Term has a value greater than X, where X is 0 by default. */
public final class TermGreaterThanCondition extends Condition {

  private final Term term;
  private final int greater;

  private TermGreaterThanCondition(Term term, int greater) {
    super(ImmutableSet.of(term));
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
  public boolean test(TermMap values) {
    return values.get(term) > greater;
  }

  public Term term() {
    return term;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    builder.indexTermCondition(term, greater + 1, this);
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, greater);
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
