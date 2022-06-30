package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;

/** Mutable state of a run; can be deep-copied. */
public class State {
  public enum TransitionStrategy {
    NONE, VANILLA, ALL;

    public boolean autoAcquire(ItemCheck check) {
      switch (this) {
        case NONE:
          return false;
        case VANILLA:
          return check.isTransition() && check.vanilla();
        case ALL:
          return check.isTransition();
      }
      throw new AssertionError("Impossible: " + this);
    }
  }

  private final StateContext ctx;
  private TransitionStrategy transitionStrategy = TransitionStrategy.VANILLA;

  private final MutableTermMap termValues = new MutableTermMap();
  private ConditionGraph graph = null;
  private final Set<ItemCheck> obtains = new HashSet<>();
  private final Set<ItemCheck> accessibleUnobtained = new HashSet<>();

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

  public void setTransitionStrategy(TransitionStrategy transitionStrategy) {
    this.transitionStrategy = transitionStrategy;
  }

  public Stream<ItemCheck> obtained() {
    return obtains.stream();
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

  private void acquireWaypoint(Term waypoint) {
    if (get(waypoint) == 0) {
      set(waypoint, 1);
      if (potentialState != null) {
        potentialState.acquireWaypoint(waypoint);
      }
    }
  }

  public void acquireCheck(ItemCheck check) {
    if (!obtains.add(check)) {
      return;
    }
    accessibleUnobtained.remove(check);

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

  private void updateGraph(Set<Term> updateTerms, Set<Term> waypointsOut,
      Set<ItemCheck> checksOut) {
    for (Condition c : graph.update(this, updateTerms)) {
      waypointsOut.addAll(ctx().waypoints().getTerms(c));
      ctx().checks().getByCondition(c).forEach(checksOut::add);
    }
  }

  // Recursively acquire waypoints (all) and checks that match the predicate.
  // `newWaypoints` and `newChecks` are iterated on until empty. Checks deemed accessible but
  // not acquired are placed into `unacquiredSink`.
  private void recursivelyUpdateGraph(Set<Term> newWaypoints, Set<ItemCheck> newChecks,
      Set<ItemCheck> unacquiredSink, Predicate<ItemCheck> acquire, Set<Term> checkUpdateTerms) {
    while (!newWaypoints.isEmpty() || !newChecks.isEmpty()) {
      if (!newWaypoints.isEmpty()) {
        // Evaluate new waypoints.
        newWaypoints.forEach(this::acquireWaypoint);
        dirtyTerms.clear();

        Set<Term> copy = new HashSet<>(newWaypoints);
        newWaypoints.clear();

        updateGraph(copy, newWaypoints, newChecks);
      }

      if (!newChecks.isEmpty()) {
        for (ItemCheck check : newChecks) {
          if (acquire.test(check)) {
            acquireCheck(check);
            unacquiredSink.remove(check);
          } else {
            unacquiredSink.add(check);
          }
        }
        newChecks.clear();

        updateGraph(checkUpdateTerms, newWaypoints, newChecks);
        dirtyTerms.clear();
      }
    }
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> newWaypoints = new HashSet<>();
    Set<ItemCheck> newChecks = new HashSet<>();
    if (graph == null) {
      ConditionGraph.Builder builder = ConditionGraph.builder(new ConditionGraph.IndexContext(
          new Condition.Context(termValues(), ctx().notchCosts()), ctx()::isMutableTerm));
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
      updateGraph(dirtyTerms, newWaypoints, newChecks);
      dirtyTerms.clear();
    }

    recursivelyUpdateGraph(newWaypoints, newChecks, accessibleUnobtained,
        transitionStrategy::autoAcquire, dirtyTerms);

    if (potentialState == null) {
      potentialState = this.deepCopy();
      toleranceValues =
          new SumTermMap(ImmutableList.of(potentialState.termValues, ctx.tolerances()));
    } else {
      potentialState.updateGraph(potentialState.dirtyTerms, newWaypoints, newChecks);
      potentialState.dirtyTerms.clear();
    }

    potentialState.recursivelyUpdateGraph(newWaypoints, newChecks, accessibleUnobtained,
        c -> !c.isTransition() || transitionStrategy.autoAcquire(c), Term.costTerms());
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
    copy.accessibleUnobtained.clear();
    copy.accessibleUnobtained.addAll(this.accessibleUnobtained);
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
