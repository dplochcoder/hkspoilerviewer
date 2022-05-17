package hollow.knight.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;

public final class SearchEngine {
  private final RoomLabels roomLabels;
  private final List<SearchResult.Filter> resultFilters;

  public SearchEngine(RoomLabels roomLabels, List<SearchResult.Filter> resultFilters) {
    this.roomLabels = roomLabels;
    this.resultFilters = resultFilters;
  }

  public boolean accept(StateContext ctx, SearchResult result) {
    return resultFilters.stream().allMatch(f -> f.accept(ctx, result));
  }

  private int sortResults(SearchResult r1, SearchResult r2) {
    return ComparisonChain.start()
        .compare(roomLabels.get(r1.location().scene(), RoomLabels.Type.MAP),
            roomLabels.get(r2.location().scene(), RoomLabels.Type.MAP))
        .compare(r1.item().term().name().toLowerCase(), r2.item().term().name().toLowerCase())
        .compare(r1.location().name(), r2.location().name()).result();
  }

  public ImmutableList<SearchResult> getSearchResults(State state) {
    // Step 1: Collect all results.
    List<SearchResult> results = new ArrayList<>();
    for (ItemCheck check : state.unobtainedItemChecks()) {
      SearchResult result = SearchResult.create(check, state);
      results.add(result);
    }

    // Step 2: Apply filters.
    results = results.stream().filter(r -> accept(state.ctx(), r)).collect(Collectors.toList());

    // Step 3: Sort results.
    results = results.stream().sorted(this::sortResults).collect(Collectors.toList());

    return ImmutableList.copyOf(results);
  }
}
