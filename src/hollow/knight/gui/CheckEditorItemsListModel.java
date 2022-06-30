package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.SynchronizedEntityManager;
import hollow.knight.logic.Term;

public final class CheckEditorItemsListModel implements ListModel<String>, ItemChecks.Listener {

  private final SynchronizedEntityManager<ListDataListener> listeners =
      new SynchronizedEntityManager<>();

  private final SceneNicknames sceneNicknames;
  private final ItemChecks checks;

  private final Multiset<Term> itemCounts = HashMultiset.create();
  private final List<Item> resultItems = new ArrayList<>();
  private final List<String> resultStrings = new ArrayList<>();
  private final Comparator<Item> sorter;

  public CheckEditorItemsListModel(SceneNicknames sceneNicknames, ItemChecks checks) {
    this.sceneNicknames = sceneNicknames;
    this.checks = checks;

    checks.allChecks().forEach(c -> itemCounts.add(c.item().term()));
    checks.addListener(this);

    this.sorter = Comparator.comparing(item -> item.displayName(sceneNicknames).toLowerCase());
  }

  private String diffSuffix(Term term) {
    int diff = itemCounts.count(term) - checks.originalItemCount(term.name());
    return diff == 0 ? "" : ((diff > 0 ? ", +" : ", ") + diff);
  }

  private String render(Item item) {
    return "(" + itemCounts.count(item.term()) + diffSuffix(item.term()) + ") "
        + item.displayName(sceneNicknames) + " " + item.valueSuffix();
  }

  public void updateResults(StateContext ctx, List<Item> resultItems) {
    int oldSize = this.resultStrings.size();

    this.resultItems.clear();
    this.resultStrings.clear();
    resultItems.stream().filter(i -> i.isCustom() || ctx.checks().isOriginalNonVanilla(i.term()))
        .sorted(sorter).forEach(i -> {
          this.resultItems.add(i);
          this.resultStrings.add(render(i));
        });

    int newSize = resultStrings.size();
    ListDataEvent e =
        new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Math.max(oldSize, newSize));
    listeners.forEach(l -> l.contentsChanged(e));
  }

  public Item get(int index) {
    if (index < 0 || index >= resultItems.size()) {
      return null;
    }

    return resultItems.get(index);
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
