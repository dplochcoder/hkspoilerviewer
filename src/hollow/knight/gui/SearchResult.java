package hollow.knight.gui;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.swing.JPanel;
import com.google.auto.value.AutoValue;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.Location;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

@AutoValue
public abstract class SearchResult {
  public static interface FilterChangedListener {
    void filterChanged();
  }

  public abstract static class Filter {
    private final Object mutex = new Object();
    private final Set<FilterChangedListener> listeners = new HashSet<>();

    public abstract boolean accept(StateContext ctx, SearchResult result);

    public abstract void addGuiToPanel(JPanel panel);

    protected final void filterChanged() {
      Set<FilterChangedListener> listenersCopy = new HashSet<>();
      synchronized (mutex) {
        listenersCopy.addAll(listeners);
      }

      listenersCopy.forEach(FilterChangedListener::filterChanged);
    }

    public void addListener(FilterChangedListener listener) {
      synchronized (mutex) {
        listeners.add(listener);
      }
    }

    public void removeListener(FilterChangedListener listener) {
      synchronized (mutex) {
        listeners.remove(listener);
      }
    }
  }

  public static enum LogicType {
    IN_LOGIC, COST_ACCESSIBLE, OUT_OF_LOGIC;
  }

  public abstract ItemCheck itemCheck();

  public final Item item() {
    return itemCheck().item();
  }

  public final Costs costs() {
    return itemCheck().costs();
  }

  public final Location location() {
    return itemCheck().location();
  }

  public final boolean vanilla() {
    return itemCheck().vanilla();
  }

  public abstract LogicType logicType();

  public abstract Optional<Integer> notchCost();

  public final String render() {
    StringBuilder sb = new StringBuilder();
    if (logicType() == LogicType.OUT_OF_LOGIC) {
      sb.append('*');
    } else if (logicType() == LogicType.COST_ACCESSIBLE) {
      sb.append('$');
    }
    if (vanilla()) {
      sb.append('#');
    }

    sb.append(item().term().name());
    sb.append(' ');
    sb.append(valueSuffix());
    sb.append("- ");
    sb.append(location().name());
    sb.append(costSuffix());

    return sb.toString();
  }

  private String valueSuffix() {
    if (notchCost().isPresent()) {
      return "(" + notchCost().get() + ") ";
    } else if (item().hasEffectTerm(Term.geo())) {
      return "(" + item().getEffectValue(Term.geo()) + " Geo) ";
    } else if (item().hasEffectTerm(Term.essence())) {
      return "(" + item().getEffectValue(Term.essence()) + " Essence) ";
    } else {
      return "";
    }
  }

  private String costSuffix() {
    return costs().suffixString();
  }

  private static LogicType getLogicType(ItemCheck itemCheck, State state) {
    if (state.test(itemCheck.location().accessCondition())) {
      if (state.test(itemCheck.costs().asCondition())) {
        return LogicType.IN_LOGIC;
      } else if (itemCheck.costs().asCondition().test(state.purchaseTermValues())) {
        return LogicType.COST_ACCESSIBLE;
      }
    }

    return LogicType.OUT_OF_LOGIC;
  }

  public static SearchResult create(ItemCheck itemCheck, State state) {
    Optional<Integer> notchCost = Optional.empty();
    Integer charmId = state.ctx().charmIds().charmId(itemCheck.item().term());
    if (charmId != null) {
      notchCost = Optional.of(state.ctx().notchCosts().notchCost(charmId));
    }

    return new AutoValue_SearchResult(itemCheck, getLogicType(itemCheck, state), notchCost);
  }
}
