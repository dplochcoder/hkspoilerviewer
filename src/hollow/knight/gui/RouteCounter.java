package hollow.knight.gui;

import java.util.function.Function;
import javax.swing.JLabel;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.State;
import hollow.knight.logic.Term;
import hollow.knight.logic.TermMap;

public final class RouteCounter {
  public static final Function<State, Integer> termFunction(Term term) {
    return state -> state.get(term);
  }

  public static final Function<State, Integer> purchaseTermFunction(Term term) {
    return state -> state.purchaseTermValues().get(term);
  }

  private static final ImmutableMap<Term, Integer> RELIC_VALUES =
      ImmutableMap.of(Term.create("Wanderer's_Journal"), 200, Term.create("Hallownest_Seal"), 450,
          Term.create("King's_Idol"), 800, Term.create("Arcane_Egg"), 1200);

  public static Integer relicGeoCounter(State state) {
    return state.obtained().mapToInt(c -> RELIC_VALUES.getOrDefault(c.item().term(), 0)).sum();
  }

  public static Integer purchaseRelicGeoCounter(State state) {
    return state.accessible().mapToInt(c -> RELIC_VALUES.getOrDefault(c.item().term(), 0)).sum();
  }

  public static Integer spentGeoCounter(State state) {
    // TODO: Fix for area blitz
    return state.obtained().mapToInt(c -> c.costs().getGeoCost()).sum();
  }

  public static Integer spendableGeoCounter(State state) {
    // TODO: Fix for area blitz
    return state.accessible().mapToInt(c -> c.costs().getGeoCost()).sum();
  }

  public static final Function<TermMap, Integer> termMapFunction(Term term) {
    return m -> m.get(term);
  }

  private final String text;
  private final Function<State, Integer> stateFunction;
  private final JLabel label;

  public RouteCounter(String text, Function<State, Integer> stateFunction) {
    this.text = text;
    this.stateFunction = stateFunction;
    this.label = new JLabel("");
  }

  public JLabel getLabel() {
    return label;
  }

  public void update(State state) {
    label.setText(text + ": " + stateFunction.apply(state));
  }
}
