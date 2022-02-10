package hollow.knight.logic;

import com.google.common.collect.ImmutableSet;

public final class Disjunction extends Condition {
  private final Condition left;
  private final Condition right;

  public Disjunction(Condition left, Condition right) {
    super(ImmutableSet.<Term>builder().addAll(left.terms()).addAll(right.terms()).build());
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean test(State state) {
    return left.test(state) || right.test(state);
  }
}
