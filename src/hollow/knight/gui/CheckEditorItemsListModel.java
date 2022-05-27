package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.Term;

public final class CheckEditorItemsListModel implements ListModel<String>, ItemChecks.Listener {

  private final Object mutex = new Object();
  private final Set<ListDataListener> listeners = new HashSet<>();

  private final Multiset<Term> itemCounts = HashMultiset.create();
  private final List<Item> resultItems = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();
  private final Comparator<Item> sorter =
      Comparator.comparing(item -> item.displayName().toLowerCase());

  public CheckEditorItemsListModel(ItemChecks checks) {
    checks.allChecks().forEach(c -> itemCounts.add(c.item().term()));
    checks.addListener(this);
  }

  private String render(Item item) {
    return "(" + itemCounts.count(item.term()) + ") " + item.displayName() + " "
        + item.valueSuffix();
  }

  public void updateResults(List<Item> resultItems) {
    List<ListDataListener> listenersCopy;
    int oldSize, newSize;
    synchronized (mutex) {
      oldSize = this.resultStrings.size();
      listenersCopy = new ArrayList<>(listeners);

      this.resultItems.clear();
      this.resultStrings.clear();
      resultItems.stream().sorted(sorter).forEach(i -> {
        this.resultItems.add(i);
        this.resultStrings.add(render(i));
      });

      newSize = resultStrings.size();
    }

    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    for (ListDataListener listener : listenersCopy) {
      listener.contentsChanged(e);
    }
  }

  public Item get(int index) {
    synchronized (mutex) {
      if (index < 0 || index >= resultItems.size()) {
        return null;
      }

      return resultItems.get(index);
    }
  }

  @Override
  public void checkAdded(ItemCheck check) {
    itemCounts.add(check.item().term());
  }

  @Override
  public void checkRemoved(ItemCheck check) {
    itemCounts.remove(check.item().term());
  }

  @Override
  public void checkReplaced(ItemCheck before, ItemCheck after) {
    checkRemoved(before);
    checkAdded(after);
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
