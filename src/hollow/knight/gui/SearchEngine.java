package hollow.knight.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.Location;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.State;
import hollow.knight.logic.Term;

public final class SearchEngine {
  @AutoValue
  public abstract static class Result {
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

    public abstract boolean inLogic();

    public abstract Optional<Integer> notchCost();

    public final String render() {
      StringBuilder sb = new StringBuilder();
      if (!inLogic()) {
        sb.append('*');
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

    public static Result create(ItemCheck itemCheck, State state) {
      boolean inLogic = itemCheck.location().canAccess(state) && itemCheck.costs().canBePaid(state);
      Optional<Integer> notchCost = Optional.empty();
      if (itemCheck.item().isCharm(state.items())) {
        notchCost = Optional.of(itemCheck.item().notchCost(state.items()));
      }
      return new AutoValue_SearchEngine_Result(itemCheck, inLogic, notchCost);
    }
  }

  public static interface ResultFilter {
    boolean accept(Result r);
  }

  private final RoomLabels roomLabels;
  private final List<ResultFilter> resultFilters;

  public SearchEngine(RoomLabels roomLabels, List<ResultFilter> resultFilters) {
    this.roomLabels = roomLabels;
    this.resultFilters = resultFilters;
  }

  private boolean accept(Result r) {
    return resultFilters.stream().allMatch(f -> f.accept(r));
  }

  private int sortResults(Result r1, Result r2) {
    return ComparisonChain.start()
        .compare(roomLabels.get(r1.location().scene(), RoomLabels.Type.MAP),
            roomLabels.get(r2.location().scene(), RoomLabels.Type.MAP))
        .compare(r1.item().term().name().toLowerCase(), r2.item().term().name().toLowerCase())
        .compare(r1.location().name(), r2.location().name()).result();
  }

  public ImmutableList<Result> getSearchResults(State state) {
    // Step 1: Collect all results.
    List<Result> results = new ArrayList<>();
    for (ItemCheck check : state.unobtainedItemChecks()) {
      Result result = Result.create(check, state);
      results.add(result);
    }

    // Step 2: Apply filters.
    results = results.stream().filter(this::accept).collect(Collectors.toList());

    // Step 3: Sort results.
    results = results.stream().sorted(this::sortResults).collect(Collectors.toList());

    return ImmutableList.copyOf(results);
  }
}
