package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final StateContext ctx;

  private final MutableTermMap termValues = new MutableTermMap();
  private ConditionGraph graph = null;
  private final Set<ItemCheck> obtains = new HashSet<>();

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

  public Stream<ItemCheck> obtained() {
    return obtains.stream();
  }

  public Stream<ItemCheck> unobtained() {
    return ctx.checks().allChecks().filter(id -> !obtains.contains(id));
  }

  public Stream<ItemCheck> accessible() {
    return potentialState.obtained();
  }

  public boolean isAcquired(ItemCheck check) {
    return obtains.contains(check);
  }

  public int get(Term term) {
    return termValues.get(term);
  }

  public void set(Term term, int value) {
    dirtyTerms.add(term);
    termValues.set(term, value);
  }

  public void acquireCheck(ItemCheck check) {
    if (!obtains.add(check)) {
      return;
    }

    if (potentialState != null) {
      potentialState.acquireCheck(check);
    }

    check.item().apply(this);
  }

  public boolean test(Condition c) {
    if (dirtyTerms.isEmpty() && graph != null) {
      return graph.test(c);
    } else {
      return c.test(termValues(), ctx().notchCosts());
    }
  }

  public boolean purchaseTest(Condition c) {
    return potentialState.test(c);
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> newWaypoints = new HashSet<>();
    Set<ItemCheck> newChecks = new HashSet<>();
    if (graph == null) {
      ConditionGraph.Builder builder =
          ConditionGraph.builder(new Condition.Context(termValues(), ctx().notchCosts()));
      for (Term t : ctx.waypoints().allWaypoints()) {
        if (builder.index(ctx.waypoints().getCondition(t))) {
          newWaypoints.add(t);
        }
      }
      ctx().checks().allChecks().forEach(c -> {
        builder.index(c.location().accessCondition());
        if (builder.index(c.condition())) {
          newChecks.add(c);
        }
      });
      graph = builder.build();
    } else {
      for (Condition c : graph.update(this, dirtyTerms)) {
        newWaypoints.addAll(ctx.waypoints().getTerms(c));
        ctx().checks().getByCondition(c).forEach(newChecks::add);
      }
      if (potentialState != null) {
        potentialState.graph.update(potentialState, dirtyTerms);
      }
    }
    dirtyTerms.clear();

    if (potentialState == null) {
      potentialState = this.deepCopy();
      toleranceValues =
          new SumTermMap(ImmutableList.of(potentialState.termValues, ctx.tolerances()));
    }

    // Iterate waypoints.
    while (!newWaypoints.isEmpty()) {
      for (Term t : newWaypoints) {
        set(t, 1);
        potentialState.set(t, 1);
      }

      newWaypoints.clear();
      for (Condition c : graph.update(this, dirtyTerms)) {
        newWaypoints.addAll(ctx.waypoints().getTerms(c));
        ctx().checks().getByCondition(c).forEach(newChecks::add);
      }
      potentialState.graph.update(potentialState, dirtyTerms);
      dirtyTerms.clear();
    }

    // Determine all accessible waypoints and checks.
    Supplier<Set<Term>> costTerms =
        () -> Sets.intersection(Term.costTerms(), potentialState.dirtyTerms);
    for (Condition c : potentialState.graph.update(potentialState, costTerms.get())) {
      newWaypoints.addAll(ctx().waypoints().getTerms(c));
      ctx().checks().getByCondition(c).forEach(newChecks::add);
    }
    potentialState.dirtyTerms.clear();

    while (!newWaypoints.isEmpty() || !newChecks.isEmpty()) {
      if (!newWaypoints.isEmpty()) {
        // Evaluate new waypoints.
        newWaypoints.forEach(t -> potentialState.set(t, 1));
        potentialState.dirtyTerms.clear();

        Set<Term> copy = new HashSet<>(newWaypoints);
        newWaypoints.clear();

        for (Condition c : potentialState.graph.update(potentialState, copy)) {
          newWaypoints.addAll(ctx().waypoints().getTerms(c));
          ctx().checks().getByCondition(c).forEach(newChecks::add);
        }
      }

      if (!newChecks.isEmpty()) {
        newChecks.forEach(potentialState::acquireCheck);
        newChecks.clear();

        for (Condition c : potentialState.graph.update(potentialState, costTerms.get())) {
          newWaypoints.addAll(ctx().waypoints().getTerms(c));
          ctx().checks().getByCondition(c).forEach(newChecks::add);
        }
        potentialState.dirtyTerms.clear();
      }
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
