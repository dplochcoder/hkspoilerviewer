package hollow.knight.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.logic.CheckId;
import hollow.knight.logic.DarknessOverrides;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.SynchronizedEntityManager;
import hollow.knight.logic.Version;

public final class SearchResultsListModel
    implements ListModel<String>, ItemChecks.Listener, SaveInterface {

  private final SynchronizedEntityManager<ListDataListener> listeners =
      new SynchronizedEntityManager<>();

  private final Set<ItemCheck> bookmarksSet = new HashSet<>();
  private final Set<ItemCheck> hiddenResultsSet = new HashSet<>();

  private final List<ItemCheck> bookmarks = new ArrayList<>();
  private final List<SearchResult> results = new ArrayList<>();
  private final List<SearchResult> hiddenResults = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  private static final String SEPARATOR = "----------------------------------------";

  private final Set<ItemCheck> matchingResults = new HashSet<>();

  private final TransitionData transitionData;
  private final Supplier<Boolean> showRawTransitions;
  private final Supplier<DarknessOverrides> darkness;
  private final Predicate<ItemCheck> isRouted;

  public SearchResultsListModel(TransitionData transitionData, Supplier<Boolean> showRawTransitions,
      Supplier<DarknessOverrides> darkness, Predicate<ItemCheck> isRouted) {
    this.transitionData = transitionData;
    this.showRawTransitions = showRawTransitions;
    this.darkness = darkness;
    this.isRouted = isRouted;
  }

  public int numBookmarks() {
    return bookmarks.size();
  }

  public boolean isMatchingSearchResult(ItemCheck check) {
    return matchingResults.contains(check);
  }

  public int indexOfSearchResult(ItemCheck check) {
    if (bookmarksSet.contains(check)) {
      return bookmarks.indexOf(check);
    }

    for (int i = 0; i < results.size(); i++) {
      if (results.get(i).itemCheck().equals(check)) {
        return bookmarks.size() + 1 + i;
      }
    }

    return -1;
  }

  private String render(SearchResult result) {
    return (isRouted.test(result.itemCheck()) ? "(R) " : "") + result
        .render(showRawTransitions.get() ? TransitionData.empty() : transitionData, darkness.get());
  }

  public void updateResults(State state, List<SearchResult> newResults) {
    matchingResults.clear();
    newResults.forEach(r -> matchingResults.add(r.itemCheck()));

    int oldSize = this.resultStrings.size();

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

    bookmarks.forEach(b -> resultStrings.add(render(SearchResult.create(b, state))));
    resultStrings.add(SEPARATOR);
    results.forEach(r -> resultStrings.add(render(r)));
    resultStrings.add(SEPARATOR);
    hiddenResults.forEach(r -> resultStrings.add(render(r)));

    int newSize = resultStrings.size();

    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    listeners.forEach(l -> l.contentsChanged(e));
  }

  public void removeBookmark(ItemCheck check) {
    if (bookmarksSet.remove(check)) {
      bookmarks.remove(check);
    }
  }

  public ItemCheck getCheck(int index) {
    if (index < 0) {
      return null;
    }

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
    if (index < 0 || index >= hiddenResults.size()) {
      return null;
    }

    return hiddenResults.get(index).itemCheck();
  }

  public SearchResult getResult(State state, int index) {
    if (index < 0) {
      return null;
    }

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
    if (index < 0 || index >= hiddenResults.size()) {
      return null;
    }

    return hiddenResults.get(index);
  }

  private void brighten(Component c) {
    c.setForeground(Color.GRAY);
  }

  public void adjustComponentStyle(Component c, State state, SearchEngine engine, int index) {
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

    if (s.itemCheck().isTransition()) {
      Font f = c.getFont();
      c.setFont(new Font(f.getFontName(), Font.ITALIC, f.getSize()));
    }
  }

  public void addBookmark(int index) {
    ItemCheck check = getCheck(index);
    if (check == null) {
      return;
    }

    if (bookmarksSet.add(check)) {
      bookmarks.add(check);
      hiddenResultsSet.remove(check);
    }
  }

  public void moveBookmark(int index, boolean up) {
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

  public void deleteBookmark(State state, int index) {
    if (index >= bookmarks.size()) {
      return;
    }

    bookmarksSet.remove(bookmarks.remove(index));
  }

  public void hideResult(int index) {
    ItemCheck check = getCheck(index);

    hiddenResultsSet.add(check);
    if (bookmarksSet.remove(check)) {
      bookmarks.remove(check);
    }
  }

  public void unhideResult(int index) {
    hiddenResultsSet.remove(getCheck(index));
  }

  public void unhideResult(ItemCheck check) {
    hiddenResultsSet.remove(check);
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

    if (json != null) {
      JsonObject obj = json.getAsJsonObject();
      for (JsonElement bookmark : obj.get("Bookmarks").getAsJsonArray()) {
        ItemCheck check = ctx.checks().get(CheckId.of(bookmark.getAsInt()));
        if (check != null) {
          bookmarks.add(check);
          bookmarksSet.add(check);
        }
      }
      for (JsonElement hidden : obj.get("Hidden").getAsJsonArray()) {
        ItemCheck check = ctx.checks().get(CheckId.of(hidden.getAsInt()));
        if (check != null) {
          hiddenResultsSet.add(check);
        }
      }
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
