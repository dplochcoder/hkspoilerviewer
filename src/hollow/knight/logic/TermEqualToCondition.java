package hollow.knight.logic;

import java.util.Objects;
import com.google.common.collect.ImmutableSet;

public final class TermEqualToCondition extends Condition {
  private final Term term;
  private final int value;

  private TermEqualToCondition(Term term, int value) {
    super(ImmutableSet.of(), Objects.hash(term, value));
    this.term = term;
    this.value = value;
  }

  public static TermEqualToCondition of(Term term, int value) {
    return (TermEqualToCondition) (new TermEqualToCondition(term, value)).intern();
  }

  @Override
  public boolean test(TermMap values) {
    return values.get(term) == value;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    // An equal-to condition is always based on initial state, and should never change.
    // So we don't index it.
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
