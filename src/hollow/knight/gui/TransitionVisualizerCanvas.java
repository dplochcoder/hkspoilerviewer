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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import javax.swing.JPanel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import hollow.knight.gui.TransitionData.GateData;
import hollow.knight.gui.TransitionData.SceneData;
import hollow.knight.logic.ICDLException;
import hollow.knight.logic.ItemCheck;

public final class TransitionVisualizerCanvas extends JPanel {
  public interface CanvasEnum {
    String displayName();
  }

  public enum EditMode implements CanvasEnum {
    SOURCE_TO_TARGET("Source -> Target"), TARGET_TO_SOURCE("Target -> Source"), COUPLED("Coupled");

    private final String displayName;

    EditMode(String displayName) {
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

    SnapToGrid(String displayName, Optional<Integer> gridSize) {
      this.displayName = displayName;
      this.gridSize = gridSize;
    }

    SnapToGrid(String displayName) {
      this(displayName, Optional.empty());
    }

    SnapToGrid(String displayName, int gridSize) {
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

  private static final long serialVersionUID = 1L;

  private final TransitionVisualizer parent;

  // TODO: Convert each group here into an individual FSM

  // For highlighting and selecting scenes.
  private Point selectionAnchor = null; // Initial click point
  private Point selectionDrag = null; // Last drag point
  private Set<ScenePlacement> currentSelection = new HashSet<>(); // Scenes currently selected
  private Set<ScenePlacement> highlightedSelection = new HashSet<>(); // Scenes highlighted for
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

  public TransitionVisualizerCanvas(TransitionVisualizer parent) {
    this.parent = parent;

    addMouseListener(newMouseListener());
    addMouseMotionListener(newMouseMotionListener());
    addMouseWheelListener(newMouseWheelListener());
    addKeyListener(newKeyListener());

    KeyboardFocusManager.getCurrentKeyboardFocusManager()
        .addKeyEventDispatcher(newKeyEventDispatcher());
  }

  public void clear() {
    selectionAnchor = null;
    selectionDrag = null;
    currentSelection.clear();
    highlightedSelection.clear();
    dragTransform = null;
    dragAnchor = null;
    lastDrag = null;
    dragSelection = false;
    center = new Point(0, 0);
    zoom = 1.0;
    zoomPower = 0;
  }

  private boolean canSpan(Rect r) {
    return (getWidth() / zoom) >= r.width() && (getHeight() / zoom) >= r.height();
  }

  public void fit() {
    clear();

    // Find the bounding rect.
    if (parent.placements().isEmpty()) {
      return;
    }

    Rect r = Rect.union(parent.placements().allScenePlacements().map(s -> s.getRect(data()))
        .collect(ImmutableList.toImmutableList()));

    center = r.center();

    // Zoom out until we can see everything.
    while (!canSpan(r)) {
      --zoomPower;
      zoom = Math.pow(ZOOM_SCALE, zoomPower);
    }
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

  public int getFontSize() {
    return font.getSize();
  }

  public void setFontSize(int size) {
    font = new Font("Arial", Font.BOLD, size);
  }

  private void updateHighlightSelectionDrag(Point dragPoint) {
    selectionDrag = dragPoint;
    Rect r = Rect.containing(selectionAnchor, selectionDrag);

    // If our first match contains the highlight, select only the top rect.
    // Otherwise, select all intersecting.
    highlightedSelection.clear();
    boolean first = true;
    for (ScenePlacement p : parent.placements().allScenePlacementsReversed()) {
      Rect pr = p.getRect(data());
      if (pr.intersects(r)) {
        highlightedSelection.add(p);

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
      currentSelection.forEach(pl -> pl.translate(dx, dy));
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
        Point p = invertMouse(e.getPoint());

        if (selectionAnchor != null) {
          updateHighlightSelectionDrag(p);

          if (isControlDown && isAltDown) {
            highlightedSelection.forEach(h -> {
              if (currentSelection.contains(h)) {
                currentSelection.remove(h);
              } else {
                currentSelection.add(h);
              }
            });
          } else if (isControlDown) {
            currentSelection.addAll(highlightedSelection);
          } else if (isAltDown) {
            currentSelection.removeAll(highlightedSelection);
          } else {
            currentSelection.clear();
            currentSelection.addAll(highlightedSelection);
          }

          highlightedSelection.clear();
          selectionAnchor = null;
          selectionDrag = null;
          repaint();
        } else if (dragAnchor != null) {
          updateDragAnchor(p);
          currentSelection.forEach(s -> s.update(snap.snap(s.point())));

          dragTransform = null;
          dragAnchor = null;
          lastDrag = null;
          dragSelection = false;
          repaint();
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        TransitionVisualizerCanvas.this.requestFocus();

        Point p = invertMouse(e.getPoint());
        if (e.getButton() == MouseEvent.BUTTON1) {
          // Are we clicking on any selected scenes?
          boolean onSelected =
              currentSelection.stream().anyMatch(pl -> pl.getRect(data()).contains(p));

          if (onSelected) {
            dragAnchor = p;
            lastDrag = p;
            dragSelection = true;
          } else {
            selectionAnchor = p;
            updateHighlightSelectionDrag(p);
          }
          repaint();
        } else if (e.getButton() == MouseEvent.BUTTON3) {
          dragTransform = transform();
          dragAnchor = p;
          lastDrag = p;
          dragSelection = false;
          repaint();
        }
      }
    };
  }

  private MouseMotionListener newMouseMotionListener() {
    return new MouseMotionAdapter() {
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
          currentSelection.forEach(parent.placements()::removePlacement);
          repaint();

          parent.updateScenesList();
          parent.repaint();
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          Set<ScenePlacement> newPlacements = new HashSet<>();
          currentSelection.stream()
              .map(p -> parent.placements().addPlacement(p.scene(), p.point().translated(30, 30)))
              .forEach(newPlacements::add);
          currentSelection.clear();
          currentSelection.addAll(newPlacements);
          repaint();

          parent.updateScenesList();
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
    double pct = highlightedSelection.contains(p) ? 0.4 : (currentSelection.contains(p) ? 0.2 : 0);
    return new Color(adjustBits(c.getRed(), pct), adjustBits(c.getGreen(), pct),
        adjustBits(c.getBlue(), pct));
  }

  private Color adjustGateColor(String scene, GateData gateData, Color c) {
    // TODO
    return c;
  }

  private float strokeWidth(ScenePlacement p) {
    return highlightedSelection.contains(p) ? 3.0f : (currentSelection.contains(p) ? 2.0f : 1.0f);
  }

  private void renderGatePlacement(Graphics2D g2d, ScenePlacement s, GateData g) {
    Rect r = s.getTransitionRect(g.name(), data());
    SceneData sData = data().sceneData(s.scene());

    g2d.setColor(adjustGateColor(s.scene(), g, sData.color()));
    g2d.fillRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
    g2d.setColor(adjustGateColor(s.scene(), g, sData.edgeColor()));
    g2d.setStroke(new BasicStroke(5.0f));
    g2d.drawRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
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
    g2d.fillRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
    g2d.setColor(adjustSceneColor(p, sData.edgeColor()));
    g2d.setStroke(new BasicStroke(strokeWidth(p)));
    g2d.drawRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());

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

  private Color targetTransitionColor(ItemCheck transition, Gate gate) {
    if (!data().isSource(gate)) {
      return Color.RED.darker();
    } else if (transition.vanilla()) {
      return Color.BLUE.brighter();
    } else {
      return Color.GRAY.brighter();
    }
  }

  private void renderTransitions(Graphics2D g2d) throws ICDLException {
    Set<ItemCheck> transitionsToDraw = new HashSet<>();
    Set<ItemCheck> duplicates = new HashSet<>();
    parent.ctx().checks().allChecks().filter(c -> c.isTransition()).forEach(transitionsToDraw::add);

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

      g2d.setStroke(new BasicStroke(5.0f));
      for (ScenePlacement s : sourcePlacements) {
        for (ScenePlacement t : targetPlacements) {
          Rect r1 = s.getTransitionRect(source.gateName(), data());
          Rect r2 = t.getTransitionRect(target.gateName(), data());

          if (symmetric) {
            g2d.setColor(sourceTransitionColor(transition, source));
          } else {
            g2d.setPaint(new GradientPaint((float) r1.center().x(), (float) r1.center().y(),
                sourceTransitionColor(transition, source), (float) r2.center().x(),
                (float) r2.center().y(), targetTransitionColor(transition, target)));
          }
          g2d.drawLine((int) r1.center().x(), (int) r1.center().y(), (int) r2.center().x(),
              (int) r2.center().y());
        }
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(400, 400);
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
      // Draw components in order.
      parent.placements().allScenePlacements().forEach(p -> renderScenePlacement(g2d, p));

      // Draw visible transitions.
      renderTransitions(g2d);

      // Draw selection rect.
      if (selectionAnchor != null) {
        Rect r = Rect.containing(selectionAnchor, selectionDrag);
        g2d.setColor(Color.white);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f));
        g2d.fillRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
        g2d.setStroke(new BasicStroke(0.7f));
        g2d.setComposite(AlphaComposite.Src);
        g2d.drawRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
      }
    } catch (ICDLException ex) {
      throw new AssertionError(ex);
    } finally {
      g2d.setTransform(prev);
    }
  }
}
