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
  public void apply(State state) {
    boolean hasLeft = state.get(LEFT_DASH) > 0;
    boolean hasRight = state.get(RIGHT_DASH) > 0;
    boolean hasShade = state.get(LEFT_DASH) > 2 || state.get(RIGHT_DASH) > 2;

    if (hasLeft && hasRight) {
      if (!hasShade) {
        state.set(LEFT_DASH, 2);
        state.set(RIGHT_DASH, 2);
      }
    } else if (leftBiased) {
      state.set(LEFT_DASH, !hasLeft && !hasShade ? 1 : 2);
    } else {
      state.set(RIGHT_DASH, !hasRight && !hasShade ? 1 : 2);
    }
  }
}
