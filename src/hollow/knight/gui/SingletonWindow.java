package hollow.knight.gui;

import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import hollow.knight.gui.SingletonWindow.Interface;

public final class SingletonWindow<T extends JFrame & Interface> {
  public interface Interface {
    default void onClose() {}
  }

  private final Supplier<T> newWindow;
  private T window;

  private final JMenuItem menuItem;

  public SingletonWindow(Component parent, String name, Supplier<T> newWindow) {
    this.newWindow = newWindow;

    this.menuItem = new JMenuItem("Open " + name);
    menuItem.setEnabled(true);
    menuItem.setToolTipText("");
    menuItem.addActionListener(GuiUtil.newActionListener(parent, () -> {
      get();
      menuItem.setEnabled(false);
      menuItem.setToolTipText(name + " is already open");
    }));
  }

  public JMenuItem getMenuItem() {
    return menuItem;
  }

  public boolean isOpen() {
    return window != null;
  }

  public void ifOpen(Consumer<T> ifOpen) {
    if (window != null) {
      ifOpen.accept(window);
    }
  }

  public T get() {
    if (window == null) {
      window = newWindow.get();
      window.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent e) {
          window.onClose();
          window = null;

          menuItem.setEnabled(true);
          menuItem.setToolTipText("");
        }
      });
    }

    return window;
  }

  public T getWithFocus() {
    T w = get();
    w.requestFocus();
    return w;
  }

  public void close() {
    if (window != null) {
      window.dispose();
    }
  }
}
