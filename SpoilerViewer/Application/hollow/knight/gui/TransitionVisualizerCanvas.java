package hollow.knight.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.Sets;
import hollow.knight.gui.TransitionData.GateData;
import hollow.knight.gui.TransitionData.SceneData;
import hollow.knight.logic.ICDLException;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.Term;

public final class TransitionVisualizerCanvas extends JPanel {
  @AutoValue
  abstract static class GatePlacement {
    public abstract ScenePlacement scene();

    public abstract String gateName();

    public final Gate asGate() {
      return Gate.create(scene().scene(), gateName());
    }

    public static GatePlacement create(ScenePlacement scene, String gateName) {
      return new AutoValue_TransitionVisualizerCanvas_GatePlacement(scene, gateName);
    }
  }

  public interface CanvasEnum {
    String displayName();
  }

  public enum EditMode implements CanvasEnum {
    SOURCE_TO_TARGET("Source -> Target"), COUPLED("Coupled");

    private final String displayName;

    private EditMode(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String displayName() {
      return displayName;
    }
  }

  public enum SnapToGrid implements CanvasEnum {
    NONE("No Snapping"), SMALL("Small", 10), MEDIUM("Medium", 25), LARGE("Large", 50);

    private final String displayName;
    private final Optional<Integer> gridSize;

    private SnapToGrid(String displayName, Optional<Integer> gridSize) {
      this.displayName = displayName;
      this.gridSize = gridSize;
    }

    private SnapToGrid(String displayName) {
      this(displayName, Optional.empty());
    }

    private SnapToGrid(String displayName, int gridSize) {
      this(displayName, Optional.of(gridSize));
    }

    @Override
    public String displayName() {
      return displayName;
    }

    private double snapDouble(double in) {
      if (Math.abs(in) < gridSize.get() / 2.0) {
        return 0.0;
      }
      return Math.round(in / gridSize.get()) * gridSize.get();
    }

    public Point snap(Point in) {
      if (!gridSize.isPresent()) {
        return in;
      }
      return new Point(snapDouble(in.x()), snapDouble(in.y()));
    }
  }

  public enum VisibleTransitions implements CanvasEnum {
    ALL("All Transitions"), TOUCH_SELECTED("Touching Selected Scenes Only"), CONTAIN_SELECTED(
        "Contained in Selected Scenes Only");

    private final String displayName;

    private VisibleTransitions(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String displayName() {
      return displayName;
    }
  }

  private static final long serialVersionUID = 1L;

  private final TransitionVisualizer parent;
  private final JLabel statusLabel;

  // TODO: Convert each group here into an individual FSM

  // For highlighting and selecting scenes.
  private Point selectionAnchor = null; // Initial click point
  private Point selectionDrag = null; // Last drag point
  private Point mouseMoved = null; // Last move point
  GatePlacement currentGate = null; // Gate currently selected
  private Set<ScenePlacement> currentSceneSelection = new HashSet<>(); // Scenes currently selected
  GatePlacement highlightGate = null; // Gate highlighted
  private Set<ScenePlacement> highlightedSceneSelection = new HashSet<>(); // Scenes highlighted for
                                                                           // selection
  // on release
  // For dragging the screen or selections
  private AffineTransform dragTransform = null; // Override transform, changed by shifting center
  private Point dragAnchor = null; // Initial click point
  private Point lastDrag = null; // Last point dragged to
  private boolean dragSelection = false; // If true, drag selected scenes. Otherwise, center.

  private Point center = new Point(0, 0);
  private double zoom = 1.0;
  private int zoomPower = 0;
  private Font font = new Font("Arial", Font.BOLD, 14);

  private EditMode editMode = EditMode.COUPLED;
  private SnapToGrid snap = SnapToGrid.NONE;
  private VisibleTransitions visibleTransitions = VisibleTransitions.ALL;

  public TransitionVisualizerCanvas(TransitionVisualizer parent) {
    this.parent = parent;

    this.statusLabel = new JLabel("");
    this.statusLabel.setFont(font);
    this.statusLabel.setMinimumSize(new Dimension(500, 20));
    this.statusLabel.setMaximumSize(new Dimension(500, 20));

    addMouseListener(newMouseListener());
    addMouseMotionListener(newMouseMotionListener());
    addMouseWheelListener(newMouseWheelListener());
    addKeyListener(newKeyListener());

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(newKeyEventDispatcher());
  }

