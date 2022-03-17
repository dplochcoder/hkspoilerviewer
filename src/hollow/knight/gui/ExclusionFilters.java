package hollow.knight.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Predicate;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import hollow.knight.logic.RoomLabels;

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

  private JCheckBox jcb(String txt, boolean isSelected) {
    JCheckBox out = new JCheckBox(txt);
    out.setSelected(isSelected);
    return out;
  }

  private ImmutableList<ExclusionFilter> createFilters(RoomLabels roomLabels) {
    return ImmutableList.of(
        ExclusionFilter.create(this, "VANILLA", r -> r.itemCheck().vanilla(),
            jcb("Vanilla (#)", false)),
        ExclusionFilter.create(this, "OUT_OF_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.OUT_OF_LOGIC,
            jcb("Out of Logic (*)", false)),
        ExclusionFilter.create(this, "PURCHASE_LOGIC",
            r -> r.logicType() == SearchResult.LogicType.COST_ACCESSIBLE,
            jcb("Purchase Logic ($)", true)));
  }

  public ExclusionFilters(RoomLabels roomLabels) {
    this.filters = createFilters(roomLabels);
  }

  @Override
  public void addGuiToPanel(JPanel panel) {
    filters.forEach(f -> panel.add(f.checkBox()));
  }

  @Override
  public boolean accept(SearchResult result) {
    return filters.stream().allMatch(f -> f.checkBox().isSelected() || !f.filter().test(result));
  }
}
