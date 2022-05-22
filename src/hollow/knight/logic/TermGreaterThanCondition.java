package hollow.knight.logic;

import java.util.Objects;
import com.google.common.collect.ImmutableSet;

/** Tests if a specific Term has a value greater than X, where X is 0 by default. */
public final class TermGreaterThanCondition extends Condition {

  private final Term term;
  private final int greater;

  public TermGreaterThanCondition(Term term) {
    this(term, 0);
  }

  public TermGreaterThanCondition(Term term, int greater) {
    super(ImmutableSet.of(term));
    this.term = term;
    this.greater = greater;
  }

  @Override
  public boolean test(State state) {
    return state.get(term) > greater;
  }

  public Term term() {
    return term;
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, greater);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof TermEqualToCondition)) {
      return false;
    }

    TermGreaterThanCondition that = (TermGreaterThanCondition) o;
    return term.equals(that.term) && greater == that.greater;
  }
}
