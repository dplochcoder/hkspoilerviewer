package hollow.knight.gui;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import hollow.knight.logic.Item;
import hollow.knight.logic.SynchronizedEntityManager;

public final class CheckEditorItemSearchField {

  @FunctionalInterface
  public interface Listener {
    void textChanged();
  }

  private final SynchronizedEntityManager<Listener> listeners = new SynchronizedEntityManager<>();

  private final JPanel searchPanel;
  private final JTextField textField;

  public CheckEditorItemSearchField() {
    searchPanel = new JPanel();
    searchPanel.add(new JLabel("Search: "));
    textField = createTextField();
    searchPanel.add(textField);
  }

  private void textChanged() {
    listeners.forEach(Listener::textChanged);
  }

  public boolean accept(Item item) {
    String t = textField.getText().trim().toLowerCase();
    if (t.isEmpty()) {
      return true;
    }

    List<String> tokens = new ArrayList<>();
    tokens.add(item.term().name().toLowerCase());
    tokens.add(item.displayName().toLowerCase());
    return Arrays.stream(t.split("\\s"))
        .allMatch(term -> tokens.stream().anyMatch(s -> s.contains(term)));
  }

  private JTextField createTextField() {
    JTextField field = new JTextField(16);
    field.getDocument().addDocumentListener(GuiUtil.newDocumentListener(this::textChanged));
    return field;
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  public void addToGui(Container container) {
    container.add(searchPanel);
  }
}
