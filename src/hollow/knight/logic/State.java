package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final StateContext ctx;

  private final MutableTermMap termValues = new MutableTermMap();
  private ConditionGraph graph = null;
  private final Set<CheckId> obtains = new HashSet<>();

  private final Set<Term> dirtyTerms = new HashSet<>();

  // A parallel State which acquires every accessible item, for computing purchase logic.
  private State potentialState = null;
  private TermMap toleranceValues = null;

  State(StateContext ctx) {
    this.ctx = ctx;

    // TRUE is always set.
    set(Term.true_(), 1);
  }

  public StateContext ctx() {
    return ctx;
  }

  public ImmutableSet<CheckId> unobtained() {
    return ctx.checks().allChecks().map(c -> c.id()).filter(id -> !obtains.contains(id))
        .collect(ImmutableSet.toImmutableSet());
  }

  public boolean isAcquired(CheckId id) {
    return obtains.contains(id);
  }

  public int get(Term term) {
    return termValues.get(term);
  }

  public void set(Term term, int value) {
    dirtyTerms.add(term);
    termValues.set(term, value);
  }

  public void dumpTerms() {
    for (Term t : termValues.terms().stream().sorted((t1, t2) -> t1.name().compareTo(t2.name()))
        .collect(ImmutableList.toImmutableList())) {
      System.err.println(t + ": " + termValues.get(t));
    }
  }

  public void acquireCheck(CheckId id) {
    if (!obtains.add(id)) {
      return;
    }

    if (potentialState != null) {
      potentialState.acquireCheck(id);
    }

    ctx.checks().get(id).item().apply(this);
  }

  public boolean test(Condition c) {
    if (dirtyTerms.isEmpty() && graph != null) {
      return graph.test(c);
    } else {
      return c.test(termValues());
    }
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> newWaypoints = new HashSet<>();
    Set<CheckId> newChecks = new HashSet<>();
    if (graph == null) {
      ConditionGraph.Builder builder = ConditionGraph.builder(termValues());
      for (Term t : ctx.waypoints().allWaypoints()) {
        if (builder.index(ctx.waypoints().getCondition(t))) {
          newWaypoints.add(t);
        }
      }
      ctx().checks().allChecks().forEach(c -> {
        if (builder.index(c.condition())) {
          newChecks.add(c.id());
        }
      });
      graph = builder.build();
    } else {
      for (Condition c : graph.update(this, dirtyTerms)) {
        newWaypoints.addAll(ctx.waypoints().getTerms(c));
        ctx().checks().getByCondition(c).forEach(check -> newChecks.add(check.id()));
      }
    }
    dirtyTerms.clear();

    // Iterate waypoints.
    while (!newWaypoints.isEmpty()) {
      for (Term t : newWaypoints) {
        set(t, 1);
      }

      newWaypoints.clear();
      for (Condition c : graph.update(this, dirtyTerms)) {
        newWaypoints.addAll(ctx.waypoints().getTerms(c));
        ctx().checks().getByCondition(c).forEach(check -> newChecks.add(check.id()));
      }
      dirtyTerms.clear();
    }

    if (potentialState == null) {
      potentialState = this.deepCopy();
      toleranceValues =
          new SumTermMap(ImmutableList.of(potentialState.termValues, ctx.tolerances()));
    }

    // Acquire all accessible item checks.
    newChecks.removeAll(potentialState.obtains);
    while (!newChecks.isEmpty()) {
      newChecks.forEach(potentialState::acquireCheck);
      newChecks.clear();

      for (Condition c : potentialState.graph.update(potentialState, potentialState.dirtyTerms)) {
        ctx().checks().getByCondition(c).forEach(check -> newChecks.add(check.id()));
      }
      potentialState.dirtyTerms.clear();
    }
  }

  public TermMap termValues() {
    return termValues;
  }

  public TermMap purchaseTermValues() {
    return toleranceValues;
  }

  public State deepCopy() {
    State copy = new State(ctx);
    copy.termValues.clear();
    this.termValues.terms().forEach(t -> copy.termValues.set(t, get(t)));
    copy.obtains.addAll(this.obtains);
    copy.dirtyTerms.clear();
    copy.dirtyTerms.addAll(this.dirtyTerms);
    if (graph != null) {
      copy.graph = graph.deepCopy();
    }
    if (potentialState != null) {
      copy.potentialState = this.potentialState.deepCopy();
      copy.toleranceValues =
          new SumTermMap(ImmutableList.of(copy.potentialState.termValues, ctx.tolerances()));
    }
    return copy;
  }

}
