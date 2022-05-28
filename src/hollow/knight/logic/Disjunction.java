package hollow.knight.logic;

import java.util.Set;

public final class Disjunction extends CommutativeCondition {
  private Disjunction(Set<Condition> operands) {
    super(Disjunction.class, operands);
  }

  public static Condition of(Condition c1, Condition c2) {
    return CommutativeCondition.of(c1, c2, Disjunction.class, Disjunction::new);
  }

  public static Condition of(Set<Condition> operands) {
    return CommutativeCondition.of(operands, Disjunction.class, Disjunction::new);
  }

  @Override
  public boolean isDisjunction() {
    return true;
  }

  @Override
  public boolean test(Context ctx) {
    return operands.stream().anyMatch(c -> c.test(ctx));
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    for (Condition c : operands) {
      builder.index(c);
      builder.indexChild(this, c);
    }
  }

}
