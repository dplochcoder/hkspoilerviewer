package hollow.knight.logic;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class DarknessCondition extends Condition {

  private final String scene;
  private final int darknessLevel;

  protected DarknessCondition(String scene, int darknessLevel) {
    super(Objects.hash(DarknessCondition.class, scene, darknessLevel));

    this.scene = scene;
    this.darknessLevel = darknessLevel;
  }

  @Override
  public boolean test(Context ctx) {
    return ctx.darkness().darknessLevel(scene).intValue() < darknessLevel;
  }

  @Override
  public boolean permanentlyFalse(ConditionGraph.IndexContext ctx) {
    return true;
  }

  @Override
  public void index(ConditionGraph.Builder builder) {
    throw new UnsupportedOperationException(
        "DarknessConditions are unchanging, and should never be indexed.");
  }

  @Override
  public Stream<Term> locationTerms() {
    return Stream.of();
  }

  @Override
  public String debugString() {
    return "$DarknessLevel[" + scene + "]<" + darknessLevel;
  }

  @Override
  public String debugEvaluation(Context ctx) {
    return "(" + ctx.darkness().darknessLevel(scene).intValue() + "<" + darknessLevel + ")="
        + test(ctx);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof DarknessCondition)) {
      return false;
    }

    DarknessCondition that = (DarknessCondition) o;
    return this.scene.equals(that.scene) && this.darknessLevel == that.darknessLevel;
  }

  private static final String PREFIX = "$DarknessLevel[";
  private static final String SUFFIX = "]";

  public static Optional<Condition> tryParse(Term lTerm, int value) {
    if (!lTerm.name().startsWith(PREFIX) || !lTerm.name().endsWith(SUFFIX)) {
      return Optional.empty();
    }

    String scene = lTerm.name().substring(PREFIX.length(), lTerm.name().length() - SUFFIX.length());
    return Optional.of(new DarknessCondition(scene, value).intern());
  }
}
