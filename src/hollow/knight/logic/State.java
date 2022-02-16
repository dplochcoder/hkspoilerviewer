package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final StateContext ctx;

  private final MutableTermMap termValues = new MutableTermMap();
  private final MutableTermMap costValuesWithTolerances = new MutableTermMap();
  private final Set<ItemCheck> acquiredItemChecks = new HashSet<>();

  private final Set<Term> dirtyTerms = new HashSet<>();

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

    itemCheck.item().apply(this);
    acquiredItemChecks.add(itemCheck);
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> inLogicTerms = new HashSet<>();
    dirtyTerms.forEach(t -> inLogicTerms.addAll(ctx.waypoints().influences(t)));
    dirtyTerms.clear();

    inLogicTerms.removeIf(t -> termValues.get(t) != 0 || !ctx.waypoints().get(t).test(this));

    // Loop: Evaluate all waypoints until there are no more advancements to be made.
    while (!inLogicTerms.isEmpty()) {
      Set<Term> newTerms = new HashSet<>();
      for (Term t : inLogicTerms) {
        set(t, 1);
        newTerms.addAll(ctx.waypoints().influences(t));
      }

      newTerms.removeIf(t -> termValues.get(t) != 0 || !ctx.waypoints().get(t).test(this));
      inLogicTerms.clear();
      inLogicTerms.addAll(newTerms);
    }

    // Acquire all in-logic items that yield an effect on a cost term, until no more such items can
    // be acquired.
    State stateCopy = this.deepCopy();
    Set<ItemCheck> canAcquire = stateCopy.unobtainedItemChecks().stream()
        .filter(StateContext::canAffectCosts).filter(c -> c.location().canAccess(this))
        .collect(Collectors.toCollection(HashSet::new));
    while (true) {
      int acquired = 0;
      for (ItemCheck c : new HashSet<>(canAcquire)) {
        if (c.costs().canBePaid(this.get(Term.canReplenishGeo()) > 0, stateCopy.termValues)) {
          stateCopy.acquireItemCheck(c);
          canAcquire.remove(c);
          acquired++;
        }
      }

      if (acquired == 0) {
        break;
      }
    }

    // For each cost term, determine the total we have logical access to, without acquiring anything
    // except more cost effects.
    for (Term t : StateContext.costTerms()) {
      costValuesWithTolerances.set(t, ctx.tolerances().get(t) + stateCopy.get(t));
    }
  }

  public TermMap termValues() {
    return this.termValues;
  }

  public TermMap accessibleTermValues() {
    return this.costValuesWithTolerances;
  }

  public State deepCopy() {
    State copy = new State(ctx);
    copy.termValues.clear();
    for (Term t : this.termValues.terms()) {
      copy.termValues.set(t, get(t));
    }
    copy.acquiredItemChecks.addAll(this.acquiredItemChecks);
    copy.costValuesWithTolerances.clear();
    copy.costValuesWithTolerances.add(this.costValuesWithTolerances);
    copy.dirtyTerms.clear();
    copy.dirtyTerms.addAll(this.dirtyTerms);
    return copy;
  }

}
