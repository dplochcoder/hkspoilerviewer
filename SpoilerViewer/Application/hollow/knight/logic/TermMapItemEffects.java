package hollow.knight.logic;

import java.util.stream.Stream;

public final class TermMapItemEffects implements ItemEffects {

  private final ImmutableTermMap effects;
  private final ImmutableTermMap caps;

  public TermMapItemEffects(TermMap effects, TermMap caps) {
    this.effects = ImmutableTermMap.copyOf(effects);
    this.caps = ImmutableTermMap.copyOf(caps);
  }

  @Override
  public boolean hasEffectTerm(Term term) {
    return effects.get(term) > 0;
  }

  @Override
  public int getEffectValue(Term term) {
    return effects.get(term);
  }

  @Override
  public Stream<Term> effectTerms() {
    return effects.terms().stream();
  }

  @Override
  public void apply(MutableTermMap ctx) {
    for (Term t : effects.terms()) {
      int cap = Integer.MAX_VALUE;
      if (caps.terms().contains(t)) {
        cap = caps.get(t);
      }

      int newVal = Math.min(ctx.get(t) + effects.get(t), cap);
      ctx.set(t, newVal);
    }
  }
}
