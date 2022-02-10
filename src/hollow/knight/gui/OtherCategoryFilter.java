package hollow.knight.gui;

import java.util.List;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.ItemCheck;

public final class OtherCategoryFilter extends ItemCategoryFilter {
  private final ImmutableList<ItemCategoryFilter> filters;

  public OtherCategoryFilter(List<ItemCategoryFilter> filters) {
    super(ItemCategoryFilters.other());
    this.filters = ImmutableList.copyOf(filters);
  }

  @Override
  public boolean accept(ItemCheck itemCheck) {
    return filters.stream().noneMatch(f -> f.accept(itemCheck));
  }
}
