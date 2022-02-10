package hollow.knight.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.State;

public final class SearchResultsListModel implements ListModel<String> {

  private final Object mutex = new Object();
  private final Set<ListDataListener> listeners = new HashSet<>();

  private final Set<ItemCheck> bookmarksSet = new HashSet<>();
  private final Set<ItemCheck> hiddenResultsSet = new HashSet<>();

  private final List<ItemCheck> bookmarks = new ArrayList<>();
  private final List<SearchEngine.Result> results = new ArrayList<>();
  private final List<SearchEngine.Result> hiddenResults = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  public int numBookmarks() {
    synchronized (mutex) {
      return bookmarks.size();
    }
  }

  private static final String SEPARATOR = "----------------------------------------";

  public void updateResults(State state, List<SearchEngine.Result> newResults) {
    List<ListDataListener> listenersCopy;
    int oldSize, newSize;
    synchronized (mutex) {
      oldSize = this.resultStrings.size();
      listenersCopy = new ArrayList<>(listeners);

      results.clear();
      resultStrings.clear();
      hiddenResults.clear();
      for (SearchEngine.Result r : newResults) {
        if (hiddenResultsSet.contains(r.itemCheck())) {
          hiddenResults.add(r);
        } else if (!bookmarksSet.contains(r.itemCheck())) {
          results.add(r);
        }
      }

      bookmarks.forEach(b -> resultStrings.add(SearchEngine.Result.create(b, state).render()));
      resultStrings.add(SEPARATOR);
      results.forEach(r -> resultStrings.add(r.render()));
      resultStrings.add(SEPARATOR);
      hiddenResults.forEach(r -> resultStrings.add(r.render()));

      newSize = resultStrings.size();
    }

    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    for (ListDataListener listener : listenersCopy) {
      listener.contentsChanged(e);
    }
  }

  public void removeBookmark(State state, ItemCheck itemCheck) {
    synchronized (mutex) {
      if (bookmarksSet.remove(itemCheck)) {
        bookmarks.remove(itemCheck);
      }
    }
  }

  public SearchEngine.Result getResult(State state, int index) {
    synchronized (mutex) {
      if (index < bookmarks.size()) {
        return SearchEngine.Result.create(bookmarks.get(index), state);
      }

      index -= bookmarks.size() + 1;
      if (index < 0) {
        return null;
      }

      if (index < results.size()) {
        return results.get(index);
      }

      index -= results.size() + 1;
      if (index < 0) {
        return null;
      }

      return hiddenResults.get(index);
    }
  }

  public void addBookmark(State state, int index) {
    synchronized (mutex) {
      SearchEngine.Result result = getResult(state, index);
      if (result == null) {
        return;
      }

      if (bookmarksSet.add(result.itemCheck())) {
        bookmarks.add(result.itemCheck());
        hiddenResultsSet.remove(result.itemCheck());
      }
    }
  }

  public void moveBookmark(State state, int index, boolean up) {
    synchronized (mutex) {
      if (index >= bookmarks.size()) {
        return;
      }

      int otherIndex = index + (up ? -1 : 1);
      if (otherIndex < 0 || otherIndex >= bookmarks.size()) {
        return;
      }

      ItemCheck a = bookmarks.get(index);
      ItemCheck b = bookmarks.get(otherIndex);
      bookmarks.set(index, b);
      bookmarks.set(otherIndex, a);
    }
  }

  public void deleteBookmark(State state, int index) {
    synchronized (mutex) {
      if (index >= bookmarks.size()) {
        return;
      }

      bookmarksSet.remove(bookmarks.remove(index));
    }
  }

  public void hideResult(State state, int index) {
    synchronized (mutex) {
      SearchEngine.Result result = getResult(state, index);

      hiddenResultsSet.add(result.itemCheck());
      if (bookmarksSet.remove(result.itemCheck())) {
        bookmarks.remove(result.itemCheck());
      }
    }
  }

  public void unhideResult(State state, int index) {
    synchronized (mutex) {
      SearchEngine.Result result = getResult(state, index);
      hiddenResultsSet.remove(result.itemCheck());
    }
  }

  public void unhideResult(State state, ItemCheck check) {
    synchronized (mutex) {
      hiddenResultsSet.remove(check);
    }
  }

  @Override
  public void addListDataListener(ListDataListener listener) {
    synchronized (mutex) {
      listeners.add(listener);
    }
  }

  @Override
  public String getElementAt(int index) {
    synchronized (mutex) {
      return resultStrings.get(index);
    }
  }

  @Override
  public int getSize() {
    synchronized (mutex) {
      return resultStrings.size();
    }
  }

  @Override
  public void removeListDataListener(ListDataListener listener) {
    synchronized (mutex) {
      listeners.remove(listener);
    }
  }

}
