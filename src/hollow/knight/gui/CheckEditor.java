package hollow.knight.gui;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Arrays;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.Item;

// Free-floating UI for editing a single check.
public final class CheckEditor extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Application application;

  private final CheckEditorItemSearchField itemSearchField;
  private final CheckEditorItemsListModel itemsListModel;
  private final JList<String> itemsList;
  private final JScrollPane itemsPane;
  // TODO: Add UI for custom geo+essence items

  public CheckEditor(Application application) {
    super("ICDL Check Editor");

    this.application = application;

    this.itemsListModel = new CheckEditorItemsListModel(application.ctx().checks());

    this.itemSearchField = new CheckEditorItemSearchField();
    this.itemSearchField.addListener(() -> repopulateItemResults());
    this.itemsList = createItemsList();
    this.itemsPane = new JScrollPane(itemsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    itemsPane.setMinimumSize(new Dimension(300, 200));

    this.addWindowListener(newWindowListener());

    getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
    itemSearchField.addToGui(getContentPane());
    getContentPane().add(itemsPane);

    pack();
    repopulateItemResults();
    setVisible(true);
  }

  private JList<String> createItemsList() {
    JList<String> itemsList = new JList<String>(itemsListModel);
    itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(itemsList.getKeyListeners()).forEach(itemsList::removeKeyListener);
    itemsList.addKeyListener(newItemsListKeyListener());

    return itemsList;
  }

  private static boolean needsExpansion(JScrollPane pane) {
    return pane.getPreferredSize().width > pane.getSize().width;
  }

  public void repopulateItemResults() {
    ImmutableList<Item> results = application.ctx().checks().allItems()
        .filter(itemSearchField::accept).collect(ImmutableList.toImmutableList());
    itemsListModel.updateResults(results);

    if (needsExpansion(itemsPane)) {
      pack();
    }
    repaint();
  }

  public Item selectedItem() {
    // TODO: Support custom geo and essence items
    return itemsListModel.get(itemsList.getSelectedIndex());
  }

  private WindowListener newWindowListener() {
    return new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        application.editorClosed();
      }
    };
  }

  private static final ImmutableMap<Integer, Integer> UP_DOWN_VALUES = ImmutableMap.of(
      KeyEvent.VK_UP, -1, KeyEvent.VK_DOWN, 1, KeyEvent.VK_PAGE_UP, -5, KeyEvent.VK_PAGE_DOWN, 5);

  private KeyListener newItemsListKeyListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        // TODO: Make key codes configurable.
        if (e.getKeyCode() != KeyEvent.VK_C && e.getKeyCode() != KeyEvent.VK_SPACE
            && !UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_C || e.getKeyCode() == KeyEvent.VK_SPACE) {
          application.copyCheckEditorItem(true);
        } else {
          // Navigate up or down.
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = itemsList.getSelectedIndex() + delta;
          if (newIndex < 0) {
            newIndex = 0;
          } else if (newIndex >= itemsListModel.getSize()) {
            newIndex = itemsListModel.getSize() - 1;
          }

          itemsList.setSelectedIndex(newIndex);
        }
      }
    };
  }

}
