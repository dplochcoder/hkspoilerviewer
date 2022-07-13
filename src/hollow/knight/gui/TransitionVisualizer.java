package hollow.knight.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import hollow.knight.gui.TransitionData.SceneData;
import hollow.knight.gui.TransitionVisualizerCanvas.CanvasEnum;
import hollow.knight.gui.TransitionVisualizerCanvas.EditMode;
import hollow.knight.gui.TransitionVisualizerCanvas.SnapToGrid;
import hollow.knight.gui.TransitionVisualizerCanvas.VisibleTransitions;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.StateContext;

public final class TransitionVisualizer extends JFrame {
  private static final long serialVersionUID = 1L;


  private final Application application;

  private final TransitionVisualizerCanvas canvas;

  private final JTextField scenesFilter;
  private final SceneSelectorListModel scenesListModel;
  private final JList<String> scenesList;
  private final JScrollPane scenesPane;

  public TransitionVisualizer(Application application) {
    super("Transition Visualizer");

    this.application = application;

    this.canvas = new TransitionVisualizerCanvas(this);

    this.scenesListModel = new SceneSelectorListModel(application.transitionData(),
        application.transitionVisualizerPlacements());
    this.scenesFilter = createScenesFilter();
    this.scenesList = createScenesList();
    this.scenesPane = new JScrollPane(scenesList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    scenesPane.setMinimumSize(new Dimension(300, 600));

    setJMenuBar(createMenu());

    JPanel rightPane = new JPanel();
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.PAGE_AXIS));
    rightPane.add(scenesFilter);
    rightPane.add(scenesPane);

    this.addWindowListener(newWindowListener());

    getContentPane().add(canvas, BorderLayout.CENTER);
    getContentPane().add(rightPane, BorderLayout.EAST);

