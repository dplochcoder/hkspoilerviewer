package hollow.knight.gui;

import java.util.stream.Stream;
import javax.swing.JCheckBox;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.RoomLabels;

public final class ExclusionFilters implements SearchResult.Filter {
  @AutoValue
  abstract static class Filter {
    public abstract String name();

    public abstract SearchResult.Filter filter();

    public abstract JCheckBox checkBox();

    public static Filter create(String name, SearchResult.Filter filter, JCheckBox checkBox) {
      return new AutoValue_ExclusionFilters_Filter(name, filter, checkBox);
    }
  }

  private final ImmutableList<Filter> filters;

  private static JCheckBox jcb(String txt, boolean isSelected) {
    JCheckBox out = new JCheckBox(txt);
    out.setSelected(isSelected);
    return out;
  }

  private ImmutableList<Filter> createFilters(RoomLabels roomLabels) {
    return ImmutableList.of(
        Filter.create("VANILLA", r -> r.itemCheck().vanilla(), jcb("Vanilla (#)", true)),
        Filter.create("OUT_OF_LOGIC", r -> r.logicType() == SearchResult.LogicType.OUT_OF_LOGIC,
            jcb("Out of Logic (*)", true)),
        Filter.create("PURCHASE_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.COST_ACCESSIBLE,
            jcb("Purchase Logic ($)", false)));
  }

  public ExclusionFilters(RoomLabels roomLabels) {
    this.filters = createFilters(roomLabels);
  }

  public Stream<JCheckBox> gui() {
    return this.filters.stream().map(Filter::checkBox);
  }

  @Override
  public boolean accept(SearchResult result) {
    return filters.stream().allMatch(f -> !f.checkBox().isSelected() || !f.filter().accept(result));
  }
}
