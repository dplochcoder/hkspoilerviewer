package hollow.knight.util;

import java.awt.Component;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.JOptionPane;

public final class GuiUtil {

  public static void showStackTrace(Component parentComponent, String header, Exception ex) {
    try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
      ex.printStackTrace(pw);
      JOptionPane.showMessageDialog(parentComponent,
          header + ex.getMessage() + ";\n" + sw.toString());
    } catch (IOException ignore) {
    }
  }

  private GuiUtil() {}
}
