package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Collectors;

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

  @Override
  public String debugString() {
    return operands.stream().map(Condition::debugString)
        .collect(Collectors.joining(") && (", "(", ")"));
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + operands.stream().map(c -> c.debugEvaluation(ctx))
        .collect(Collectors.joining(") && (", "(", ")")) + ")=" + test(ctx);
  }
}
