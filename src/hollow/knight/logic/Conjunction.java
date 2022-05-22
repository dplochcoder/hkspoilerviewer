package hollow.knight.logic;

import com.google.common.collect.ImmutableSet;

public final class Conjunction extends Condition {
  private final Condition left;
  private final Condition right;

  public Conjunction(Condition left, Condition right) {
    super(ImmutableSet.<Term>builder().addAll(left.terms()).addAll(right.terms()).build());
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean test(State state) {
    return left.test(state) && right.test(state);
  }

  @Override
  public int hashCode() {
    return Conjunction.class.hashCode() + left.hashCode() + right.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Conjunction)) {
      return false;
    }

    Conjunction that = (Conjunction) o;
    return (left.equals(that.left) && right.equals(that.right))
        || (left.equals(that.right) && right.equals(that.left));
  }
}
