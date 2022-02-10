package hollow.knight.gui;

import java.util.HashMap;
import java.util.Map;
import javax.swing.JList;
import hollow.knight.gui.SearchEngine.Result;
import hollow.knight.logic.RoomLabels;

public final class RoomFilters implements SearchEngine.ResultFilter {

  private final RoomLabels roomLabels;
  private final Map<RoomLabels.Type, JList<String>> selectionLists;
  private RoomLabels.Type activeType;

  public RoomFilters(RoomLabels roomLabels, Map<RoomLabels.Type, JList<String>> selectionLists) {
    this.roomLabels = roomLabels;
    this.selectionLists = new HashMap<>(selectionLists);
    this.activeType = RoomLabels.Type.MAP;
  }

  public void setActiveType(RoomLabels.Type type) {
    activeType = type;
  }

  @Override
  public boolean accept(Result r) {
    String scene = r.location().scene();
    String label = roomLabels.get(scene, activeType);
    JList<String> list = selectionLists.get(activeType);
    return list.getSelectedValuesList().contains(label);
  }

}
