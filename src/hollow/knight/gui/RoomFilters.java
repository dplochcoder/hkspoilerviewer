package hollow.knight.gui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.StateContext;
import hollow.knight.util.GuiUtil;

public final class RoomFilters extends SearchResult.Filter {

  private RoomLabels.Type activeType = RoomLabels.Type.MAP;

  private final JButton allAreas = new JButton("All Areas");
  private final JTabbedPane tabPane = new JTabbedPane();

  private static final ImmutableList<RoomLabels.Type> TYPES =
      Arrays.stream(RoomLabels.Type.values()).collect(ImmutableList.toImmutableList());
  private final ImmutableMap<RoomLabels.Type, JList<String>> selectionLists;

  public RoomFilters(RoomLabels roomLabels) {
    this.selectionLists = createSelectionLists(roomLabels);
    this.activeType = RoomLabels.Type.MAP;

    this.allAreas.addActionListener(GuiUtil.newActionListener(null, this::selectAllAreas));
    this.tabPane.addChangeListener(tabChangedListener());
  }

  private ImmutableMap<RoomLabels.Type, JList<String>> createSelectionLists(RoomLabels roomLabels) {
    ImmutableMap.Builder<RoomLabels.Type, JList<String>> builder = ImmutableMap.builder();
    Map<RoomLabels.Type, JList<String>> lists = new HashMap<>();
    for (RoomLabels.Type type : RoomLabels.Type.values()) {
      DefaultListModel<String> model = new DefaultListModel<>();
      roomLabels.allLabels(type).stream().sorted().forEach(model::addElement);

      JList<String> list = new JList<>(model);
      lists.put(type, list);
      list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      list.setSelectedIndices(IntStream.range(0, model.getSize()).toArray());

      JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      tabPane.addTab(type == RoomLabels.Type.MAP ? "Map Areas" : "Titled Areas", scroll);
      builder.put(type, list);

      list.addListSelectionListener(new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
          RoomFilters.this.filterChanged();
        }
      });
    }

    return builder.build();
  }

  private void selectAllAreas() {
    selectionLists.values()
        .forEach(l -> l.setSelectedIndices(IntStream.range(0, l.getModel().getSize()).toArray()));
    filterChanged();
  }

  private ChangeListener tabChangedListener() {
    return new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        activeType = TYPES.get(tabPane.getSelectedIndex());
        filterChanged();
      }
    };
  }

  public void addGuiToPanel(JPanel panel) {
    panel.add(allAreas);
    panel.add(tabPane);
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    String scene = result.location().scene();
    String label = ctx.roomLabels().get(scene, activeType);
    JList<String> list = selectionLists.get(activeType);
    return list.getSelectedValuesList().contains(label);
  }

}
