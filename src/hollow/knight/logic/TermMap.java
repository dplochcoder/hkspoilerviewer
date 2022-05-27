package hollow.knight.logic;

import java.util.Set;

public interface TermMap {
  Set<Term> terms();

  int get(Term term);

  static TermMap empty() {
    return ImmutableTermMap.empty();
  }
}
