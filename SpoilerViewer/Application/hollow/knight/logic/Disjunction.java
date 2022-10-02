package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Collectors;

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
  public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
    return operands.stream().allMatch(c -> c.permanentlyFalse(ctx));
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    for (Condition c : operands) {
      builder.index(c);
      builder.indexChild(this, c);
    }
  }

  @Override
  public String debugString() {
    return operands.stream().map(Condition::debugString)
        .collect(Collectors.joining(") || (", "(", ")"));
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + operands.stream().map(c -> c.debugEvaluation(ctx))
        .collect(Collectors.joining(") || (", "(", ")")) + ")=" + test(ctx);
  }
}