  public JLabel statusLabel() {
    return this.statusLabel;
  }

  public void clear() {
    selectionAnchor = null;
    selectionDrag = null;
    currentGate = null;
    currentSceneSelection.clear();
    highlightGate = null;
    highlightedSceneSelection.clear();
    dragTransform = null;
    dragAnchor = null;
    lastDrag = null;
    dragSelection = false;
    center = new Point(0, 0);
    zoom = 1.0;
    zoomPower = 0;
    updateStatus("");
  }

  public void removeScenePlacement(ScenePlacement placement) {
    currentSceneSelection.remove(placement);
    highlightedSceneSelection.remove(placement);

    if ((currentGate != null && currentGate.scene() == placement)
        || (highlightGate != null && highlightGate.scene() == placement)) {
      currentGate = null;
      highlightGate = null;
    }
    parent.placements().removePlacement(placement);
  }

  public void selectSceneForEdit(String scene) {
    clear();

    parent.placements().placementsForScene(scene).forEach(currentSceneSelection::add);
    if (currentSceneSelection.isEmpty()) {
      fitInternal(parent.placements().allScenePlacements().collect(ImmutableSet.toImmutableSet()));
    } else {
      fitInternal(currentSceneSelection);
    }
    parent.updateChecksList();
  }

  public void editTransition(ItemCheck check) {
    String scene = check.location().scene();
    Optional<ScenePlacement> p = parent.placements().placementsForScene(scene).findAny();

    if (p.isPresent()) {
      clear();

      currentGate = GatePlacement.create(p.get(), Gate.parse(check.location().name()).gateName());
      center = p.get().getTransitionRect(currentGate.gateName(), data()).center();
      requestFocus();
    }
  }

  private boolean canSpan(Rect r) {
    return (getWidth() / zoom) >= r.width() && (getHeight() / zoom) >= r.height();
  }

  private void fitInternal(Set<ScenePlacement> placements) {
    // Find the bounding rect.
    if (placements.isEmpty()) {
      return;
    }

    Rect r = Rect.union(
        placements.stream().map(s -> s.getRect(data())).collect(ImmutableList.toImmutableList()));

    center = r.center();

    // Zoom out until we can see everything.
    while (!canSpan(r)) {
      --zoomPower;
      zoom = Math.pow(ZOOM_SCALE, zoomPower);
    }
  }

  public void fit() {
    clear();
    fitInternal(parent.placements().allScenePlacements().collect(ImmutableSet.toImmutableSet()));
  }

  public Point center() {
    return center;
  }

  private TransitionData data() {
    return parent.transitionData();
  }

  public EditMode editMode() {
    return editMode;
  }

  public void setEditMode(EditMode editMode) {
    this.editMode = editMode;
  }

  public SnapToGrid snap() {
    return snap;
  }

  public void setSnap(SnapToGrid snap) {
    this.snap = snap;
  }

  public VisibleTransitions visibleTransitions() {
    return visibleTransitions;
  }

  public void setVisibleTransitions(VisibleTransitions visibleTransitions) {
    this.visibleTransitions = visibleTransitions;
  }

  public ImmutableSet<String> getSelectedScenes() {
    return currentSceneSelection.stream().map(sp -> sp.scene())
        .collect(ImmutableSet.toImmutableSet());
  }

  public int getFontSize() {
    return font.getSize();
  }

  public void setFontSize(int size) {
    font = new Font("Arial", Font.BOLD, size);
  }

  private void updateStatus(String s) {
    statusLabel.setText(s);
  }

  private void updateStatus(GatePlacement g) {
    SceneData sData = parent.transitionData().sceneData(g.scene().scene());
    GateData gData = sData.getGate(g.gateName());
    updateStatus(String.format("%s[%s] (%s[%s])", sData.alias(), gData.alias(), g.scene().scene(),
        g.gateName()));
  }

