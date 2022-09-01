package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Stream;

public interface ItemEffects {
  boolean hasEffectTerm(Term term);

  int getEffectValue(Term term);

  Stream<Term> effectTerms();

  void apply(Condition.MutableContext ctx, Set<Term> dirtyTerms);
}
