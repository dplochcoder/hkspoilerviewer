package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;

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
  private ConditionGraph graph;
  private final Set<ItemCheck> obtains = new HashSet<>();
  private final Set<ItemCheck> accessibleUnobtained = new HashSet<>();

  private final Set<Term> dirtyTerms = new HashSet<>();

  // A parallel State which acquires every accessible item, for computing purchase logic.
  private final MutableTermMap potentialTermValues = new MutableTermMap();
  private final TermMap toleranceValues;

  public State(StateContext ctx) {
    this.ctx = ctx;
    this.toleranceValues = new SumTermMap(ImmutableList.of(potentialTermValues, ctx.tolerances()));

    // TRUE is always set.
    set(Term.true_(), 1);

    ctx.checks().startChecks().forEach(this::acquireCheck);
    for (Term t : ctx.setters().terms()) {
      set(t, ctx.setters().get(t));
    }

    Set<Term> newWaypoints = new HashSet<>();
    Set<ItemCheck> newChecks = new HashSet<>();
    ConditionGraph.Builder builder = ConditionGraph.builder(new ConditionGraph.IndexContext(
        new Condition.Context(termValues(), ctx().notchCosts(), ctx().darkness()),
        ctx()::isMutableTerm));
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
    this.graph = builder.build();

    newWaypoints.forEach(this::acquireWaypoint);
    newChecks.forEach(this::maybeAutoAcquire);
    normalize();
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
    return Streams.concat(obtains.stream(), accessibleUnobtained.stream());
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
    potentialTermValues.set(term, Math.max(potentialTermValues.get(term), value));
  }

  private void acquireWaypoint(Term waypoint) {
    if (get(waypoint) == 0) {
      set(waypoint, 1);
    }
  }

  private void purchaseAcquire(ItemCheck check) {
    if (!obtains.contains(check) && accessibleUnobtained.add(check)) {
      check.item().apply(
          new Condition.MutableContext(potentialTermValues, ctx.notchCosts(), ctx.darkness()),
          new HashSet<>());
    }
  }

  public void acquireCheck(ItemCheck check) {
    purchaseAcquire(check);
    if (obtains.add(check)) {
      check.item().apply(new Condition.MutableContext(termValues, ctx.notchCosts(), ctx.darkness()),
          dirtyTerms);
    }
  }

  public boolean test(Condition c) {
    if (dirtyTerms.isEmpty()) {
      return graph.test(c);
    } else {
      return c.test(termValues(), ctx().notchCosts(), ctx().darkness());
    }
  }

  public boolean purchaseTest(Condition c) {
    return c.test(toleranceValues, ctx.notchCosts(), ctx.darkness());
  }

  private void updateGraph(Set<Term> waypointsOut, Set<ItemCheck> checksOut) {
    for (Condition c : graph.update(this, dirtyTerms)) {
      waypointsOut.addAll(ctx().waypoints().getTerms(c));
      ctx().checks().getByCondition(c).forEach(checksOut::add);
    }
    dirtyTerms.clear();
  }

  private void maybeAutoAcquire(ItemCheck check) {
    if (autoAcquire(check)) {
      acquireCheck(check);
    } else {
      purchaseAcquire(check);
    }
  }

  // Recursively acquire waypoints (all) and checks that match the predicate.
  // `newWaypoints` and `newChecks` are iterated on until empty. Checks deemed accessible but
  // not acquired are placed into `unacquiredSink`.
  private void recursivelyUpdateGraph(Set<Term> newWaypoints, Set<ItemCheck> newChecks) {
    while (!newWaypoints.isEmpty() || !newChecks.isEmpty()) {
      if (!newWaypoints.isEmpty()) {
        // Evaluate new waypoints.
        newWaypoints.forEach(this::acquireWaypoint);
        newWaypoints.clear();

        updateGraph(newWaypoints, newChecks);
      }

      if (!newChecks.isEmpty()) {
        for (ItemCheck check : newChecks) {
          if (autoAcquire(check)) {
            acquireCheck(check);
          } else {
            purchaseAcquire(check);
          }
        }
        newChecks.clear();

        updateGraph(newWaypoints, newChecks);
      }
    }
  }

  private static final ImmutableSet<String> TRJR_HELPER_TERMS = ImmutableSet.of("Bluggsac",
      "Crystal_Guardian", "Elder_Baldur", "Grimmkin_Master", "Grimmkin_Nightmare",
      "Grimmkin_Novice", "Gruz_Mother", "Hornet", "Kingsmould", "Respawning_Gruz_Mother",
      "Respawning_Kingsmould", "Respawning_Vengefly_King", "Vengefly_King");

  private boolean autoAcquire(ItemCheck c) {
    return transitionStrategy.autoAcquire(c) || TRJR_HELPER_TERMS.contains(c.item().term().name());
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> newWaypoints = new HashSet<>();
    Set<ItemCheck> newChecks = new HashSet<>();
    updateGraph(newWaypoints, newChecks);
    recursivelyUpdateGraph(newWaypoints, newChecks);
  }

  public TermMap termValues() {
    return termValues;
  }

  public TermMap purchaseTermValues() {
    return toleranceValues;
  }

  public State deepCopy() {
    State copy = new State(ctx);
    copy.transitionStrategy = this.transitionStrategy;
    copy.termValues.clear();
    copy.termValues.add(this.termValues);
    copy.obtains.addAll(this.obtains);
    copy.dirtyTerms.clear();
    copy.dirtyTerms.addAll(this.dirtyTerms);
    copy.accessibleUnobtained.clear();
    copy.accessibleUnobtained.addAll(this.accessibleUnobtained);
    copy.graph = graph.deepCopy();
    copy.potentialTermValues.clear();
    copy.potentialTermValues.add(this.potentialTermValues);
    return copy;
  }

}
