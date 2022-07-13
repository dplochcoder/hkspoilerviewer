package hollow.knight.gui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
import java.util.Set;
import javax.swing.JPanel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import hollow.knight.gui.TransitionData.SceneData;
import hollow.knight.gui.TransitionVisualizerPlacements.Placement;

public final class TransitionVisualizerCanvas extends JPanel {
  @AutoValue
  abstract static class TransitionPlacement {
    public abstract Placement scenePlacement();

    public abstract String gate();

    public static TransitionPlacement create(Placement placement, String gate) {
      return new AutoValue_TransitionVisualizerCanvas_TransitionPlacement(placement, gate);
    }
  }

  private static final long serialVersionUID = 1L;

  private final TransitionVisualizer parent;

  // TODO: Convert each group here into an individual FSM

  // For highlighting and selecting scenes.
  private Point selectionAnchor = null; // Initial click point
  private Point selectionDrag = null; // Last drag point
  private Set<Placement> currentSelection = new HashSet<>(); // Scenes currently selected
  private Set<Placement> highlightedSelection = new HashSet<>(); // Scenes highlighted for selection
                                                                 // on release
  // For dragging the screen or selections
  private AffineTransform dragTransform = null; // Override transform, changed by shifting center
  private Point dragAnchor = null; // Initial click point
  private Point lastDrag = null; // Last point dragged to
  private boolean dragSelection = false; // If true, drag selected scenes. Otherwise, center.

  private Point center = new Point(0, 0);
  private double zoom = 1.0;
  private int zoomPower = 0;
  private Font font = new Font("Arail", Font.BOLD, 14);

  public TransitionVisualizerCanvas(TransitionVisualizer parent) {
    this.parent = parent;

    addMouseListener(newMouseListener());
    addMouseMotionListener(newMouseMotionListener());
    addMouseWheelListener(newMouseWheelListener());
    addKeyListener(newKeyListener());
  }

  public Point center() {
    return center;
  }

  private TransitionData data() {
    return parent.transitionData();
  }

  private void updateHighlightSelectionDrag(Point dragPoint) {
    selectionDrag = dragPoint;
    Rect r = Rect.containing(selectionAnchor, selectionDrag);

    // If our first match contains the highlight, select only the top rect.
    // Otherwise, select all intersecting.
    highlightedSelection.clear();
    boolean first = true;
    for (Placement p : parent.placements().allPlacementsReversed()) {
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

          // TODO: Support ctrl+add
          currentSelection.clear();
          currentSelection.addAll(highlightedSelection);
          highlightedSelection.clear();
          selectionAnchor = null;
          selectionDrag = null;
          repaint();
        } else if (dragAnchor != null) {
          updateDragAnchor(p);

          dragTransform = null;
          dragAnchor = null;
          lastDrag = null;
          dragSelection = false;
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

  private final ImmutableMap<Integer, Integer> X_DIST = ImmutableMap.of(KeyEvent.VK_LEFT, -1,
      KeyEvent.VK_A, -1, KeyEvent.VK_RIGHT, 1, KeyEvent.VK_D, 1);
  private final ImmutableMap<Integer, Integer> Y_DIST =
      ImmutableMap.of(KeyEvent.VK_UP, -1, KeyEvent.VK_W, -1, KeyEvent.VK_DOWN, 1, KeyEvent.VK_S, 1);

  private static final double SCROLL_INCREMENT = 40.0;

  private KeyListener newKeyListener() {
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (X_DIST.containsKey(e.getKeyCode()) || Y_DIST.containsKey(e.getKeyCode())) {
          double dx = SCROLL_INCREMENT * zoom * X_DIST.getOrDefault(e.getKeyCode(), 0);
          double dy = SCROLL_INCREMENT * zoom * Y_DIST.getOrDefault(e.getKeyCode(), 0);
          center = new Point(center.x() + dx, center.y() + dy);
          repaint();
        }
      }
    };
  }

  private static final int FONT_PADDING = 2;

  private int adjustBits(int b, double pct) {
    return b + (int) ((255 - b) * pct);
  }

  private Color adjustColor(Placement p, Color c) {
    double pct = highlightedSelection.contains(p) ? 0.4 : (currentSelection.contains(p) ? 0.2 : 0);
    return new Color(adjustBits(c.getRed(), pct), adjustBits(c.getGreen(), pct),
        adjustBits(c.getBlue(), pct));
  }

  private float strokeWidth(Placement p) {
    return highlightedSelection.contains(p) ? 3.0f : (currentSelection.contains(p) ? 2.0f : 1.0f);
  }

  private void renderPlacement(Graphics2D g2d, TransitionVisualizerPlacements.Placement p) {
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
        (int) r.y1() - fh - FONT_PADDING * 2 - 1, fw + 2 * FONT_PADDING + 1,
        fh + 2 * FONT_PADDING + 1);
    g2d.setColor(Color.white);
    g2d.drawString(txt, (float) (r.center().x() - fw / 2),
        (float) (r.y1() - FONT_PADDING - fm.getDescent()));

    g2d.setColor(adjustColor(p, sData.color()));
    g2d.fillRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
    g2d.setColor(adjustColor(p, sData.edgeColor()));
    g2d.setStroke(new BasicStroke(strokeWidth(p)));
    g2d.drawRect((int) r.x1(), (int) r.y1(), (int) r.width(), (int) r.height());
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
      parent.placements().allPlacements().forEach(p -> renderPlacement(g2d, p));

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
    } finally {
      g2d.setTransform(prev);
    }
  }
}
