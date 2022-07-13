package hollow.knight.gui;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.SynchronizedEntityManager;

public final class SelectedSceneSearchResultsListModel implements ListModel<String> {

  private final SynchronizedEntityManager<ListDataListener> listeners =
      new SynchronizedEntityManager<>();

  private final List<SearchResult> results = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  private final TransitionData transitionData;
  private final Predicate<ItemCheck> isRouted;

  public SelectedSceneSearchResultsListModel(TransitionData transitionData,
      Predicate<ItemCheck> isRouted) {
    this.transitionData = transitionData;
    this.isRouted = isRouted;
  }

  public SearchResult getResult(int index) {
    return results.get(index);
  }

  private String render(SearchResult result) {
    return (isRouted.test(result.itemCheck()) ? "(R) " : "") + result.render(transitionData);
  }

  public void updateResults(List<SearchResult> newResults) {
    int oldSize = this.resultStrings.size();
    results.clear();
    resultStrings.clear();
    results.addAll(newResults);
    newResults.forEach(r -> resultStrings.add(render(r)));
    int newSize = resultStrings.size();

    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    listeners.forEach(l -> l.contentsChanged(e));
  }

  public void adjustComponentStyle(Component c, int index) {
    SearchResult s = results.get(index);

    if (s.itemCheck().isTransition()) {
      Font f = c.getFont();
      c.setFont(new Font(f.getFontName(), Font.ITALIC, f.getSize()));
    }
  }

  @Override
  public void addListDataListener(ListDataListener listener) {
    listeners.add(listener);
  }

  @Override
  public String getElementAt(int index) {
    return resultStrings.get(index);
  }

  @Override
  public int getSize() {
    return resultStrings.size();
  }

  @Override
  public void removeListDataListener(ListDataListener listener) {
    listeners.remove(listener);
  }

}
