package hollow.knight.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import hollow.knight.logic.ParseException;
import hollow.knight.util.JsonUtil;

// Canonical filters with check box toggles.
public final class ItemCategoryFilters implements SearchEngine.ResultFilter {
  private final ImmutableMap<String, ItemCategoryFilter> filters;
  private final Set<String> selectedFilters;

  private ItemCategoryFilters(Iterable<ItemCategoryFilter> filters) {
    this.filters = Maps.uniqueIndex(filters, ItemCategoryFilter::name);
    this.selectedFilters = new HashSet<>(this.filters.keySet());
  }

  public ImmutableSet<String> allFilters() {
    return filters.keySet();
  }

  public void enableFilter(String filter, boolean enable) {
    Verify.verify(filters.containsKey(filter));

    if (enable) {
      selectedFilters.add(filter);
    } else {
      selectedFilters.remove(filter);
    }
  }

  @Override
  public boolean accept(SearchEngine.Result r) {
    return selectedFilters.stream().anyMatch(f -> filters.get(f).accept(r.itemCheck()));
  }

  public static ItemCategoryFilters load() throws ParseException {
    JsonArray jsonFilters =
        JsonUtil.loadResource(ItemCategoryFilters.class, "category_filters.json").getAsJsonArray();

    List<ItemCategoryFilter> filters = new ArrayList<>();
    for (JsonElement filter : jsonFilters) {
      filters.add(ItemCategoryFilter.parse(filter.getAsJsonObject()));
    }

    // 'Other' is always last.
    OtherCategoryFilter other = new OtherCategoryFilter(filters);
    filters.add(other);

    return new ItemCategoryFilters(filters);
  }

  private static final String OTHER = "Other";

  // The special 'Other' filter covers everything not in the defined filters.
  public static String other() {
    return OTHER;
  }
}
