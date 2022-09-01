package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Stream;
import com.google.common.collect.Streams;

public final class TermMapItemEffects implements ItemEffects {

  private final Condition logic;
  private final ImmutableTermMap trueEffects;
  private final ImmutableTermMap falseEffects;
  private final ImmutableTermMap caps;

  public TermMapItemEffects(Condition logic, TermMap trueEffects, TermMap falseEffects,
      TermMap caps) {
    this.logic = logic;
    this.trueEffects = ImmutableTermMap.copyOf(trueEffects);
    this.falseEffects = ImmutableTermMap.copyOf(falseEffects);
    this.caps = ImmutableTermMap.copyOf(caps);
  }

  @Override
  public boolean hasEffectTerm(Term term) {
    return trueEffects.get(term) + falseEffects.get(term) > 0;
  }

  @Override
  public int getEffectValue(Term term) {
    return trueEffects.get(term);
  }

  @Override
  public Stream<Term> effectTerms() {
    return Streams.concat(trueEffects.terms().stream(), falseEffects.terms().stream()).distinct();
  }

  @Override
  public void apply(Condition.MutableContext ctx, Set<Term> dirtyTerms) {
    // We can't do state.test() here because item conditions don't necessarily hold to the false ->
    // true paradigm.
    TermMap effects = logic.test(ctx) ? trueEffects : falseEffects;

    for (Term t : effects.terms()) {
      int cap = Integer.MAX_VALUE;
      if (caps.terms().contains(t)) {
        cap = caps.get(t);
      }

      int newVal = Math.min(ctx.get(t) + effects.get(t), cap);
      ctx.set(t, newVal);
      dirtyTerms.add(t);
    }
  }
}