    updateScenesList();
    pack();
    setVisible(true);
  }

  public Application app() {
    return application;
  }

  public StateContext ctx() {
    return application.ctx();
  }

  public boolean isICDL() {
    return application.isICDL();
  }

  public TransitionData transitionData() {
    return application.transitionData();
  }

  public TransitionVisualizerPlacements placements() {
    return application.transitionVisualizerPlacements();
  }

  private JTextField createScenesFilter() {
    JTextField field = new JTextField(16);
    field.getDocument().addDocumentListener(GuiUtil.newDocumentListener(this::updateScenesList));
    field.setMaximumSize(new Dimension(300, 60));
    return field;
  }

  private JList<String> createScenesList() {
    JList<String> scenesList = new JList<String>(scenesListModel);
    scenesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(scenesList.getKeyListeners()).forEach(scenesList::removeKeyListener);
    scenesList.addKeyListener(scenesListKeyListener());
    return scenesList;
  }

  private static final ImmutableMap<Integer, Integer> UP_DOWN_VALUES = ImmutableMap.of(
      KeyEvent.VK_UP, -1, KeyEvent.VK_DOWN, 1, KeyEvent.VK_PAGE_UP, -10, KeyEvent.VK_PAGE_DOWN, 10);

  private KeyListener scenesListKeyListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (scenesList.getSelectedIndex() == -1) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          // Spawn selected scene.
          String scene = scenesListModel.getScene(scenesList.getSelectedIndex());

          application.transitionVisualizerPlacements().addPlacement(scene, canvas.center());

          try {
            application.transitionData().refresh(application.ctx().roomLabels());
          } catch (Exception ex) {
            System.err.println("Whoops: " + ex);
          }

          updateScenesList();
          repaint();
          e.consume();
        } else if (UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = scenesList.getSelectedIndex() + delta;
          if (newIndex < 0) {
            newIndex = 0;
          }
          if (newIndex > scenesListModel.getSize() - 1) {
            newIndex = scenesListModel.getSize() - 1;
          }
          scenesList.setSelectedIndex(newIndex);
          e.consume();
        }
      }
    };

  }

  private JMenuItem createViewResetMenu() {
    JMenuItem out = new JMenuItem("Reset to Origin");
    out.addActionListener(GuiUtil.newActionListener(this, () -> {
      canvas.clear();
      repaint();
    }));
    return out;
  }

  private JMenuItem createViewFitMenu() {
    JMenuItem out = new JMenuItem("Fit");
    out.addActionListener(GuiUtil.newActionListener(this, () -> {
      canvas.fit();
      repaint();
    }));
    return out;
  }

  private JMenuItem createFontSizeMenu() {
    JMenuItem fSize = new JMenuItem("Font Size");
    fSize.addActionListener(GuiUtil.newActionListener(this, () -> {
      String out = JOptionPane.showInputDialog(this, "Enter new font size",
          String.valueOf(canvas.getFontSize()));
      if (out == null || out.trim().isEmpty()) {
        return;
      }

      Integer s = Ints.tryParse(out.trim());
      if (s == null || s < 1) {
        JOptionPane.showMessageDialog(this, "Must input a positive integer", "Bad Font Size",
            JOptionPane.WARNING_MESSAGE);
        return;
      }

      canvas.setFontSize(s);
      repaint();
    }));

    return fSize;
  }


  private <E extends Enum<E> & CanvasEnum> JMenu createCanvasEnumMenu(Class<E> clazz, String name,
      Supplier<E> getter, Consumer<E> setter) {
    JMenu out = new JMenu(name);

    ButtonGroup g = new ButtonGroup();
    for (E value : EnumSet.allOf(clazz)) {
      JRadioButton b = new JRadioButton(value.displayName());
      b.setSelected(value == getter.get());
      b.addActionListener(GuiUtil.newActionListener(this, () -> {
        setter.accept(value);
        repaint();
      }));

      g.add(b);
      out.add(b);
    }

    return out;
  }

  private JMenuItem createVanillaPlacementsMenu() {
    JMenuItem out = new JMenuItem("Vanilla Scene Placements");
    out.addActionListener(GuiUtil.newActionListener(this, () -> {
      placements().clear();
      canvas.clear();
      transitionData().scenes().stream().forEach(scene -> {
        SceneData sData = transitionData().sceneData(scene);
        Optional<Point> p = sData.vanillaPlacement();
        if (p.isPresent()) {
          placements().addPlacement(scene, p.get());
        }

        updateScenesList();
        repaint();
      });
    }));
    return out;
  }

  private JMenuBar createMenu() {
    JMenuBar menu = new JMenuBar();

    JMenu view = new JMenu("View");
    view.add(createViewResetMenu());
    view.add(createViewFitMenu());
    view.add(new JSeparator());
    view.add(createFontSizeMenu());
    view.add(new JSeparator());
    view.add(createCanvasEnumMenu(SnapToGrid.class, "Snap to Grid", canvas::snap, canvas::setSnap));
    view.add(new JSeparator());
    view.add(createCanvasEnumMenu(VisibleTransitions.class, "Transitions",
        canvas::visibleTransitions, canvas::setVisibleTransitions));
    view.add(new JSeparator());
    view.add(createVanillaPlacementsMenu());
    menu.add(view);

    JMenu edit = new JMenu("Edit");
    if (application.isICDL()) {
      edit.add(createCanvasEnumMenu(EditMode.class, "Transition Mode", canvas::editMode,
          canvas::setEditMode));
    } else {
      edit.setEnabled(false);
      edit.setToolTipText("Must open an ICDL ctx.json for editing");
    }
    menu.add(edit);

    return menu;
  }

  private boolean matches(ImmutableList<String> searchTokens, String scene) {
    if (searchTokens.isEmpty()) {
      return true;
    }

    List<String> names = new ArrayList<>();
    names.add(scene);

    SceneData sData = transitionData().sceneData(scene);
    names.add(sData.alias());
    RoomLabels rooms = application.ctx().roomLabels();
    for (RoomLabels.Type type : RoomLabels.Type.values()) {
      names.add(rooms.get(scene, type));
    }

    return searchTokens.stream()
        .allMatch(t -> names.stream().anyMatch(n -> n.toLowerCase().contains(t)));
  }

  public void updateScenesList() {
    ImmutableList<String> tokens =
        Arrays.stream(scenesFilter.getText().split(" ")).map(s -> s.toLowerCase().trim())
            .filter(s -> !s.isEmpty()).collect(ImmutableList.toImmutableList());

    List<String> newScenes = transitionData().scenes().stream().filter(s -> matches(tokens, s))
        .sorted(Comparator.comparing(s -> transitionData().sceneData(s).alias()))
        .collect(ImmutableList.toImmutableList());
    scenesListModel.updateScenes(newScenes);
  }

  private WindowListener newWindowListener() {
    return new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        application.transitionVisualizerClosed();
      }
    };
  }

}
