package hollow.knight.gui;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JFileChooser;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.filechooser.FileFilter;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.State;

public final class RouteListModel implements ListModel<String> {

  private final Object mutex = new Object();

  private final State initialState;
  private State currentState;
  private final List<SearchEngine.Result> route = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();

  private final Set<ListDataListener> listeners = new HashSet<>();

  public RouteListModel(State initialState) {
    this.initialState = initialState.deepCopy();
    this.currentState = initialState.deepCopy();
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

    String out;
    synchronized (mutex) {
      out = resultStrings.stream().collect(Collectors.joining("\n"));
    }

    Files.write(f.toPath(), out.getBytes(StandardCharsets.UTF_8));
  }

  public State currentState() {
    return getState(route.size() - 1);
  }

  private State getState(int index) {
    synchronized (mutex) {
      if (index == route.size() - 1) {
        return currentState;
      } else if (index == -1) {
        return initialState;
      } else {
        State newState = initialState.deepCopy();
        for (int i = 0; i <= index; i++) {
          newState.acquireItemCheck(route.get(i).itemCheck());
        }
        newState.normalize();

        return newState;
      }
    }
  }

  public void addToRoute(SearchEngine.Result result) {
    List<ListDataListener> listenersCopy;
    int newSize;
    synchronized (mutex) {
      listenersCopy = new ArrayList<>(listeners);

      currentState.acquireItemCheck(result.itemCheck());
      currentState.normalize();

      this.route.add(result);
      this.resultStrings.add((resultStrings.size() + 1) + ": " + result.render());
      newSize = this.resultStrings.size();
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, newSize - 1, newSize);
    for (ListDataListener listener : listenersCopy) {
      listener.contentsChanged(e);
    }
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

      route.set(before, SearchEngine.Result.create(b, prevState));
      route.set(after, SearchEngine.Result.create(a, newState1));
      resultStrings.set(before, (before + 1) + ": " + route.get(before).render());
      resultStrings.set(after, (after + 1) + ": " + route.get(after).render());
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, before, after + 1);
    for (ListDataListener listener : listenersCopy) {
      listener.contentsChanged(e);
    }
  }

  public void removeCheck(int index) {
    List<ListDataListener> listenersCopy;
    synchronized (mutex) {
      if (index < 0 || index >= getSize()) {
        return;
      }
      listenersCopy = new ArrayList<>(listeners);

      currentState = getState(index - 1).deepCopy();
      for (int i = index + 1; i < getSize(); i++) {
        ItemCheck check = route.get(i).itemCheck();
        currentState.acquireItemCheck(check);
        currentState.normalize();

        route.set(i - 1, SearchEngine.Result.create(check, currentState));
        resultStrings.set(i - 1, i + ": " + route.get(i - 1).render());
      }

      route.remove(route.size() - 1);
      resultStrings.remove(resultStrings.size() - 1);
    }

    ListDataEvent e = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index + 1);
    for (ListDataListener listener : listenersCopy) {
      listener.contentsChanged(e);
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
