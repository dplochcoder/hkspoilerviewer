package hollow.knight.gui;

import java.awt.Color;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JFileChooser;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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

public final class RouteListModel implements ItemChecks.Listener, ListModel<String>, SaveInterface {

  private final Object mutex = new Object();

  private StateContext ctx;
  private State initialState; // No checks
  private State currentState; // All checks up to but excluding insertionPoint
  private State finalState; // All checks
  private int insertionPoint = 0; // New elements go @ this index

  private final List<ItemCheck> route = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  private final Set<ListDataListener> listeners = new HashSet<>();
  private final SearchResult.Filter futureFilter;

  public RouteListModel(StateContext ctx) {
    this.ctx = ctx;
    this.initialState = ctx.newInitialState();
    this.currentState = this.initialState.deepCopy();
    this.finalState = this.initialState.deepCopy();
    this.futureFilter = new SearchResult.Filter() {
      @Override
      public boolean accept(StateContext ctx, SearchResult result) {
        return !finalState.isAcquired(result.itemCheck());
      }
    };
  }

  public SearchResult.Filter futureCheckFilter() {
    return this.futureFilter;
  }

  public void saveAsTxt(Component parent) throws IOException {
    JFileChooser j = new JFileChooser("Save Route as *.txt");
    j.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().endsWith(".txt");
      }

