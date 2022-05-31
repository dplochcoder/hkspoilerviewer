package hollow.knight.gui;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.StateContext;

// Canonical filters with check box toggles.
public final class ItemCategoryFilters extends SearchResult.Filter {
  private final JPanel filtersPanel = new JPanel();
  private final ImmutableMap<String, ItemCategoryFilter> filters;
  private final ImmutableMap<String, JCheckBox> filterBoxes;

  private ItemCategoryFilters(Iterable<ItemCategoryFilter> filters) {
    this.filters = Maps.uniqueIndex(filters, ItemCategoryFilter::name);

    int numRows = (this.filters.size() + 3) / 2;
    this.filtersPanel.setLayout(new GridLayout(numRows, 2));

    this.filterBoxes = createFilterBoxes();
    filtersPanel.add(createAllButton("All Items", true));
    filtersPanel.add(createAllButton("No Items", false));
    filterBoxes.values().forEach(filtersPanel::add);
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    return filterBoxes.keySet().stream().filter(n -> filterBoxes.get(n).isSelected())
        .map(filters::get).anyMatch(icf -> icf.accept(ctx, result.itemCheck()));
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

  private ImmutableMap<String, JCheckBox> createFilterBoxes() {
    ImmutableMap.Builder<String, JCheckBox> builder = ImmutableMap.builder();
    for (String name : filters.keySet()) {
      JCheckBox jcb = new JCheckBox(name);
      jcb.setSelected(false);
      jcb.addActionListener(GuiUtil.newActionListener(null, this::filterChanged));

      builder.put(name, jcb);
    }

    return builder.build();
  }

  private JButton createAllButton(String txt, boolean enable) {
    JButton button = new JButton(txt);
    button.addActionListener(GuiUtil.newActionListener(null, () -> {
      filterBoxes.values().forEach(b -> b.setSelected(enable));
      filterChanged();
    }));

    return button;
  }

  public void addGuiToPanel(JPanel panel) {
    panel.add(filtersPanel);
  }
}
