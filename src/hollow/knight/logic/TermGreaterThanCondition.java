package hollow.knight.logic;

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

}
