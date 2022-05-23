package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.auto.value.AutoValue;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SortedSetMultimap;

public final class TermConditionIndex {
  @AutoValue
  abstract static class TermIndex {
    public abstract Term term();

    public abstract int index();

    public static TermIndex of(Term term, int index) {
      return new AutoValue_TermConditionIndex_TermIndex(term, index);
    }
  }

  private final SortedSetMultimap<Term, Integer> indexValues =
      MultimapBuilder.hashKeys().treeSetValues().build();
  private final BiMultimap<TermIndex, Condition> conditionIndex = new BiMultimap<>();

  public TermConditionIndex() {}

  public TermConditionIndex(TermConditionIndex copy) {
    indexValues.putAll(copy.indexValues);
    conditionIndex.putAll(copy.conditionIndex);
  }

  public void put(Term term, int index, Condition c) {
    indexValues.put(term, index);
    conditionIndex.put(TermIndex.of(term, index), c);
  }

  public Set<Condition> removeTermIndex(Term term, int index) {
    Set<Condition> conditions = new HashSet<>();
    Set<Integer> indices = new HashSet<>(indexValues.get(term).headSet(index + 1));
    for (int i : indices) {
      indexValues.remove(term, i);

      TermIndex ti = TermIndex.of(term, i);
      conditions.addAll(conditionIndex.removeKey(ti));
    }
    return conditions;
  }

  public void removeCondition(Condition c) {
    for (TermIndex ti : conditionIndex.removeValue(c)) {
      indexValues.remove(ti.term(), ti.index());
    }
  }
}
