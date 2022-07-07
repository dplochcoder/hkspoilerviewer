package hollow.knight.logic;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableSet;

public final class NotchCostCondition extends Condition {

  private final boolean safe;
  private final ImmutableSet<Integer> charmIds;

  protected NotchCostCondition(boolean safe, Set<Integer> charmIds) {
    super(Objects.hash(NotchCostCondition.class, safe, charmIds));

    this.safe = safe;
    this.charmIds = ImmutableSet.copyOf(charmIds);
  }

  private int notchCost(NotchCosts notchCosts) {
    int sum = 0;
    int max = 0;
    for (int charmId : charmIds) {
      int cost = notchCosts.notchCost(charmId);
      sum += cost;
      max = Math.max(max, cost);
    }

    return sum - (safe ? 1 : max);
  }

  @Override
  public boolean test(Context ctx) {
    return ctx.values().get(Term.notches()) > notchCost(ctx.notchCosts());
  }

  @Override
  public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
    return false;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    builder.indexTermCondition(Term.notches(), notchCost(builder.ctx().notchCosts()) + 1, this);
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of();
  }

  @Override
  public String debugString() {
    return "NOTCHES > $NotchCost" + charmIds;
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + ctx.values().get(Term.notches()) + ">" + notchCost(ctx.notchCosts()) + ")="
        + test(ctx);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NotchCostCondition)) {
      return false;
    }

    return charmIds.equals(((NotchCostCondition) o).charmIds);
  }

  private static final String UNSAFE_PREFIX = "$NotchCost[";
  private static final String SAFE_PREFIX = "$SafeNotchCost[";
  private static final String SUFFIX = "]";

  public static Optional<Condition> tryParse(Term lTerm, Term rTerm) {
    if (!lTerm.name().contentEquals("NOTCHES")) {
      return Optional.empty();
    }
    if (!rTerm.name().endsWith(SUFFIX)) {
      return Optional.empty();
    }

    boolean safe;
    String ids;
    if (rTerm.name().startsWith(UNSAFE_PREFIX)) {
      safe = false;
      ids = rTerm.name().substring(UNSAFE_PREFIX.length(), rTerm.name().length() - 1);
    } else {
      safe = true;
      ids = rTerm.name().substring(SAFE_PREFIX.length(), rTerm.name().length() - 1);
    }

    Set<Integer> charmIds =
        Arrays.stream(ids.split(",")).map(Integer::parseInt).collect(ImmutableSet.toImmutableSet());
    return Optional.of(new NotchCostCondition(safe, charmIds).intern());
  }
}
