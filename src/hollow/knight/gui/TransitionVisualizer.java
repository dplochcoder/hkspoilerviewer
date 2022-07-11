package hollow.knight.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JFrame;

public final class TransitionVisualizer extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Application application;

  public TransitionVisualizer(Application application) {
    super("Transition Visualizer");

    this.application = application;

    this.addWindowListener(newWindowListener());

    pack();
    setVisible(true);
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
