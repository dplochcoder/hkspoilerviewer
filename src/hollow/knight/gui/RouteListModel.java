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
import javax.swing.JPanel;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;

public final class RouteListModel implements ListModel<String>, SaveInterface {

  private final Object mutex = new Object();

  private StateContext ctx;
  private State initialState; // No checks
  private State currentState; // All checks up to but excluding insertionPoint
  private State finalState; // All checks
  private int insertionPoint = 0; // New elements go @ this index
  private final List<SearchResult> route = new ArrayList<>();
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
      public void addGuiToPanel(JPanel panel) {}

      @Override
      public boolean accept(SearchResult result) {
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
          newState.acquireItemCheck(route.get(i).itemCheck());
        }
        newState.normalize();

        return newState;
      } else {
        State newState = currentState.deepCopy();
        for (int i = insertionPoint; i <= index; i++) {
          newState.acquireItemCheck(route.get(i).itemCheck());
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
        currentState.acquireItemCheck(route.get(i).itemCheck());
      }
      currentState.normalize();

      listenersCopy = new ArrayList<>(listeners);
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, getSize());
    listenersCopy.forEach(l -> l.contentsChanged(e));
  }

  public void addToRoute(SearchResult result) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      listenersCopy = new ArrayList<>(listeners);

      currentState.acquireItemCheck(result.itemCheck());
      currentState.normalize();

      this.route.add(insertionPoint, result);
      this.resultStrings.add(insertionPoint, result.render());
      ++insertionPoint;

      finalState = currentState.deepCopy();
      for (int i = insertionPoint; i < route.size(); i++) {
        SearchResult newResult = SearchResult.create(route.get(i).itemCheck(), finalState);
        route.set(i, newResult);
        resultStrings.set(i, newResult.render());

        finalState.acquireItemCheck(route.get(i).itemCheck());
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

      ItemCheck a = this.route.get(before).itemCheck();
      ItemCheck b = this.route.get(after).itemCheck();

      State prevState = getState(before - 1);
      State newState1 = prevState.deepCopy();
      newState1.acquireItemCheck(b);
      newState1.normalize();
      State newState2 = newState1.deepCopy();
      newState2.acquireItemCheck(a);
      newState2.normalize();

      route.set(before, SearchResult.create(b, prevState));
      route.set(after, SearchResult.create(a, newState1));
      resultStrings.set(before, route.get(before).render());
      resultStrings.set(after, route.get(after).render());

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
        ItemCheck check = route.get(i).itemCheck();
        finalState.acquireItemCheck(check);
        finalState.normalize();

        route.set(i - 1, SearchResult.create(check, finalState));
        resultStrings.set(i - 1, route.get(i - 1).render());
      }

      route.remove(route.size() - 1);
      resultStrings.remove(resultStrings.size() - 1);

      if (index < insertionPoint) {
        --insertionPoint;
        currentState = initialState.deepCopy();
        for (int i = 0; i < insertionPoint; i++) {
          currentState.acquireItemCheck(route.get(i).itemCheck());
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

  @Override
  public String saveName() {
    return "RouteListModel";
  }

  @Override
  public JsonElement save() {
    JsonObject obj = new JsonObject();

    JsonArray arr = new JsonArray();
    route.forEach(r -> arr.add(r.itemCheck().id()));
    obj.add("Route", arr);

    return obj;
  }

  @Override
  public void open(String version, StateContext ctx, JsonElement json) {
    this.ctx = ctx;
    this.initialState = ctx.newInitialState();
    this.currentState = this.initialState.deepCopy();
    this.finalState = this.initialState.deepCopy();
    this.insertionPoint = 0;

    this.route.clear();
    this.resultStrings.clear();
    for (JsonElement id : json.getAsJsonObject().get("Route").getAsJsonArray()) {
      ItemCheck check = ctx.items().get(id.getAsInt());
      addToRoute(SearchResult.create(check, currentState));
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
