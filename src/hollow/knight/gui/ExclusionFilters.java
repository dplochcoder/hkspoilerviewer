package hollow.knight.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Predicate;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.StateContext;

public final class ExclusionFilters extends SearchResult.Filter {
  @AutoValue
  abstract static class ExclusionFilter {
    public abstract String name();

    public abstract Predicate<SearchResult> filter();

    public abstract JCheckBox checkBox();

    public static ExclusionFilter create(ExclusionFilters filters, String name,
        Predicate<SearchResult> filter, JCheckBox checkBox) {
      checkBox.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          filters.filterChanged();
        }
      });

      return new AutoValue_ExclusionFilters_ExclusionFilter(name, filter, checkBox);
    }
  }

  private final ImmutableList<ExclusionFilter> filters;
  private final JPanel filtersPanel;

  private JCheckBox jcb(String txt, boolean isSelected) {
    JCheckBox out = new JCheckBox(txt);
    out.setSelected(isSelected);
    return out;
  }

  private static final ImmutableSet<String> SHOPS =
      ImmutableSet.of("Egg_shop", "Grubfather", "Iselda", "Leg_Eater", "Salubra", "Seer", "Sly",
          "Sly_(Key)", "Unbreakable_Greed", "Unbreakable_Heart", "Unbreakable_Strength");

  private ImmutableList<ExclusionFilter> createFilters(RoomLabels roomLabels) {
    return ImmutableList.of(
        ExclusionFilter.create(this, "NON_VANILLA", r -> !r.itemCheck().vanilla(),
            jcb("Randomized", true)),
        ExclusionFilter.create(this, "VANILLA", r -> r.itemCheck().vanilla(),
            jcb("Vanilla (#)", false)),
        ExclusionFilter.create(this, "IN_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.IN_LOGIC, jcb("In Logic", true)),
        ExclusionFilter.create(this, "OUT_OF_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.OUT_OF_LOGIC,
            jcb("Out of Logic (*)", false)),
        ExclusionFilter.create(this, "SHOPS", r -> SHOPS.contains(r.location().name()),
            jcb("Shops", true)),
        ExclusionFilter.create(this, "PURCHASE_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.COST_ACCESSIBLE,
            jcb("Purchase Logic ($)", true)));
  }

  public ExclusionFilters(RoomLabels roomLabels) {
    this.filters = createFilters(roomLabels);
    this.filtersPanel = new JPanel();

    int numRows = (this.filters.size() + 1) / 2;
    this.filtersPanel.setLayout(new GridLayout(numRows, 2));
    this.filters.forEach(f -> filtersPanel.add(f.checkBox()));
  }

  @Override
  public void addGuiToPanel(JPanel panel) {
    panel.add(filtersPanel);
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    return filters.stream().allMatch(f -> f.checkBox().isSelected() || !f.filter().test(result));
  }
}