  private void updateStatus(Set<ScenePlacement> selection) {
    if (selection.isEmpty()) {
      updateStatus("");
    } else if (selection.size() == 1) {
      ScenePlacement p = selection.iterator().next();
      updateStatus(String.format("%s (%s)", parent.transitionData().sceneData(p.scene()).alias(),
          p.scene()));
    } else {
      updateStatus(selection.size() + " scenes.");
    }
  }

  private void updateSelectedTransitionUnsafe() throws ICDLException {
    Gate source = currentGate.asGate();
    Gate target = highlightGate.asGate();
    currentGate = null;
    highlightGate = null;

    ItemCheck sourceCheck = parent.ctx().checks().getChecksAtLocation(source.termString())
        .collect(MoreCollectors.onlyElement());
    Item targetItem = parent.ctx().checks().getItem(Term.create(target.termString()));
    parent.app().copyItemToCheck(targetItem, sourceCheck);

    if (editMode == EditMode.COUPLED && !source.equals(target) && data().isSource(target)
        && data().isTarget(source)) {
      ItemCheck targetCheck = parent.ctx().checks().getChecksAtLocation(target.termString())
          .collect(MoreCollectors.onlyElement());
      Item sourceItem = parent.ctx().checks().getItem(Term.create(source.termString()));
      parent.app().copyItemToCheck(sourceItem, targetCheck);
    }
  }

  private void updateSelectedTransitionSafe() {
    try {
      updateSelectedTransitionUnsafe();
      updateStatus("");
    } catch (ICDLException ex) {
      GuiUtil.showStackTrace(this, "Error editing transition", ex);
    }
  }

  private void updateHighlightSelectionDrag(Point dragPoint) {
    highlightedSceneSelection.clear();
    highlightGate = null;

    selectionDrag = dragPoint;
    Rect r = Rect.containing(selectionAnchor, selectionDrag);

    if (parent.isICDL()) {
      // Check for an editable transition first.
      for (ScenePlacement p : parent.placements().allScenePlacementsReversed()) {
        SceneData sData = data().sceneData(p.scene());

        for (GateData gate : sData.allGates()) {
          Gate g = Gate.create(p.scene(), gate.name());
          if ((currentGate != null && !data().isTarget(g))
              || (currentGate == null && !data().isSource(g))) {
            continue;
          }

          Rect gr = p.getTransitionRect(gate.name(), data());
          if (gr.contains(r)) {
            // Select the transition.
            highlightGate = GatePlacement.create(p, gate.name());
            currentSceneSelection.clear();
            parent.updateChecksList();
            return;
          }
        }
      }
    }

    if (currentGate != null) {
      // Can't select scenes.
      return;
    }

    // If our first match contains the highlight, select only the top rect.
    // Otherwise, select all intersecting.
    boolean first = true;
    for (ScenePlacement p : parent.placements().allScenePlacementsReversed()) {
      Rect pr = p.getRect(data());
      if (pr.intersects(r)) {
        highlightedSceneSelection.add(p);

        if (first && pr.contains(r)) {
          break;
        }
        first = false;
      }
    }
  }

  private void updateDragAnchor(Point dragPoint) {
    double dx = dragPoint.x() - lastDrag.x();
    double dy = dragPoint.y() - lastDrag.y();
    lastDrag = dragPoint;

    if (dragSelection) {
      currentSceneSelection.forEach(pl -> pl.translate(dx, dy));
    } else {
      center = center.translated(-dx, -dy);
    }
  }

  private AffineTransform transform() {
    AffineTransform tx = AffineTransform.getScaleInstance(zoom, zoom);
    tx.translate(getWidth() / (2 * zoom) - center.x(), getHeight() / (2 * zoom) - center.y());
    return tx;
  }

  private Point invertMouse(java.awt.Point in) {
    Point2D.Double src = new Point2D.Double(in.getX(), in.getY());
    Point2D.Double dst = new Point2D.Double();

    AffineTransform tx = dragTransform != null ? dragTransform : transform();
    try {
      tx.inverseTransform(src, dst);
    } catch (NoninvertibleTransformException ex) {
      throw new AssertionError(ex);
    }

    return new Point(dst.getX(), dst.getY());
  }

