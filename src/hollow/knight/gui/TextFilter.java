package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import hollow.knight.logic.RoomLabels;

public final class TextFilter implements SearchResult.Filter {

  public static enum Mode {
    ITEM, LOCATION, BOTH;
  }

  private final RoomLabels roomLabels;
  private String text = "";
  private Mode mode = Mode.BOTH;

  public TextFilter(RoomLabels roomLabels) {
    this.roomLabels = roomLabels;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setMode(Mode mode) {
    this.mode = mode;
  }

  @Override
  public boolean accept(SearchResult result) {
    String t = text.trim().toLowerCase();
    if (t.isEmpty()) {
      return true;
    }

    return Arrays.stream(t.split("\\s")).allMatch(term -> matchesTerm(result, term));
  }

  private boolean matchesTerm(SearchResult result, String term) {
    if (mode != Mode.LOCATION) {
      String item = result.item().term().name().toLowerCase();
      if (item.contains(term)) {
        return true;
      }
    }

    if (mode != Mode.ITEM) {
      List<String> locations = new ArrayList<>();
      locations.add(result.location().name());
      locations.add(result.location().scene());
      locations.add(roomLabels.get(result.location().scene(), RoomLabels.Type.MAP));
      locations.add(roomLabels.get(result.location().scene(), RoomLabels.Type.TITLE));
      if (locations.stream().anyMatch(l -> l.toLowerCase().contains(term))) {
        return true;
      }
    }

    return false;
  }

}
