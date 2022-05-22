package hollow.knight.logic;

import java.util.Objects;
import com.google.common.collect.ImmutableSet;

public final class TermEqualToCondition extends Condition {
  private final Term term;
  private final int value;

  private TermEqualToCondition(Term term, int value) {
    super(ImmutableSet.of(term));
    this.term = term;
    this.value = value;
  }

  public static TermEqualToCondition of(Term term, int value) {
    return (TermEqualToCondition) (new TermEqualToCondition(term, value)).intern();
  }

  @Override
  public boolean test(State state) {
    return state.get(term) == value;
  }

  @Override
  public int hashCode() {
    return Objects.hash(term, value);
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
