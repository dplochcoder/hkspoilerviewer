package hollow.knight.gui;

import java.awt.Component;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.google.common.collect.ImmutableList;

public abstract class ComplexTextEditor {

  protected static class FormException extends Exception {
    private static final long serialVersionUID = 1L;

    public FormException(String msg) {
      super(msg);
    }
  }

  protected abstract List<String> getInitialLines();

  protected abstract void parseLine(String line) throws FormException;

  protected abstract void finish() throws FormException;

  public final boolean performEdit(Component parent) {
    List<String> initialLines = getInitialLines();
    String txtForm = initialLines.stream().collect(Collectors.joining("\n"));

    outer: while (true) {
      JTextArea textArea = new JTextArea(txtForm);
      textArea.setColumns(64);
      textArea.setRows(initialLines.size());
      textArea.setSize(textArea.getPreferredSize());

      int code = JOptionPane.showConfirmDialog(parent,
          new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
          "Edit Notch Costs", JOptionPane.OK_CANCEL_OPTION);
      if (code != JOptionPane.OK_OPTION) {
        return false;
      }

      txtForm = textArea.getText();
      List<String> lines =
          Arrays.stream(txtForm.split("\\n")).collect(ImmutableList.toImmutableList());
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (line.isEmpty()) {
          continue;
        }

        try {
          parseLine(line);
        } catch (FormException e) {
          JOptionPane.showMessageDialog(parent,
              "Couldn't parse line #" + (i + 1) + " '" + line + "': " + e.getMessage(),
              "Form Error", JOptionPane.WARNING_MESSAGE);
          continue outer;
        }
      }

      try {
        finish();
        return true;
      } catch (FormException e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Form Error",
            JOptionPane.WARNING_MESSAGE);
      }
    }
  }
}
