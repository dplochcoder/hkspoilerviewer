package hollow.knight.logic;

import java.util.stream.Stream;

public final class SplitCloakItemEffects implements ItemEffects {

  private static final Term LEFT_DASH = Term.create("LEFTDASH");
  private static final Term RIGHT_DASH = Term.create("RIGHTDASH");

  private final boolean leftBiased;

  public SplitCloakItemEffects(boolean leftBiased) {
    this.leftBiased = leftBiased;
  }

  @Override
  public boolean hasEffectTerm(Term term) {
    return term.equals(LEFT_DASH) || term.equals(RIGHT_DASH);
  }

  @Override
  public int getEffectValue(Term term) {
    return hasEffectTerm(term) ? 1 : 0;
  }

  @Override
  public Stream<Term> effectTerms() {
    return Stream.of(LEFT_DASH, RIGHT_DASH);
  }

  @Override
  public void apply(MutableTermMap ctx) {
    boolean hasLeft = ctx.get(LEFT_DASH) > 0;
    boolean hasRight = ctx.get(RIGHT_DASH) > 0;
    boolean hasShade = ctx.get(LEFT_DASH) >= 2 || ctx.get(RIGHT_DASH) >= 2;

    if (hasLeft && hasRight) {
      if (!hasShade) {
        ctx.set(LEFT_DASH, 2);
        ctx.set(RIGHT_DASH, 2);
      }
    } else if (leftBiased) {
      ctx.set(LEFT_DASH, !hasLeft && !hasShade ? 1 : 2);
    } else {
      ctx.set(RIGHT_DASH, !hasRight && !hasShade ? 1 : 2);
    }
  }
}
