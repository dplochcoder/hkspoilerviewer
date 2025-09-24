package hollow.knight.gui;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.SynchronizedEntityManager;

public final class LogicEditorListModel implements ListModel<String> {

  private final SynchronizedEntityManager<ListDataListener> listeners =
      new SynchronizedEntityManager<>();

  private final List<String> resultNames = new ArrayList<>();
  private final List<String> resultDisplayNames = new ArrayList<>();

  private String diffPrefix(StateContext ctx, String name) {
    if (ctx.logicEdits().isNew(name))
      return "(+) ";
    else if (ctx.logicEdits().isEdited(name))
      return "(*) ";
    else
      return "";
  }

  private String render(StateContext ctx, String name) {
    return diffPrefix(ctx, name) + name;
  }

  public void updateResults(StateContext ctx, List<String> resultNames) {
    int oldSize = this.resultNames.size();

    this.resultNames.clear();
    this.resultDisplayNames.clear();
    resultNames.stream().sorted().forEach(name -> {
      this.resultNames.add(name);
      this.resultDisplayNames.add(render(ctx, name));
    });

    int newSize = resultNames.size();
    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    listeners.forEach(l -> l.contentsChanged(e));
  }

  public String get(int index) {
    if (index < 0 || index >= resultNames.size()) {
      return null;
    }

    return resultNames.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener listener) {
    listeners.add(listener);
  }

  @Override
  public String getElementAt(int index) {
    return resultDisplayNames.get(index);
  }

  @Override
  public int getSize() {
    return resultNames.size();
  }

  @Override
  public void removeListDataListener(ListDataListener listener) {
    listeners.remove(listener);
  }
}
