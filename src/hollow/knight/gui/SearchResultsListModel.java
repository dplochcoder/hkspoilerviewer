package hollow.knight.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;

public final class SearchResultsListModel implements ListModel<String>, SaveInterface {

  private final Object mutex = new Object();
  private final Set<ListDataListener> listeners = new HashSet<>();

  private final Set<ItemCheck> bookmarksSet = new HashSet<>();
  private final Set<ItemCheck> hiddenResultsSet = new HashSet<>();

  private final List<ItemCheck> bookmarks = new ArrayList<>();
  private final List<SearchResult> results = new ArrayList<>();
  private final List<SearchResult> hiddenResults = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  public int numBookmarks() {
    synchronized (mutex) {
      return bookmarks.size();
    }
  }

  private static final String SEPARATOR = "----------------------------------------";

  public void updateResults(State state, List<SearchResult> newResults) {
    List<ListDataListener> listenersCopy;
    int oldSize, newSize;
    synchronized (mutex) {
      oldSize = this.resultStrings.size();
      listenersCopy = new ArrayList<>(listeners);

      results.clear();
      resultStrings.clear();
      hiddenResults.clear();
      for (SearchResult r : newResults) {
        if (hiddenResultsSet.contains(r.itemCheck())) {
          hiddenResults.add(r);
        } else if (!bookmarksSet.contains(r.itemCheck())) {
          results.add(r);
        }
      }

      bookmarks.forEach(b -> resultStrings.add(SearchResult.create(b, state).render()));
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

  public SearchResult getResult(State state, int index) {
    synchronized (mutex) {
      if (index < bookmarks.size()) {
        return SearchResult.create(bookmarks.get(index), state);
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
      SearchResult result = getResult(state, index);
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
      SearchResult result = getResult(state, index);

      hiddenResultsSet.add(result.itemCheck());
      if (bookmarksSet.remove(result.itemCheck())) {
        bookmarks.remove(result.itemCheck());
      }
    }
  }

  public void unhideResult(State state, int index) {
    synchronized (mutex) {
      SearchResult result = getResult(state, index);
      hiddenResultsSet.remove(result.itemCheck());
    }
  }

  public void unhideResult(State state, ItemCheck check) {
    synchronized (mutex) {
      hiddenResultsSet.remove(check);
    }
  }

  @Override
  public String saveName() {
    return "SearchResultsListModel";
  }

  @Override
  public JsonElement save() {
    JsonObject obj = new JsonObject();

    JsonArray bookmarksArr = new JsonArray();
    bookmarks.forEach(b -> bookmarksArr.add(b.id()));
    obj.add("Bookmarks", bookmarksArr);

    JsonArray hiddenArr = new JsonArray();
    hiddenResultsSet.forEach(h -> hiddenArr.add(h.id()));
    obj.add("Hidden", hiddenArr);

    return obj;
  }

  @Override
  public void open(String version, State initialState, JsonElement json) {
    bookmarks.clear();
    bookmarksSet.clear();
    hiddenResultsSet.clear();

    JsonObject obj = json.getAsJsonObject();
    for (JsonElement bookmark : obj.get("Bookmarks").getAsJsonArray()) {
      ItemCheck check = initialState.items().get(bookmark.getAsInt());
      bookmarks.add(check);
      bookmarksSet.add(check);
    }
    for (JsonElement hidden : obj.get("Hidden").getAsJsonArray()) {
      hiddenResultsSet.add(initialState.items().get(hidden.getAsInt()));
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
