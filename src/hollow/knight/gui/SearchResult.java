package hollow.knight.gui;

import java.util.Optional;
import com.google.auto.value.AutoValue;
import hollow.knight.logic.CheckId;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ListenerManager;
import hollow.knight.logic.Location;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;

@AutoValue
public abstract class SearchResult {
  public static interface FilterChangedListener {
    void filterChanged();
  }

  public abstract static class Filter {
    private final ListenerManager<FilterChangedListener> listeners = new ListenerManager<>();

    public abstract boolean accept(StateContext ctx, SearchResult result);

    protected final void filterChanged() {
      listeners.forEach(FilterChangedListener::filterChanged);
    }

    public void addListener(FilterChangedListener listener) {
      listeners.add(listener);
    }

    public void removeListener(FilterChangedListener listener) {
      listeners.remove(listener);
    }
  }

  public static enum LogicType {
    IN_LOGIC, COST_ACCESSIBLE, OUT_OF_LOGIC;
  }

  public abstract ItemCheck itemCheck();

  public final CheckId id() {
    return itemCheck().id();
  }

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

    sb.append(item().displayName());
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
    } else {
      return item().valueSuffix();
    }
  }

  private String costSuffix() {
    return costs().suffixString();
  }

  private static LogicType getLogicType(ItemCheck itemCheck, State state) {
    if (state.test(itemCheck.location().accessCondition())) {
      if (state.test(itemCheck.costs().asCondition())) {
        return LogicType.IN_LOGIC;
      } else if (itemCheck.costs().asCondition().test(state.purchaseTermValues(),
          state.ctx().notchCosts())) {
        return LogicType.COST_ACCESSIBLE;
      }
    } else if (state.purchaseTest(itemCheck.condition())) {
      return LogicType.COST_ACCESSIBLE;
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
