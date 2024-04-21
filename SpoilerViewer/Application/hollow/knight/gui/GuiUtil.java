package hollow.knight.gui;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public final class GuiUtil {

  @FunctionalInterface
  public interface Action {
    void run() throws Exception;
  }

  public static void copyToClipboard(String s) {
    StringSelection sel = new StringSelection(s);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
  }

  public static String wrap(String in, int maxCols) {
    if (in == null) {
      return "null";
    }

    StringBuilder out = new StringBuilder();

    int lineStart = 0;
    int lastSpace = -1;
    for (int i = 0; i < in.length(); i++) {
      if (i - lineStart >= maxCols) {
        if (lastSpace < lineStart) {
          out.append(in.substring(lineStart, i));
          lineStart = i;
          lastSpace = i - 1;
        } else {
          out.append(in.substring(lineStart, lastSpace));
          lineStart = lastSpace + 1;
          lastSpace = lineStart - 1;
        }

        if (i < in.length() - 1) {
          out.append('\n');
        }
      }

      char ch = in.charAt(i);
      if (ch == '\n') {
        out.append(in.substring(lineStart, i + 1));
        lineStart = i + 1;
        lastSpace = i;
      } else if (Character.isWhitespace(in.charAt(i))) {
        lastSpace = i;
        if (lineStart == lastSpace - 1) {
          lineStart = lastSpace + 1;
        }
      }
    }

    out.append(in.substring(lineStart));
    return out.toString();
  }

  public static void showStackTrace(Component parentComponent, String header, Exception ex) {
    try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      String msg = wrap(ex.getMessage(), 200) + ";\n" + wrap(sw.toString(), 200);
      copyToClipboard(msg);

      JOptionPane.showMessageDialog(parentComponent, msg + "\n\n(Copied to Clipboard!)", header,
          JOptionPane.WARNING_MESSAGE);
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

  public static JMenuItem newInfoMenuItem(JFrame frame, String title, Iterable<String> content) {
    JMenuItem out = new JMenuItem(title);
    out.addActionListener(newActionListener(frame, () -> showInfo(frame, title, content)));
    return out;
  }

  private static void showInfo(JFrame frame, String title, Iterable<String> content) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

    for (String info : content) {
      if (info.contentEquals("-")) {
        panel.add(new JSeparator());
      } else {
        panel.add(new JLabel(info));
      }
    }

    JDialog dialog = new JDialog(frame, title);
    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setVisible(true);
  }

  private GuiUtil() {}
}