      @Override
      public String getDescription() {
        return "*.txt";
      }
    });
    if (j.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File f = j.getSelectedFile();

    StringBuilder out = new StringBuilder();
    synchronized (mutex) {
      for (int i = 0; i < resultStrings.size(); i++) {
        out.append((i + 1) + ": " + resultStrings.get(i));
        if (i < resultStrings.size() - 1) {
          out.append("\n");
        }
      }
    }

    Files.write(f.toPath(), out.toString().getBytes(StandardCharsets.UTF_8));
  }

  public StateContext ctx() {
    return ctx;
  }

  public ItemCheck getCheck(int index) {
    synchronized (mutex) {
      if (index < 0 || index >= getSize()) {
        return null;
      }

      return route.get(index);
    }
  }

  public int indexOfRouteCheck(ItemCheck check) {
    return route.indexOf(check);
  }

  public State currentState() {
    return getState(insertionPoint - 1);
  }

  private State getState(int index) {
    synchronized (mutex) {
      if (index == -1) {
        return initialState;
      } else if (index == insertionPoint - 1) {
        return currentState;
      } else if (index == route.size() - 1) {
        return finalState;
      } else if (index < insertionPoint) {
        State newState = initialState.deepCopy();
        for (int i = 0; i <= index; i++) {
          newState.acquireCheck(route.get(i));
        }
        newState.normalize();

        return newState;
      } else {
        State newState = currentState.deepCopy();
        for (int i = insertionPoint; i <= index; i++) {
          newState.acquireCheck(route.get(i));
        }
        newState.normalize();

        return newState;
      }
    }
  }

  public void adjustForegroundColor(Component c, int index) {
    if (index >= insertionPoint) {
      c.setForeground(Color.GRAY);
    }
  }

  public void setInsertionPoint(int newInsertionPoint) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      if (newInsertionPoint < 0 || newInsertionPoint > getSize()) {
        return;
      }
      insertionPoint = newInsertionPoint;

      currentState = initialState.deepCopy();
      for (int i = 0; i < insertionPoint; i++) {
        currentState.acquireCheck(route.get(i));
      }
      currentState.normalize();

      listenersCopy = new ArrayList<>(listeners);
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  public void updateSkips(Skips skips) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      listenersCopy = new ArrayList<>(listeners);

      initialState = ctx.newInitialState();
      skips.applySkips(initialState);

      finalState = initialState.deepCopy();
      for (int i = 0; i < route.size(); i++) {
        if (i == insertionPoint) {
          currentState = finalState.deepCopy();
        }

        SearchResult newResult = SearchResult.create(route.get(i), finalState);
        resultStrings.set(i, newResult.render());

        finalState.acquireCheck(route.get(i));
        finalState.normalize();
      }

      if (route.size() == insertionPoint) {
        currentState = finalState.deepCopy();
      }
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  public void addToRoute(ItemCheck check) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      listenersCopy = new ArrayList<>(listeners);

      currentState.acquireCheck(check);
      currentState.normalize();

      this.route.add(insertionPoint, check);
      this.resultStrings.add(insertionPoint, SearchResult.create(check, currentState).render());
      ++insertionPoint;

      finalState = currentState.deepCopy();
      for (int i = insertionPoint; i < route.size(); i++) {
        SearchResult newResult = SearchResult.create(route.get(i), finalState);
        resultStrings.set(i, newResult.render());

        finalState.acquireCheck(check);
        finalState.normalize();
      }
    }

    ListDataEvent e1 =
        new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, insertionPoint - 1, insertionPoint);
    listenersCopy.forEach(l -> l.contentsChanged(e1));

    ListDataEvent e2 =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, insertionPoint, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e2));
  }

  public void swap(int before, int after) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      if (before + 1 != after || before < 0 || after >= getSize()) {
        return;
      }

      listenersCopy = new ArrayList<>(listeners);

      ItemCheck a = route.get(before);
      ItemCheck b = route.get(after);

      State prevState = getState(before - 1);
      State newState1 = prevState.deepCopy();
      newState1.acquireCheck(b);
      newState1.normalize();
      State newState2 = newState1.deepCopy();
      newState2.acquireCheck(a);
      newState2.normalize();

      route.set(before, b);
      route.set(after, a);
      resultStrings.set(before, SearchResult.create(b, newState1).render());
      resultStrings.set(after, SearchResult.create(a, newState2).render());

      if (insertionPoint == after) {
        currentState = newState1;
      }
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, before, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  public void removeCheck(int index) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      if (index < 0 || index >= getSize()) {
        return;
      }
      listenersCopy = new ArrayList<>(listeners);

      finalState = getState(index - 1).deepCopy();
      for (int i = index + 1; i < getSize(); i++) {
        ItemCheck check = route.get(i);
        finalState.acquireCheck(check);
        finalState.normalize();

        route.set(i - 1, check);
        resultStrings.set(i - 1, SearchResult.create(check, finalState).render());
      }

      route.remove(route.size() - 1);
      resultStrings.remove(resultStrings.size() - 1);

      if (index < insertionPoint) {
        --insertionPoint;
        currentState = initialState.deepCopy();
        for (int i = 0; i < insertionPoint; i++) {
          currentState.acquireCheck(route.get(i));
        }
        currentState.normalize();
      }
    }

    ListDataEvent e1 = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + 1);
    listenersCopy.forEach(l -> l.contentsChanged(e1));

    ListDataEvent e2 =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index + 1, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e2));
  }

  public void replaceCheck(int index, ItemCheck replacement) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      if (index < 0 || index >= getSize()) {
        return;
      }
      listenersCopy = new ArrayList<>(listeners);

      finalState = getState(index - 1).deepCopy();
      route.set(index, replacement);
      for (int i = index; i < getSize(); i++) {
        ItemCheck check = route.get(i);
        resultStrings.set(i, SearchResult.create(check, finalState).render());

        finalState.acquireCheck(check);
        finalState.normalize();
      }

      if (index < insertionPoint) {
        currentState = initialState.deepCopy();
        for (int i = 0; i < insertionPoint; i++) {
          currentState.acquireCheck(route.get(i));
        }
        currentState.normalize();
      }
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  public void refreshLogic() {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      listenersCopy = new ArrayList<>(listeners);

      finalState = ctx.newInitialState();
      for (int i = 0; i < getSize(); i++) {
        ItemCheck check = route.get(i);
        resultStrings.set(i, SearchResult.create(check, finalState).render());
        if (i == insertionPoint) {
          currentState = finalState.deepCopy();
        }

        finalState.acquireCheck(check);;
        finalState.normalize();
      }

      if (insertionPoint == getSize()) {
        currentState = finalState.deepCopy();
      }
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  @Override
  public void checkAdded(ItemCheck check) {}

  @Override
  public void checkRemoved(ItemCheck check) {
    int index = route.indexOf(check);
    if (index != -1) {
      removeCheck(index);
    }
  }

  @Override
  public void multipleChecksRemoved(ImmutableSet<ItemCheck> checks) {
    boolean anyRemoved = false;
    synchronized (mutex) {
      anyRemoved = route.removeIf(checks::contains);
      resultStrings.subList(route.size(), resultStrings.size()).clear();
    }

    if (anyRemoved) {
      refreshLogic();
    }
  }

  @Override
  public void checkReplaced(ItemCheck before, ItemCheck after) {
    int index = route.indexOf(before);
    if (index != -1) {
      replaceCheck(index, after);
    }
  }

  @Override
  public void multipleChecksReplaced(ImmutableMap<ItemCheck, ItemCheck> replacements) {
    boolean anyReplaced = false;
    synchronized (mutex) {
      for (int i = 0; i < route.size(); i++) {
        ItemCheck replacement = replacements.get(route.get(i));
        if (replacement != null) {
          route.set(i, replacement);
          anyReplaced = true;
        }
      }
    }

    if (anyReplaced) {
      refreshLogic();
    }
  }

  @Override
  public String saveName() {
    return "RouteListModel";
  }

  @Override
  public JsonElement save() {
    JsonObject obj = new JsonObject();

    JsonArray arr = new JsonArray();
    route.forEach(r -> arr.add(r.id().id()));
    obj.add("Route", arr);

    return obj;
  }

  @Override
  public void open(Version version, StateContext ctx, JsonElement json) {
    this.ctx = ctx;
    this.initialState = ctx.newInitialState();
    this.currentState = this.initialState.deepCopy();
    this.finalState = this.initialState.deepCopy();
    this.insertionPoint = 0;

    this.route.clear();
    this.resultStrings.clear();
    if (json != null) {
      for (JsonElement id : json.getAsJsonObject().get("Route").getAsJsonArray()) {
        addToRoute(ctx.checks().get(CheckId.of(id.getAsInt())));
      }
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
      return (index + 1) + ": " + resultStrings.get(index);
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
