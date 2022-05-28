package hollow.knight.logic;

import java.util.Set;

public final class Conjunction extends CommutativeCondition {
  private Conjunction(Set<Condition> operands) {
    super(Conjunction.class, operands);
  }

  public static Condition of(Condition c1, Condition c2) {
    return CommutativeCondition.of(c1, c2, Conjunction.class, Conjunction::new);
  }

  public static Condition of(Set<Condition> operands) {
    return CommutativeCondition.of(operands, Conjunction.class, Conjunction::new);
  }

  @Override
  public boolean isDisjunction() {
    return false;
  }

  @Override
  public boolean test(Context ctx) {
    return operands.stream().allMatch(c -> c.test(ctx));
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    for (Condition c : operands) {
      if (!builder.index(c)) {
        builder.indexChild(this, c);
      }
    }
  }
}
