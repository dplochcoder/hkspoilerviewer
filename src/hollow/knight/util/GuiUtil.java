package hollow.knight.util;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class GuiUtil {

  @FunctionalInterface
  public interface Action {
    void run() throws Exception;
  }

  public static void showStackTrace(Component parentComponent, String header, Exception ex) {
    try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      JOptionPane.showMessageDialog(parentComponent,
          header + ex.getMessage() + ";\n" + sw.toString());
    } catch (IOException ignore) {
    }
  }

  public static ActionListener newActionListener(Component component, Action action) {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          action.run();
        } catch (Exception ex) {
          showStackTrace(component, "An error occurred", ex);
        }
      }
    };
  }

  public static DocumentListener newDocumentListener(Runnable onChange) {
    return new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent e) {
        onChange.run();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        onChange.run();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        onChange.run();
      }
    };
  }

  private GuiUtil() {}
}
