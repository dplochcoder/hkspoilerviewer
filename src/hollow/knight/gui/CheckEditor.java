package hollow.knight.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.primitives.Ints;
import hollow.knight.logic.Cost;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.Term;
import hollow.knight.util.GuiUtil;

// Free-floating UI for editing a single check.
public final class CheckEditor extends JFrame implements ItemChecks.Listener {
  private static final long serialVersionUID = 1L;

  private final Application application;

  private final CheckEditorItemSearchField itemSearchField;
  private final CheckEditorItemsListModel itemsListModel;
  private final JList<String> itemsList;
  private final JScrollPane itemsPane;

  private ItemCheck checkForEdit;
  private final JLabel itemLabel;
  private final JLabel locationLabel;
  private final JPanel costsPanel;
  private final List<JButton> costButtons;

  private final List<CostEditor> costEditors = new ArrayList<>();

  public CheckEditor(Application application) {
    super("ICDL Check Editor");

    this.application = application;

    this.itemsListModel = new CheckEditorItemsListModel(application.ctx().checks());

    this.itemSearchField = new CheckEditorItemSearchField();
    this.itemSearchField.addListener(() -> repopulateItemResults());
    this.itemsList = createItemsList();
    this.itemsPane = new JScrollPane(itemsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    itemsPane.setMinimumSize(new Dimension(300, 300));

    this.itemLabel = new JLabel("");
    this.locationLabel = new JLabel("");
    this.costsPanel = new JPanel();
    costsPanel.setLayout(new BoxLayout(costsPanel, BoxLayout.PAGE_AXIS));

    this.costButtons = new ArrayList<>();

    JButton newCostButton = new JButton("New Cost");
    newCostButton
        .addActionListener(GuiUtil.newActionListener(this, () -> addCost(Cost.createGeo(1))));
    costButtons.add(newCostButton);

    this.addWindowListener(newWindowListener());

    JPanel contentPane = new JPanel();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
    itemSearchField.addToGui(contentPane);
    contentPane.add(itemsPane);
    contentPane.add(createCustomItemsPanel());
    contentPane.add(new JSeparator());
    contentPane.add(itemLabel);
    contentPane.add(locationLabel);
    contentPane.add(new JSeparator());
    contentPane.add(costsPanel);
    contentPane.add(newCostButton);
    contentPane.add(new JSeparator());
    contentPane.add(createActionPanel());
    contentPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    getContentPane().add(contentPane);

    updateSize();
    repopulateItemResults();
    editCheck(null);
    setVisible(true);

    application.ctx().checks().addListener(this);
  }

  private void updateSize() {
    int height =
        500 + costEditors.stream().mapToInt(e -> e.panel().getPreferredSize().height).sum();
    Dimension d = new Dimension(Math.max(425, getSize().width), height);

    setPreferredSize(d);
    setSize(d);
    repaint();
  }

  private JList<String> createItemsList() {
    JList<String> itemsList = new JList<String>(itemsListModel);
    itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(itemsList.getKeyListeners()).forEach(itemsList::removeKeyListener);
    itemsList.addKeyListener(newItemsListKeyListener());

    return itemsList;
  }

  private JButton customItemButton(String txt, Function<Integer, Item> itemFn) {
    JButton button = new JButton(txt);
    button.addActionListener(GuiUtil.newActionListener(this, () -> {
      String valueStr = JOptionPane.showInputDialog(CheckEditor.this, "Enter custom amount");
      if (valueStr == null || valueStr.trim().isEmpty()) {
        return;
      }

      Integer val = Ints.tryParse(valueStr);
      if (val == null || val <= 0) {
        JOptionPane.showMessageDialog(CheckEditor.this, "Must enter a positive integer");
        return;
      }

      application.ctx().checks().addItem(itemFn.apply(val));
    }));
    return button;
  }

  private JPanel createCustomItemsPanel() {
    JPanel panel = new JPanel();
    panel.add(customItemButton("Add Custom Geo", Item::newGeoItem));
    panel.add(customItemButton("Add Custom Essence", Item::newEssenceItem));
    return panel;
  }

  private JPanel createActionPanel() {
    JPanel panel = new JPanel();

    JButton applyCosts = new JButton("Apply Costs");
    applyCosts.addActionListener(GuiUtil.newActionListener(this, this::applyCosts));
    panel.add(applyCosts);
    costButtons.add(applyCosts);

    JButton resetCosts = new JButton("Reset Costs");
    resetCosts.addActionListener(GuiUtil.newActionListener(this, () -> editCheck(checkForEdit)));
    panel.add(resetCosts);
    costButtons.add(resetCosts);

    return panel;
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

  private void addCost(Cost cost) {
    CostEditor next = new CostEditor(cost, this::deleteCostEditor);
    costEditors.add(next);
    costsPanel.add(next.panel());

    if (costEditors.size() > 1) {
      CostEditor prev = costEditors.get(costEditors.size() - 2);
      prev.setDownNeighbor(next);
      next.setUpNeighbor(prev);
    }

    updateSize();
  }

  private void deleteCostEditor(CostEditor editor) {
    int index = costEditors.indexOf(editor);
    costEditors.remove(index);
    costsPanel.remove(editor.panel());

    CostEditor before = index > 0 ? costEditors.get(index - 1) : null;
    CostEditor after = index < costEditors.size() ? costEditors.get(index) : null;

    if (before != null) {
      before.setDownNeighbor(after);
    }
    if (after != null) {
      after.setUpNeighbor(before);
    }

    updateSize();
  }

  private void applyCosts() {
    List<Cost> costs = new ArrayList<>();
    Set<Term> costTerms = new HashSet<>();

    for (int i = 0; i < costEditors.size(); i++) {
      CostEditor e = costEditors.get(i);
      Cost c = e.getCost();

      if (c == null) {
        JOptionPane.showMessageDialog(this, "Error: Cost #" + (i + 1) + " is not valid");
        return;
      }

      Term term = c.type() == Cost.Type.GEO ? Term.geo() : c.term();
      if (!costTerms.add(term)) {
        JOptionPane.showMessageDialog(this, "Error: Duplicate costs for type " + term.name());
        return;
      }

      costs.add(c);
    }

    Costs newCosts = new Costs(ImmutableSet.copyOf(costs));
    application.ctx().checks().replace(checkForEdit.id(), checkForEdit.location(),
        checkForEdit.item(), newCosts, false);
    application.refreshLogic();
  }

  private void updateCheckMetadata() {
    if (checkForEdit == null) {
      itemLabel.setText("Item: <none selected>");
      locationLabel.setText("Location: <none selected>");
    } else {
      itemLabel.setText(
          "Item: " + checkForEdit.item().displayName() + " " + checkForEdit.item().valueSuffix());
      locationLabel.setText("Location: " + checkForEdit.location().name());
    }
  }

  public void editCheck(ItemCheck check) {
    checkForEdit = check;
    updateCheckMetadata();

    costEditors.forEach(e -> costsPanel.remove(e.panel()));
    costEditors.clear();

    if (check != null) {
      costButtons.forEach(b -> b.setEnabled(true));
      costButtons.forEach(b -> b.setToolTipText(""));
      check.costs().costs().forEach(this::addCost);
    } else {
      costButtons.forEach(b -> b.setEnabled(false));
      costButtons.forEach(b -> b.setToolTipText("Select a check to edit with 'E' first"));
    }
    updateSize();
  }

  public Item selectedItem() {
    return itemsListModel.get(itemsList.getSelectedIndex());
  }

  private ItemCheck getSemiUniqueItemCheck() {
    Item item = selectedItem();
    if (item == null) {
      return null;
    }

    ImmutableSet<ItemCheck> checks = application.ctx().checks().allChecks()
        .filter(c -> c.item().term().equals(item.term())).collect(ImmutableSet.toImmutableSet());
    if (checks.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No checks with this item to edit",
          "Cannot perform action", JOptionPane.WARNING_MESSAGE);
      return null;
    }

    ItemCheck check = checks.asList().get(0);
    for (int i = 1; i < checks.size(); i++) {
      // Check that location and costs match.
      ItemCheck c = checks.asList().get(i);
      if (!c.location().name().equals(check.location().name())) {
        JOptionPane.showMessageDialog(this, "Ambiguous: Item exists at multiple locations",
            "Cannot perform action", JOptionPane.WARNING_MESSAGE);
        return null;
      }
      if (!c.costs().equals(check.costs())) {
        JOptionPane.showMessageDialog(this, "Ambiguous: Item has multiple cost types",
            "Cannot perform action", JOptionPane.WARNING_MESSAGE);
        return null;
      }
    }

    if (!application.ensureRandomized(check)) {
      return null;
    }

    return check;
  }

  private void selectUniqueItemCheck() {
    ItemCheck check = getSemiUniqueItemCheck();
    if (check == null) {
      return;
    }

    editCheck(check);
  }

  private void duplicateUniqueItemCheck() {
    ItemCheck check = getSemiUniqueItemCheck();
    if (check == null) {
      return;
    }

    application.duplicateCheck(check);
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
        if (e.getKeyCode() != KeyEvent.VK_Q && e.getKeyCode() != KeyEvent.VK_C
            && e.getKeyCode() != KeyEvent.VK_E && !UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_Q) {
          itemsList.clearSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
          ItemCheck searchCheck = application.getSelectedSearchResultCheck();
          ItemCheck routeCheck = application.getSelectedRouteCheck();
          if ((routeCheck == null) != (searchCheck == null)) {
            application.copyCheckEditorItem(routeCheck == null ? searchCheck : routeCheck);
          } else if (routeCheck == searchCheck) {
            application.copyCheckEditorItem(routeCheck);
          } else {
            JOptionPane.showMessageDialog(CheckEditor.this,
                "Ambiguous: Different checks selected in Search and Route (Q to deselect)",
                "Cannot perform action", JOptionPane.WARNING_MESSAGE);
          }
        } else if (e.getKeyCode() == KeyEvent.VK_E) {
          selectUniqueItemCheck();
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          duplicateUniqueItemCheck();
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

  @Override
  public void checkAdded(ItemCheck check) {}

  @Override
  public void checkRemoved(ItemCheck check) {
    if (checkForEdit == check) {
      editCheck(null);
    }
  }

  @Override
  public void checkReplaced(ItemCheck before, ItemCheck after) {
    if (checkForEdit == before) {
      // Don't update cost fields; might be editing.
      checkForEdit = after;
      updateCheckMetadata();
      repaint();
    }
  }

}