  private MouseListener newMouseListener() {
    return new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        mouseMoved = invertMouse(e.getPoint());

        if (selectionAnchor != null) {
          updateHighlightSelectionDrag(mouseMoved);

          if (highlightGate != null) {
            if (currentGate == null) {
              currentGate = highlightGate;
              highlightGate = null;

              updateStatus(currentGate);
            } else {
              // Set transition.
              updateSelectedTransitionSafe();
              updateStatus("");
            }
          } else if (currentGate != null) {
            currentGate = null;
            highlightGate = null;
            updateStatus("");
          } else if (isControlDown && isAltDown) {
            highlightedSceneSelection.forEach(h -> {
              if (currentSceneSelection.contains(h)) {
                currentSceneSelection.remove(h);
              } else {
                currentSceneSelection.add(h);
              }
            });

            updateStatus(currentSceneSelection);
          } else if (isControlDown) {
            currentSceneSelection.addAll(highlightedSceneSelection);
            updateStatus(currentSceneSelection);
          } else if (isAltDown) {
            currentSceneSelection.removeAll(highlightedSceneSelection);
            updateStatus(currentSceneSelection);
          } else {
            currentSceneSelection.clear();
            currentSceneSelection.addAll(highlightedSceneSelection);
            updateStatus(currentSceneSelection);
          }

          highlightedSceneSelection.clear();
          selectionAnchor = null;
          selectionDrag = null;

          parent.updateChecksList();
          repaint();
        } else if (dragAnchor != null) {
          updateDragAnchor(mouseMoved);
          currentSceneSelection.forEach(s -> s.update(snap.snap(s.point())));

          dragTransform = null;
          dragAnchor = null;
          lastDrag = null;
          dragSelection = false;
          repaint();
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        mouseMoved = invertMouse(e.getPoint());
        TransitionVisualizerCanvas.this.requestFocus();

        if (e.getButton() == MouseEvent.BUTTON1) {
          // Are we clicking on any selected scenes?
          boolean onSelected = currentSceneSelection.stream()
              .anyMatch(pl -> pl.getRect(data()).contains(mouseMoved));

          if (onSelected) {
            dragAnchor = mouseMoved;
            lastDrag = mouseMoved;
            dragSelection = true;
          } else {
            selectionAnchor = mouseMoved;
            updateHighlightSelectionDrag(mouseMoved);
          }
          repaint();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
          dragTransform = transform();
          dragAnchor = mouseMoved;
          lastDrag = mouseMoved;
          dragSelection = false;
          repaint();
        }
      }
    };
  }

  private MouseMotionListener newMouseMotionListener() {
    return new MouseMotionAdapter() {
      @Override
      public void mouseMoved(MouseEvent e) {
        mouseMoved = invertMouse(e.getPoint());

        if (currentGate != null) {
          repaint();
        }
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        Point p = invertMouse(e.getPoint());

        if (selectionAnchor != null) {
          updateHighlightSelectionDrag(p);
          repaint();
        } else if (dragAnchor != null) {
          updateDragAnchor(p);
          repaint();
        }
      }
    };
  }

  private final double ZOOM_SCALE = 1.15;

  private MouseWheelListener newMouseWheelListener() {
    return new MouseWheelListener() {
      @Override
      public void mouseWheelMoved(MouseWheelEvent e) {
        zoomPower -= e.getWheelRotation();
        zoom = Math.pow(ZOOM_SCALE, zoomPower);

        repaint();
      }
    };
  }

  private final ImmutableMap<Integer, Integer> X_DIST =
      ImmutableMap.of(KeyEvent.VK_LEFT, -1, KeyEvent.VK_RIGHT, 1);
  private final ImmutableMap<Integer, Integer> Y_DIST =
      ImmutableMap.of(KeyEvent.VK_UP, -1, KeyEvent.VK_DOWN, 1);

  private static final double SCROLL_INCREMENT = 40.0;

