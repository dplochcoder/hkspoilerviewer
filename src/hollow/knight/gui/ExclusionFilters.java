package hollow.knight.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.RoomLabels;

public final class ExclusionFilters implements SearchResult.Filter {
  @AutoValue
  abstract static class Filter {
    public abstract String name();

    public abstract SearchResult.Filter filter();

    public static Filter create(String name, SearchResult.Filter filter) {
      return new AutoValue_ExclusionFilters_Filter(name, filter);
    }
  }

  private final ImmutableList<Filter> filters;
  private final List<Boolean> enabled;

  private ImmutableList<Filter> createFilters(RoomLabels roomLabels) {
    return ImmutableList.of(Filter.create("Vanilla (#)", r -> r.itemCheck().vanilla()),
        Filter.create("Out of Logic (*)",
            r -> r.logicType() == SearchResult.LogicType.OUT_OF_LOGIC),
        Filter.create("Purchase Logic ($)",
            r -> r.logicType() == SearchResult.LogicType.COST_ACCESSIBLE));
  }

  public ExclusionFilters(RoomLabels roomLabels) {
    this.filters = createFilters(roomLabels);
    this.enabled =
        this.filters.stream().map(f -> false).collect(Collectors.toCollection(ArrayList::new));
  }

  public int numFilters() {
    return filters.size();
  }

  public Stream<String> filterNames() {
    return filters.stream().map(Filter::name);
  }

  public void enableFilter(int index, boolean enabled) {
    this.enabled.set(index, enabled);
  }

  @Override
  public boolean accept(SearchResult result) {
    for (int i = 0; i < filters.size(); i++) {
      if (enabled.get(i) && filters.get(i).filter().accept(result)) {
        return false;
      }
    }

    return true;
  }
}
