package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;

public final class SearchEngine {
  private final TransitionData transitionData;
  private final RoomLabels roomLabels;
  private final List<SearchResult.Filter> resultFilters;

  public SearchEngine(TransitionData transitionData, RoomLabels roomLabels,
      List<SearchResult.Filter> resultFilters) {
    this.transitionData = transitionData;
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
        .compare(r1.item().displayName(transitionData).toLowerCase(),
            r2.item().displayName(transitionData).toLowerCase())
        .compare(r1.location().name(), r2.location().name()).result();
  }

  public ImmutableList<SearchResult> getSearchResults(State state) {
    // Step 1: Collect all results.
    List<SearchResult> results = new ArrayList<>();
    state.ctx().checks().allChecks().forEach(c -> results.add(SearchResult.create(c, state)));

    // Step 2: Apply filters.
    results.removeIf(r -> !accept(state.ctx(), r));

    // Step 3: Sort results.
    Collections.sort(results, this::sortResults);

    return ImmutableList.copyOf(results);
  }
}
