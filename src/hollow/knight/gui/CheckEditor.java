package hollow.knight.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import hollow.knight.logic.Condition;
import hollow.knight.logic.Cost;
import hollow.knight.logic.Costs;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.MutableTermMap;
import hollow.knight.logic.Term;
import hollow.knight.logic.TermMap;

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
    newCostButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        addCost(Cost.createGeo(1));
      }
    });
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

  private JButton customItemButton(String txt, Term term, ImmutableSet<String> types) {
    JButton button = new JButton(txt);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        String valueStr = JOptionPane.showInputDialog(CheckEditor.this, "Enter custom amount");
        if (valueStr == null || valueStr.trim().isEmpty()) {
          return;
        }

        Integer val = Ints.tryParse(valueStr);
        if (val == null || val <= 0) {
          JOptionPane.showMessageDialog(CheckEditor.this, "Must enter a positive integer");
          return;
        }

        MutableTermMap termMap = new MutableTermMap();
        termMap.set(term, val);
        Item item = new Item(Term.create(val + "_" + term.name()), types, Condition.alwaysTrue(),
            termMap, TermMap.empty(), TermMap.empty());

        application.ctx().checks().addItem(item);
      }
    });
    return button;
  }

  private JPanel createCustomItemsPanel() {
    JPanel panel = new JPanel();
    panel.add(customItemButton("Add Custom Geo", Term.geo(),
        ImmutableSet.of("RandomizerMod.RC.CustomGeoItem", "RandomizerMod")));
    panel.add(customItemButton("Add Custom Essence", Term.essence(),
        ImmutableSet.of("RandomizerCore.LogicItems.SingleItem", "RandomizerCore")));

    return panel;
  }

  private JPanel createActionPanel() {
    JPanel panel = new JPanel();

    JButton applyCosts = new JButton("Apply Costs");
    applyCosts.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        applyCosts();
      }
    });
    panel.add(applyCosts);
    costButtons.add(applyCosts);

    JButton resetCosts = new JButton("Reset Costs");
    resetCosts.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editCheck(checkForEdit);
      }
    });
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

    Costs newCosts = new Costs(costs);
    application.ctx().checks().replace(checkForEdit.id(), checkForEdit.location(),
        checkForEdit.item(), newCosts, false);
    application.refreshLogic(true);
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

        // TODO: Support 'E' here, if the item is placed once.
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
