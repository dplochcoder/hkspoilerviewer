package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.Sets;

// A mutable record of all Condition evaluations, updated incrementally.
// Assumes all conditions can only transition from false -> true, and that term values can only
// increase.
public final class ConditionGraph {

  // All true conditions are stored here.
  private final Set<Condition> trueConditions;

  // Only false conditions are stored here.
  private final BiMultimap<CommutativeCondition, Condition> children;

  // (Term, value) -> Condition index such that Term >= value implies the Condition is true.
  // Only indices for which false terms remain are kept here.
  private final TermConditionIndex index;

  private ConditionGraph(Set<Condition> trueConditions,
      BiMultimap<CommutativeCondition, Condition> children, TermConditionIndex index) {
    this.trueConditions = Sets.newIdentityHashSet();
    this.trueConditions.addAll(trueConditions);

    this.children = new BiMultimap<>(children);
    this.index = new TermConditionIndex(index);
  }

  public boolean test(Condition c) {
    return trueConditions.contains(c);
  }

  public ConditionGraph deepCopy() {
    return new ConditionGraph(this.trueConditions, this.children, this.index);
  }

  // Adds `parent` to `sink` and removes it from the children graph if `parent` has become true.
  private void updateChildren(CommutativeCondition parent, Condition child, Set<Condition> sink) {
    if (parent.isDisjunction()) {
      sink.add(parent);

      // Any child becoming true makes the parent true for a disjunction, so we no longer
      // care about these child edges.
      children.removeKey(parent);
    } else {
      children.remove(parent, child);

      // This is a conjunction. Remove the parent only if it has become empty.
      if (!children.containsKey(parent)) {
        sink.add(parent);
      }
    }
  }

  // Returns the set of Conditions which were false before the update, true after.
  public Set<Condition> update(State newState, Set<Term> updatedTerms) {
    Set<Condition> updates = new HashSet<>();
    for (Term t : updatedTerms) {
      updates.addAll(index.removeTermIndex(t, newState.get(t)));
    }

    Set<Condition> queue = new HashSet<>(updates);
    while (!queue.isEmpty()) {
      Set<Condition> next = new HashSet<>();
      for (Condition c : queue) {
        for (CommutativeCondition cc : new HashSet<>(children.getValue(c))) {
          updateChildren(cc, c, next);
        }
      }

      updates.addAll(next);
      queue = next;
    }

    // We now have the full set of all Conditions that have become true.
    trueConditions.addAll(updates);
    return updates;
  }

  public static Builder builder(Condition.Context ctx) {
    return new Builder(ctx);
  }

  public static final class Builder {
    private final Condition.Context ctx;
    private final Set<Condition> indexed = new HashSet<>();

    private final Set<Condition> trueConditions = new HashSet<>();
    private final BiMultimap<CommutativeCondition, Condition> children = new BiMultimap<>();
    private final TermConditionIndex termIndex = new TermConditionIndex();

    private Builder(Condition.Context ctx) {
      this.ctx = ctx;
    }

    public Condition.Context ctx() {
      return ctx;
    }

    public boolean index(Condition c) {
      if (indexed.add(c)) {
        if (c.test(ctx)) {
          trueConditions.add(c);
          return true;
        } else {
          c.index(this);
          return false;
        }
      } else {
        return trueConditions.contains(c);
      }
    }

    public void indexTermCondition(Term t, int index, Condition c) {
      termIndex.put(t, index, c);
    }

    public boolean indexChild(CommutativeCondition parent, Condition child) {
      boolean childTrue = index(child);
      if (!childTrue) {
        children.put(parent, child);
      }
      return childTrue;
    }

    public ConditionGraph build() {
      return new ConditionGraph(trueConditions, children, termIndex);
    }
  }
}
