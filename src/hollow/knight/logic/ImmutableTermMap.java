package hollow.knight.logic;

import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class ImmutableTermMap implements TermMap {
  private final ImmutableMap<Term, Integer> terms;

  private ImmutableTermMap(Map<Term, Integer> terms) {
    this.terms = ImmutableMap.copyOf(terms);
  }

  @Override
  public ImmutableSet<Term> terms() {
    return terms.keySet();
  }

  @Override
  public int get(Term term) {
    return terms.getOrDefault(term, 0);
  }

  public static ImmutableTermMap copyOf(TermMap map) {
    if (map instanceof ImmutableTermMap) {
      return (ImmutableTermMap) map;
    } else {
      ImmutableMap.Builder<Term, Integer> builder = ImmutableMap.builder();
      for (Term t : map.terms()) {
        builder.put(t, map.get(t));
      }
      return new ImmutableTermMap(builder.build());
    }
  }
}
