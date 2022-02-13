package hollow.knight.gui;

import java.util.Optional;
import com.google.auto.value.AutoValue;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.Location;
import hollow.knight.logic.State;
import hollow.knight.logic.Term;

@AutoValue
public abstract class SearchResult {
  public static interface Filter {
    boolean accept(SearchResult result);
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
    if (itemCheck.location().canAccess(state)) {
      if (itemCheck.costs().canBePaid(state.get(Term.canReplenishGeo()) > 0, state.termValues())) {
        return LogicType.IN_LOGIC;
      } else if (itemCheck.costs().canBePaid(state.get(Term.canReplenishGeo()) > 0,
          state.accessibleTermValues())) {
        return LogicType.COST_ACCESSIBLE;
      }
    }

    return LogicType.OUT_OF_LOGIC;
  }

  public static SearchResult create(ItemCheck itemCheck, State state) {
    Optional<Integer> notchCost = Optional.empty();
    if (itemCheck.item().isCharm(state.items())) {
      notchCost = Optional.of(itemCheck.item().notchCost(state.items()));
    }

    return new AutoValue_SearchResult(itemCheck, getLogicType(itemCheck, state), notchCost);
  }
}