package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MutableTermMap implements TermMap {
  private final Map<Term, Integer> values = new HashMap<>();

  @Override
  public Set<Term> terms() {
    return values.keySet();
  }

  @Override
  public int get(Term term) {
    return values.getOrDefault(term, 0);
  }

  public void set(Term t, int value) {
    if (value == 0) {
      values.remove(t);
    } else {
      values.put(t, value);
    }
  }

  public void add(Term t, int value) {
    set(t, get(t) + value);
  }

  public void add(TermMap other) {
    for (Term t : other.terms()) {
      set(t, get(t) + other.get(t));
    }
  }

  public void clear() {
    values.clear();
  }
}
