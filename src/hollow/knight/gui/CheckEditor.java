package hollow.knight.gui;

import java.awt.Dimension;
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
import hollow.knight.logic.Item;

// Free-floating UI for editing a single check.
public final class CheckEditor extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Application application;

  private final CheckEditorItemSearchField itemSearchField;
  private final CheckEditorItemListModel itemListModel;
  private final JList<String> itemsList;
  private final JScrollPane itemsPane;
  // TODO: Add UI for custom geo+essence items

  public CheckEditor(Application application) {
    super("ICDL Check Editor");

    this.application = application;

    this.itemListModel = new CheckEditorItemListModel(application.ctx().checks());

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
    JList<String> itemsList = new JList<String>(itemListModel);
    itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(itemsList.getKeyListeners()).forEach(itemsList::removeKeyListener);

    return itemsList;
  }

  private static boolean needsExpansion(JScrollPane pane) {
    return pane.getPreferredSize().width > pane.getSize().width;
  }

  public void repopulateItemResults() {
    ImmutableList<Item> results = application.ctx().checks().allItems()
        .filter(itemSearchField::accept).collect(ImmutableList.toImmutableList());
    itemListModel.updateResults(results);

    if (needsExpansion(itemsPane)) {
      pack();
    }
    repaint();
  }

  public Item selectedItem() {
    // FIXME
    return null;
  }

  private final WindowListener newWindowListener() {
    return new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        application.editorClosed();
      }
    };
  }

}
