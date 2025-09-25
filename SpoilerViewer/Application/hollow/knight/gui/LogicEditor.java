package hollow.knight.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

// Free-floating UI for editing a single check.
public final class LogicEditor extends JFrame implements SingletonWindow.Interface {
  private static final long serialVersionUID = 1L;

  private final Application application;

  private final LogicEditorSearchField logicEditorSearchField;
  private final LogicEditorListModel logicEditorListModel;
  private final JList<String> logicList;
  private final JScrollPane logicPane;

  private String logicForEdit;
  private final JLabel nameLabel;
  private final JTextArea editText;
  private final JScrollPane editScrollPane;

  public LogicEditor(Application application) {
    super("ICDL Logic Editor");

    this.application = application;

    this.logicEditorListModel = new LogicEditorListModel();

    this.logicEditorSearchField = new LogicEditorSearchField();
    this.logicEditorSearchField.addListener(() -> repopulateLogicResults());
    this.logicList = createLogicList();
    this.logicPane = new JScrollPane(logicList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    logicPane.setMinimumSize(new Dimension(300, 300));

    this.nameLabel = new JLabel("");
    this.editText = new JTextArea("");
    editText.setLineWrap(true);

    KeyStroke ctrlS = KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK);
    editText.getInputMap(JComponent.WHEN_FOCUSED).put(ctrlS, "saveLogic");
    editText.getActionMap().put("saveDocument", new AbstractAction() {
      private static final long serialVersionUID = 1L;

      @Override
      public void actionPerformed(ActionEvent arg0) {
        saveLogic();
      }
    });

    this.editScrollPane = new JScrollPane(editText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    JPanel contentPane = new JPanel();
    contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.PAGE_AXIS));
    logicEditorSearchField.addToGui(contentPane);
    contentPane.add(logicPane);
    contentPane.add(createManageWaypointsPanel());
    contentPane.add(new JSeparator());
    contentPane.add(nameLabel);
    contentPane.add(editScrollPane);
    contentPane.add(new JSeparator());
    contentPane.add(createManageLogicPanel());
    contentPane.setAlignmentX(Component.LEFT_ALIGNMENT);
    getContentPane().add(contentPane);

    repopulateLogicResults();
    editLogic(null);
    setVisible(true);
  }

  public void editLogic(String name) {
    if (name == null) {
      logicForEdit = null;
      nameLabel.setText("Name: n/a");
      editText.setText("");
      editText.setEditable(false);
      return;
    }

    logicForEdit = name;
    nameLabel.setText("Name: " + name);
    editText.setText(application.ctx().logicEdits().getLogic(application.ctx(), name));
    editText.setEditable(true);
  }

  private JList<String> createLogicList() {
    JList<String> itemsList = new JList<String>(logicEditorListModel);
    itemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(itemsList.getKeyListeners()).forEach(itemsList::removeKeyListener);
    itemsList.addKeyListener(newItemsListKeyListener());

    return itemsList;
  }

  private JButton newWaypointButton() {
    JButton button = new JButton("New Waypoint");
    button.addActionListener(GuiUtil.newActionListener(this, () -> {
      String resp = JOptionPane.showInputDialog(this, "Waypoint Name");
      if (resp == null || resp.trim().isEmpty()) {
        return;
      }

      application.ctx().logicEdits().addWaypoint(application.ctx(), resp);
      repopulateLogicResults();
      editLogic(resp);
    }));
    return button;
  }

  private JButton revertLogicButton() {
    JButton button = new JButton("Revert Logic");
    button.addActionListener(GuiUtil.newActionListener(this, () -> {
      if (logicForEdit == null) {
        return;
      }

      application.ctx().logicEdits().clearLogic(logicForEdit);
      repopulateLogicResults();
      editLogic(null);
    }));
    return button;
  }

  private JPanel createManageWaypointsPanel() {
    JPanel panel = new JPanel();
    panel.add(newWaypointButton());
    panel.add(revertLogicButton());
    return panel;
  }

  private void saveLogic() {
    if (logicForEdit == null) {
      return;
    }

    application.ctx().logicEdits().saveLogic(application.ctx(), logicForEdit,
        editText.getText().trim());
    repopulateLogicResults();
  }

  private JButton saveLogicButton() {
    JButton button = new JButton("Save");
    button.addActionListener(GuiUtil.newActionListener(this, this::saveLogic));
    return button;
  }

  private JButton undoLogicButton() {
    JButton button = new JButton("Revert");
    button.addActionListener(GuiUtil.newActionListener(this, () -> {
      if (logicForEdit == null) {
        return;
      }

      editLogic(logicForEdit);
    }));
    return button;
  }

  private JPanel createManageLogicPanel() {
    JPanel panel = new JPanel();
    panel.add(saveLogicButton());
    panel.add(undoLogicButton());
    return panel;
  }

  private static boolean needsExpansion(JScrollPane pane) {
    return pane.getPreferredSize().width > pane.getSize().width;
  }

  public void repopulateLogicResults() {
    ImmutableList<String> results = application.ctx().logicEdits().allLogicNames(application.ctx())
        .stream().filter(logicEditorSearchField::accept).collect(ImmutableList.toImmutableList());
    logicEditorListModel.updateResults(application.ctx(), results);

    if (needsExpansion(logicPane)) {
      pack();
    }
    repaint();
  }

  public String selectedName() {
    return logicEditorListModel.get(logicList.getSelectedIndex());
  }

  private static final ImmutableMap<Integer, Integer> UP_DOWN_VALUES = ImmutableMap.of(
      KeyEvent.VK_UP, -1, KeyEvent.VK_DOWN, 1, KeyEvent.VK_PAGE_UP, -5, KeyEvent.VK_PAGE_DOWN, 5);

  private KeyListener newItemsListKeyListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        // TODO: Make key codes configurable.
        if (e.getKeyCode() != KeyEvent.VK_Q && e.getKeyCode() != KeyEvent.VK_E
            && !UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_Q) {
          editLogic(null);
        } else if (e.getKeyCode() == KeyEvent.VK_E) {
          editLogic(selectedName());
        } else {
          // Navigate up or down.
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = logicList.getSelectedIndex() + delta;
          if (newIndex < 0) {
            newIndex = 0;
          } else if (newIndex >= logicEditorListModel.getSize()) {
            newIndex = logicEditorListModel.getSize() - 1;
          }

          logicList.setSelectedIndex(newIndex);
        }
      }
    };
  }
}
