package hollow.knight.gui;

import java.awt.Color;
import java.awt.Component;
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
import hollow.knight.logic.CheckId;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;

public final class SearchResultsListModel
    implements ListModel<String>, ItemChecks.Listener, SaveInterface {

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

  public void removeBookmark(ItemCheck check) {
    synchronized (mutex) {
      if (bookmarksSet.remove(check)) {
        bookmarks.remove(check);
      }
    }
  }

  public ItemCheck getCheck(int index) {
    synchronized (mutex) {
      if (index < bookmarks.size()) {
        return bookmarks.get(index);
      }

      index -= bookmarks.size() + 1;
      if (index < 0) {
        return null;
      }

      if (index < results.size()) {
        return results.get(index).itemCheck();
      }

      index -= results.size() + 1;
      if (index < 0) {
        return null;
      }

      return hiddenResults.get(index).itemCheck();
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

  private void brighten(Component c) {
    c.setForeground(Color.GRAY);
  }

  public void adjustForegroundColor(Component c, State state, SearchEngine engine, int index) {
    SearchResult s = getResult(state, index);
    if (s == null) {
      return;
    }

    if (bookmarksSet.contains(s.itemCheck())) {
      if (!engine.accept(state.ctx(), s)) {
        brighten(c);
      }
    } else if (hiddenResultsSet.contains(s.itemCheck())) {
      brighten(c);
    }
  }

  public void addBookmark(int index) {
    synchronized (mutex) {
      ItemCheck check = getCheck(index);
      if (check == null) {
        return;
      }

      if (bookmarksSet.add(check)) {
        bookmarks.add(check);
        hiddenResultsSet.remove(check);
      }
    }
  }

  public void moveBookmark(int index, boolean up) {
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

  public void hideResult(int index) {
    synchronized (mutex) {
      ItemCheck check = getCheck(index);

      hiddenResultsSet.add(check);
      if (bookmarksSet.remove(check)) {
        bookmarks.remove(check);
      }
    }
  }

  public void unhideResult(int index) {
    synchronized (mutex) {
      hiddenResultsSet.remove(getCheck(index));
    }
  }

  public void unhideResult(ItemCheck check) {
    synchronized (mutex) {
      hiddenResultsSet.remove(check);
    }
  }

  @Override
  public void checkAdded(ItemCheck check) {}

  @Override
  public void checkRemoved(ItemCheck check) {
    if (bookmarksSet.remove(check)) {
      bookmarks.remove(check);
    } else {
      hiddenResultsSet.remove(check);
    }
  }

  @Override
  public void checkReplaced(ItemCheck before, ItemCheck after) {
    if (bookmarksSet.remove(before)) {
      bookmarksSet.add(after);
      bookmarks.set(bookmarks.indexOf(before), after);
    } else if (hiddenResultsSet.remove(before)) {
      hiddenResultsSet.add(after);
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
    bookmarks.forEach(b -> bookmarksArr.add(b.id().id()));;
    obj.add("Bookmarks", bookmarksArr);

    JsonArray hiddenArr = new JsonArray();
    hiddenResultsSet.forEach(h -> hiddenArr.add(h.id().id()));
    obj.add("Hidden", hiddenArr);

    return obj;
  }

  @Override
  public void open(Version version, StateContext ctx, JsonElement json) {
    bookmarks.clear();
    bookmarksSet.clear();
    hiddenResultsSet.clear();

    JsonObject obj = json.getAsJsonObject();
    for (JsonElement bookmark : obj.get("Bookmarks").getAsJsonArray()) {
      ItemCheck check = ctx.checks().get(CheckId.of(bookmark.getAsLong()));
      bookmarks.add(check);
      bookmarksSet.add(check);
    }
    for (JsonElement hidden : obj.get("Hidden").getAsJsonArray()) {
      hiddenResultsSet.add(ctx.checks().get(CheckId.of(hidden.getAsLong())));
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
