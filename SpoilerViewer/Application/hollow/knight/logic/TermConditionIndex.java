package hollow.knight.logic;

import java.util.Set;
import java.util.SortedSet;
import com.google.auto.value.AutoValue;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Sets;
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
  private final BiMap<TermIndex, Condition> conditionIndex = HashBiMap.create();

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
    Set<Condition> conditions = Sets.newIdentityHashSet();

    SortedSet<Integer> affected = indexValues.get(term).headSet(index + 1);
    for (int i : affected) {
      TermIndex ti = TermIndex.of(term, i);
      conditions.add(conditionIndex.remove(ti));
    }
    affected.clear();

    return conditions;
  }

  public void removeCondition(Condition c) {
    conditionIndex.inverse().remove(c);
  }
}