  private KeyListener newKeyListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_X) {
          currentSceneSelection.forEach(parent.placements()::removePlacement);
          currentSceneSelection.clear();
          repaint();

          parent.updateScenesList();
          parent.updateChecksList();
          parent.repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          Set<ScenePlacement> newPlacements = new HashSet<>();
          currentSceneSelection.stream()
              .map(p -> parent.placements().addPlacement(p.scene(), p.point().translated(30, 30)))
              .forEach(newPlacements::add);
          currentSceneSelection.clear();
          currentSceneSelection.addAll(newPlacements);
          repaint();

          parent.updateScenesList();
          parent.updateChecksList();
          parent.repaint();
        } else if (X_DIST.containsKey(e.getKeyCode()) || Y_DIST.containsKey(e.getKeyCode())) {
          double dx = SCROLL_INCREMENT * zoom * X_DIST.getOrDefault(e.getKeyCode(), 0);
          double dy = SCROLL_INCREMENT * zoom * Y_DIST.getOrDefault(e.getKeyCode(), 0);
          center = new Point(center.x() + dx, center.y() + dy);
          repaint();
        }
      }
    };
  }

  private boolean isControlDown = false;
  private boolean isAltDown = false;

  private KeyEventDispatcher newKeyEventDispatcher() {
    return new KeyEventDispatcher() {
      @Override
      public boolean dispatchKeyEvent(KeyEvent e) {
        isControlDown = e.isControlDown();
        isAltDown = e.isAltDown();
        return false;
      }
    };
  }

  private static final int FONT_PADDING = 2;

  private int adjustBits(int b, double pct) {
    return b + (int) ((255 - b) * pct);
  }

  private Color adjustSceneColor(ScenePlacement p, Color c) {
    double pct =
        highlightedSceneSelection.contains(p) ? 0.4 : (currentSceneSelection.contains(p) ? 0.2 : 0);
    return new Color(adjustBits(c.getRed(), pct), adjustBits(c.getGreen(), pct),
        adjustBits(c.getBlue(), pct));
  }

  private Color adjustGateColor(String scene, GateData gateData, Color c) {
    // TODO
    return c;
  }

  private float strokeWidth(ScenePlacement p) {
    return highlightedSceneSelection.contains(p) ? 3.0f
        : (currentSceneSelection.contains(p) ? 2.0f : 1.0f);
  }

  private void renderGatePlacement(Graphics2D g2d, ScenePlacement s, GateData g) {
    Rect r = s.getTransitionRect(g.name(), data());
    SceneData sData = data().sceneData(s.scene());

    g2d.setColor(adjustGateColor(s.scene(), g, sData.color()));
    r.fill(g2d);
    g2d.setColor(adjustGateColor(s.scene(), g, sData.edgeColor()));
    g2d.setStroke(new BasicStroke(5.0f));
    r.draw(g2d);
  }

  private static final int TEXT_BUFFER = 20;

  private void renderScenePlacement(Graphics2D g2d, ScenePlacement p) {
    SceneData sData = data().sceneData(p.scene());
    Rect r = p.getRect(data());

    // Draw header.
    String txt = parent.transitionData().sceneData(p.scene()).alias();
    FontMetrics fm = g2d.getFontMetrics(font);
    int fh = fm.getAscent() + fm.getDescent();
    int fw = fm.stringWidth(txt);

    g2d.setFont(font);
    g2d.setColor(Color.black);
    g2d.fillRect((int) r.center().x() - fw / 2 - FONT_PADDING - 1,
        (int) r.y1() - TEXT_BUFFER - fh - FONT_PADDING * 2 - 1, fw + 2 * FONT_PADDING + 1,
        fh + 2 * FONT_PADDING + 1);
    g2d.setColor(Color.white);
    g2d.drawString(txt, (float) (r.center().x() - fw / 2),
        (float) (r.y1() - TEXT_BUFFER - FONT_PADDING - fm.getDescent()));

    g2d.setColor(adjustSceneColor(p, sData.color()));
    r.fill(g2d);
    g2d.setColor(adjustSceneColor(p, sData.edgeColor()));
    g2d.setStroke(new BasicStroke(strokeWidth(p)));
    r.draw(g2d);

    // Render transitions on top.
    sData.allGates().forEach(g -> renderGatePlacement(g2d, p, g));
  }

  private Color sourceTransitionColor(ItemCheck transition, Gate gate) {
    if (transition.vanilla()) {
      return Color.BLUE.brighter();
    } else {
      return Color.GRAY.brighter();
    }
  }

  private static final int MAX_GRADIENTS = 50;
  private static final int GRADIENT_INTERPS = 10;

  private static int interpBits(int b1, double f, int b2) {
    return (int) (b1 + (b2 - b1) * f);
  }

  private static Color interpColor(Color c1, double f, Color c2) {
    return new Color(interpBits(c1.getRed(), f, c2.getRed()),
        interpBits(c1.getGreen(), f, c2.getGreen()), interpBits(c1.getBlue(), f, c2.getBlue()));
  }

  @AutoValue
  abstract static class TransitionToDraw {
    public abstract Point source();

    public abstract Color sourceColor();

    public abstract Point target();

    public abstract Color targetColor();

    public boolean isGradient() {
      return !sourceColor().equals(targetColor());
    }

    private void drawInternal(Graphics2D g2d, Point p1, Point p2) {
      g2d.drawLine((int) p1.x(), (int) p1.y(), (int) p2.x(), (int) p2.y());
    }

    public void draw(Graphics2D g2d, boolean doGradient) {
      if (!isGradient()) {
        g2d.setColor(sourceColor());
        drawInternal(g2d, source(), target());
        return;
      }

      if (doGradient) {
        g2d.setPaint(new GradientPaint((float) source().x(), (float) source().y(), sourceColor(),
            (float) target().x(), (float) target().y(), targetColor()));
        drawInternal(g2d, source(), target());
        return;
      }

      // Do an interp gradient, which is much cheaper.
      for (int i = 0; i < GRADIENT_INTERPS; ++i) {
        Color c = interpColor(sourceColor(), (i + 0.5) / GRADIENT_INTERPS, targetColor());
        g2d.setColor(c);

        Point p1 = Point.interp(source(), i * 1.0 / GRADIENT_INTERPS, target());
        Point p2 = Point.interp(source(), (i + 1.0) / GRADIENT_INTERPS, target());
        drawInternal(g2d, p1, p2);
      }
    }

    public static TransitionToDraw create(Point source, Color sourceColor, Point target,
        Color targetColor) {
      return new AutoValue_TransitionVisualizerCanvas_TransitionToDraw(source, sourceColor, target,
          targetColor);
    }
  }

  private void renderTransitions(Graphics2D g2d) throws ICDLException {
    Set<ItemCheck> transitionsToDraw = new HashSet<>();
    Set<ItemCheck> duplicates = new HashSet<>();
    parent.ctx().checks().allChecks().filter(c -> c.isTransition()).forEach(transitionsToDraw::add);

    List<TransitionToDraw> toDraw = new ArrayList<>();
    for (ItemCheck transition : transitionsToDraw) {
      if (duplicates.contains(transition)) {
        continue;
      }

      Gate source = Gate.parse(transition.location().name());
      Gate target = Gate.parse(transition.item().term().name());

      // Check both are visible.
      ImmutableSet<ScenePlacement> sourcePlacements = parent.placements()
          .placementsForScene(source.sceneName()).collect(ImmutableSet.toImmutableSet());
      ImmutableSet<ScenePlacement> targetPlacements = parent.placements()
          .placementsForScene(target.sceneName()).collect(ImmutableSet.toImmutableSet());
      if (sourcePlacements.isEmpty() || targetPlacements.isEmpty()) {
        continue;
      }

      if (visibleTransitions != VisibleTransitions.ALL) {
        int empty = (Sets.intersection(sourcePlacements, currentSceneSelection).isEmpty() ? 1 : 0)
            + (Sets.intersection(targetPlacements, currentSceneSelection).isEmpty() ? 1 : 0);

        if (visibleTransitions == VisibleTransitions.CONTAIN_SELECTED && empty > 0) {
          continue;
        } else if (empty > 1) {
          continue;
        }
      }

      boolean symmetric = false;
      if (data().isTarget(source) && data().isSource(target)) {
        ItemCheck dupe = parent.ctx().checks().getChecksAtLocation(transition.item().term().name())
            .collect(MoreCollectors.onlyElement());
        if (dupe.item().term().name().equals(transition.location().name())) {
          // This is a symmetric transition
          duplicates.add(dupe);
          symmetric = true;
        }
      }

      for (ScenePlacement s : sourcePlacements) {
        for (ScenePlacement t : targetPlacements) {
          Rect r1 = s.getTransitionRect(source.gateName(), data());
          Rect r2 = t.getTransitionRect(target.gateName(), data());

          toDraw.add(TransitionToDraw.create(r1.center(), sourceTransitionColor(transition, source),
              r2.center(),
              symmetric ? sourceTransitionColor(transition, source) : Color.red.darker()));
        }
      }
    }

    g2d.setStroke(new BasicStroke(5.0f));
    boolean doGradient = toDraw.stream().filter(t -> t.isGradient()).count() <= MAX_GRADIENTS;
    toDraw.forEach(t -> t.draw(g2d, doGradient));
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(400, 400);
  }

  private Rect getViewportRect() {
    return new Rect(center, getWidth() / zoom, getHeight() / zoom);
  }

  private static final int EXPORT_PADDING = 50;

  private Rect getCanvasRect() {
    if (parent.placements().allScenePlacements().count() == 0) {
      return new Rect(new Point(0, 0), 100, 100);
    }

    Rect bound = Rect.union(parent.placements().allScenePlacements().map(s -> s.getRect(data()))
        .collect(ImmutableList.toImmutableList()));
    return new Rect(bound.center(), bound.width() + EXPORT_PADDING * 2,
        bound.height() + EXPORT_PADDING * 2);
  }

  private static final FileFilter PNG_FILTER = new FileFilter() {
    @Override
    public boolean accept(File pathname) {
      return pathname.isDirectory() || pathname.getName().endsWith(".png");
    }

    @Override
    public String getDescription() {
      return "PNG Images";
    }
  };

  public void exportImage(boolean viewport) {
    Rect viewRect = viewport ? getViewportRect() : getCanvasRect();

    BufferedImage img = new BufferedImage((int) viewRect.width(), (int) viewRect.height(),
        BufferedImage.TYPE_3BYTE_BGR);
    Graphics2D g2d = img.createGraphics();
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    g2d.setColor(Color.black);
    g2d.fillRect(-1, -1, (int) viewRect.width() + 2, (int) viewRect.height() + 2);
    g2d.transform(AffineTransform.getTranslateInstance(viewRect.width() / 2 - viewRect.center().x(),
        viewRect.height() / 2 - viewRect.center().y()));
    paintInternal(g2d);

    JFileChooser c = new JFileChooser("Save As");
    c.setFileFilter(PNG_FILTER);
    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    File f = c.getSelectedFile();
    if (!f.getName().endsWith(".png")) {
      f = new File(f.getParentFile(), f.getName() + ".png");
    }

    try {
      ImageIO.write(img, "png", f);
    } catch (Exception ex) {
      GuiUtil.showStackTrace(parent, "Failed to Export", ex);
    }
  }

  private void paintInternal(Graphics2D g2d) {
    try {
      // Draw components in order.
      parent.placements().allScenePlacements().forEach(p -> renderScenePlacement(g2d, p));

      // Draw visible transitions.
      renderTransitions(g2d);

      // Draw selection rect.
      if (selectionAnchor != null) {
        Rect r = Rect.containing(selectionAnchor, selectionDrag);
        g2d.setColor(Color.white);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        r.fill(g2d);
        g2d.fillRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
        g2d.setStroke(new BasicStroke(0.7f));
        g2d.setComposite(AlphaComposite.Src);
        r.draw(g2d);
      }
      if (currentGate != null) {
        Point start =
            currentGate.scene().getTransitionRect(currentGate.gateName(), data()).center();
        Point end = mouseMoved;

        // TODO: Adjust color based on target eligibility
        g2d.setStroke(new BasicStroke(7.5f));
        g2d.setColor(Color.GREEN.brighter());
        g2d.drawLine((int) start.x(), (int) start.y(), (int) end.x(), (int) end.y());
      }
    } catch (ICDLException ex) {
      throw new AssertionError(ex);
    }
  }

  @Override
  public void paintComponent(Graphics g) {
    Graphics2D g2d = (Graphics2D) g;
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    Rectangle bounds = g2d.getClipBounds();
    g2d.setColor(Color.BLACK);
    g2d.fillRect(bounds.x - 1, bounds.y - 1, bounds.width + 1, bounds.height + 1);

    // Apply affine transform.
    AffineTransform prev = g2d.getTransform();
    g2d.transform(transform());
    try {
      paintInternal(g2d);
    } finally {
      g2d.setTransform(prev);
    }
  }
}
