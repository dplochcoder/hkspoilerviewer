package hollow.knight.logic;

import java.util.stream.Stream;

public interface ItemEffects {
  boolean hasEffectTerm(Term term);

  int getEffectValue(Term term);

  Stream<Term> effectTerms();

  void apply(MutableTermMap ctx);
}
