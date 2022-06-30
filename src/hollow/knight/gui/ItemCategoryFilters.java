package hollow.knight.gui;

import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

// Canonical filters with check box toggles.
public final class ItemCategoryFilters extends SearchResult.Filter {
  private final JPanel filtersPanel = new JPanel();
  private final ImmutableMap<String, ItemCategoryFilter> filters;
  private final ImmutableMap<String, JCheckBox> filterBoxes;
  private final JCheckBox otherBox;

  private static ImmutableMap<String, ItemCategoryFilter> generateFilters() {
    return ImmutableMap.<String, ItemCategoryFilter>builder()
        .put("Movement",
            ItemCategoryFilter.forTerms("Mothwing_Cloak", "Left_Mothwing_Cloak",
                "Right_Mothwing_Cloak", "Shade_Cloak", "Split_Shade_Cloak", "Mantis_Claw",
                "Left_Mantis_Claw", "Right_Mantis_Claw", "Crystal_Heart", "Left_Crystal_Heart",
                "Right_Crystal_Heart", "Monarch_Wings", "Swim", "Isma's_Tear"))
        .put("Spells",
            ItemCategoryFilter.forTerms("Vengeful_Spirit", "Shade_Soul", "Desolate_Dive",
                "Descending_Dark", "Howling_Wraiths", "Abyss_Shriek"))
        .put("Stags",
            ItemCategoryFilter.forTerms("City_Storerooms_Stag", "Crossroads_Stag", "Dirtmouth_Stag",
                "Switch-Dirtmouth_Stag", "Distant_Village_Stag", "Greenpath_Stag",
                "Hidden_Station_Stag", "King's_Station_Stag", "Queen's_Gardens_Stag",
                "Queen's_Station_Stag", "Resting_Grounds_Stag", "Lever-Resting_Grounds_Stag",
                "Stag_Nest_Stag"))
        .put("True Ending",
            ItemCategoryFilter.forTerms("Lurien", "Monomon", "Herrah", "Dreamer", "Queen_Fragment",
                "King_Fragment", "Void_Heart", "Dream_Nail", "Dream_Gate", "Awoken_Dream_Nail"))
        .put("Upgrades",
            ItemCategoryFilter.forTerms("Vessel_Fragment", "Double_Vessel_Fragment",
                "Full_Soul_Vessel", "Mask_Shard", "Double_Mask_Shard", "Full_Mask", "Charm_Notch",
                "Pale_Ore", "Leftslash", "Rightslash", "Upslash", "Great_Slash", "Cyclone_Slash",
                "Dash_Slash"))
        .put("Consumeables", ItemCategoryFilter.forPools("Cocoon", "Soul"))
        .put("Keys", ItemCategoryFilter.forPools("Key"))
        .put("Levers", ItemCategoryFilter.forPools("Levers"))
        .put("Benches", ItemCategoryFilter.forPools("Benches"))
        .put("Essence", ItemCategoryFilter.forEffect(Term.essence()))
        .put("Charms", ItemCategoryFilter.forPools("Charm"))
        .put("Rancid Eggs", ItemCategoryFilter.forEffect(Term.rancidEggs()))
        .put("Grubs & Mimics", ItemCategoryFilter.forTerms("Grub", "Mimic_Grub"))
        .put("Geo", ItemCategoryFilter.forEffect(Term.geo()))
        .put("Relics", ItemCategoryFilter.forTerms("Relics", "Wanderer's_Journal",
            "Hallownest_Seal", "King's_Idol", "Arcane_Egg"))
        .build();
  }

  public ItemCategoryFilters() {
    this.filters = generateFilters();

    int numRows = (this.filters.size() + 4) / 2;
    this.filtersPanel.setLayout(new GridLayout(numRows, 2));

    this.filterBoxes = createFilterBoxes();
    this.otherBox = createFilterBox("Other");
    filtersPanel.add(createAllButton("All Items", true));
    filtersPanel.add(createAllButton("No Items", false));
    filterBoxes.values().forEach(filtersPanel::add);
    filtersPanel.add(otherBox);
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    boolean noneMatch = true;
    for (String name : filters.keySet()) {
      if (filters.get(name).accept(ctx, result.itemCheck())) {
        noneMatch = false;
        if (filterBoxes.get(name).isSelected()) {
          return true;
        }
      }
    }

    return noneMatch && otherBox.isSelected();
  }

  private JCheckBox createFilterBox(String name) {
    JCheckBox jcb = new JCheckBox(name);
    jcb.setSelected(false);
    jcb.addActionListener(GuiUtil.newActionListener(null, this::filterChanged));
    return jcb;
  }

  private ImmutableMap<String, JCheckBox> createFilterBoxes() {
    ImmutableMap.Builder<String, JCheckBox> builder = ImmutableMap.builder();
    for (String name : filters.keySet()) {
      builder.put(name, createFilterBox(name));
    }

    return builder.build();
  }

  private JButton createAllButton(String txt, boolean enable) {
    JButton button = new JButton(txt);
    button.addActionListener(GuiUtil.newActionListener(null, () -> {
      filterBoxes.values().forEach(b -> b.setSelected(enable));
      otherBox.setSelected(enable);
      filterChanged();
    }));

    return button;
  }

  public void addGuiToPanel(JPanel panel) {
    panel.add(filtersPanel);
  }
}
