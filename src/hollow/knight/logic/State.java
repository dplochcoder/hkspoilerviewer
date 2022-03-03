package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final StateContext ctx;

  private final MutableTermMap termValues = new MutableTermMap();
  private final Set<ItemCheck> acquiredItemChecks = new HashSet<>();

  private final Set<Term> dirtyTerms = new HashSet<>();

  // A parallel State which acquires every accessible item, for computing purchase logic.
  private State potentialState = null;
  private TermMap toleranceValues = null;
  private MutableTermMap normalizedCosts = new MutableTermMap();
  private final Set<ItemCheck> purchasableItemChecks = new HashSet<>();


  State(StateContext ctx) {
    this.ctx = ctx;

    // TRUE is always set.
    setClean(Term.true_(), 1);
  }

  public StateContext ctx() {
    return ctx;
  }

  public ImmutableSet<ItemCheck> unobtainedItemChecks() {
    Set<ItemCheck> unobtained = new HashSet<>(ctx.items().allItemChecks());
    unobtained.removeAll(acquiredItemChecks);
    return ImmutableSet.copyOf(unobtained);
  }

  public int get(Term term) {
    return termValues.get(term);
  }

  public void set(Term term, int value) {
    dirtyTerms.add(term);
    setClean(term, value);
  }

  private void setClean(Term term, int value) {
    termValues.set(term, value);
  }

  public void dumpTerms() {
    for (Term t : termValues.terms().stream().sorted((t1, t2) -> t1.name().compareTo(t2.name()))
        .collect(ImmutableList.toImmutableList())) {
      System.err.println(t + ": " + termValues.get(t));
    }
  }

  public void acquireItemCheck(ItemCheck itemCheck) {
    if (acquiredItemChecks.contains(itemCheck)) {
      return;
    }

    if (potentialState != null) {
      potentialState.acquireItemCheck(itemCheck);
      purchasableItemChecks.remove(itemCheck);
    }

    itemCheck.item().apply(this);
    acquiredItemChecks.add(itemCheck);
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> inLogicTerms = new HashSet<>();
    Set<ItemCheck> newChecks = new HashSet<>();
    dirtyTerms.forEach(t -> inLogicTerms.addAll(ctx.waypoints().influences(t)));
    dirtyTerms.forEach(t -> newChecks.addAll(ctx.items().influences(t)));
    dirtyTerms.clear();

    inLogicTerms.removeIf(t -> termValues.get(t) != 0 || !ctx.waypoints().get(t).test(this));

    // Loop: Evaluate all waypoints until there are no more advancements to be made.
    while (!inLogicTerms.isEmpty()) {
      Set<Term> newTerms = new HashSet<>();
      for (Term t : inLogicTerms) {
        set(t, 1);
        newTerms.addAll(ctx.waypoints().influences(t));
        newChecks.addAll(ctx.items().influences(t));
      }

      newTerms.removeIf(t -> termValues.get(t) != 0 || !ctx.waypoints().get(t).test(this));
      inLogicTerms.clear();
      inLogicTerms.addAll(newTerms);
    }

    if (potentialState == null) {
      potentialState = this.deepCopy();
      toleranceValues =
          new SumTermMap(ImmutableList.of(potentialState.termValues, ctx.tolerances()));
    }

    boolean canReplenishGeo = get(Term.canReplenishGeo()) > 0;
    newChecks.removeIf(c -> potentialState.acquiredItemChecks.contains(c)
        || purchasableItemChecks.contains(c) || !c.location().canAccess(this));
    while (!newChecks.isEmpty()) {
      for (ItemCheck check : newChecks) {
        if (check.costs().canBePaid(canReplenishGeo, potentialState.termValues)) {
          potentialState.acquireItemCheck(check);
        } else {
          purchasableItemChecks.add(check);
        }
      }
      newChecks.clear();

      for (Term cost : Term.costTerms()) {
        int prev = normalizedCosts.get(cost);
        int next = potentialState.get(cost);

        if (next > prev) {
          ctx.items().costChecks(cost, prev, next).forEach(c -> {
            newChecks.add(c);
            purchasableItemChecks.remove(c);
          });
          normalizedCosts.set(cost, next);
        }
      }
    }
  }

  public TermMap termValues() {
    return termValues;
  }

  public TermMap accessibleTermValues() {
    return toleranceValues;
  }

  public State deepCopy() {
    State copy = new State(ctx);
    copy.termValues.clear();
    this.termValues.terms().forEach(t -> copy.termValues.set(t, get(t)));
    copy.acquiredItemChecks.addAll(this.acquiredItemChecks);
    copy.dirtyTerms.clear();
    copy.dirtyTerms.addAll(this.dirtyTerms);
    if (potentialState != null) {
      copy.potentialState = this.potentialState.deepCopy();
      copy.toleranceValues =
          new SumTermMap(ImmutableList.of(copy.potentialState.termValues, ctx.tolerances()));
      copy.purchasableItemChecks.addAll(this.purchasableItemChecks);
    }
    return copy;
  }

}
