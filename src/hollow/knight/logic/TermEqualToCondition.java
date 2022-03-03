package hollow.knight.logic;

import com.google.common.collect.ImmutableSet;

public final class TermEqualToCondition extends Condition {
  private final Term term;
  private final int value;

  public TermEqualToCondition(Term term, int value) {
    super(ImmutableSet.of(term));
    this.term = term;
    this.value = value;
  }

  @Override
  public boolean test(State state) {
    return state.get(term) == value;
  }
}
