package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final StateContext ctx;

  private final MutableTermMap termValues = new MutableTermMap();
  private final Set<ItemCheck> obtains = new HashSet<>();

  public State(StateContext ctx) {
    this.ctx = ctx;

    // TRUE is always set.
    set(Term.true_(), 1);

    ctx.checks().startChecks().forEach(this::acquireCheck);
    for (Term t : ctx.setters().terms()) {
      set(t, ctx.setters().get(t));
    }
  }

  public StateContext ctx() {
    return ctx;
  }

  public Stream<ItemCheck> obtained() {
    return obtains.stream();
  }

  public boolean isAcquired(ItemCheck check) {
    return obtains.contains(check);
  }

  public int get(Term term) {
    return termValues.get(term);
  }

  public void set(Term term, int value) {
    termValues.set(term, value);
  }

  public void acquireCheck(ItemCheck check) {
    if (obtains.add(check)) {
      check.item().apply(termValues);
    }
  }

  public TermMap termValues() {
    return termValues;
  }

  public State deepCopy() {
    State copy = new State(ctx);
    copy.termValues.clear();
    copy.termValues.add(this.termValues);
    copy.obtains.addAll(this.obtains);
    return copy;
  }

}
