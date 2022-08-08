package hollow.knight.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableSet;

// A view of multiple TermMaps added together.
public final class SumTermMap implements TermMap {
  private final List<TermMap> addends;

  public SumTermMap(List<? extends TermMap> addends) {
    this.addends = new ArrayList<>(addends);
  }

  @Override
  public Set<Term> terms() {
    return addends.stream().flatMap(m -> m.terms().stream()).distinct()
        .filter(t -> this.get(t) != 0).collect(ImmutableSet.toImmutableSet());
  }

  @Override
  public int get(Term term) {
    return addends.stream().mapToInt(m -> m.get(term)).sum();
  }
}
