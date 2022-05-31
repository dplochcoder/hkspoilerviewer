package hollow.knight.gui;

import java.awt.Container;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import hollow.knight.logic.Item;

public final class CheckEditorItemSearchField {

  @FunctionalInterface
  public interface Listener {
    void textChanged();
  }

  private final Object mutex = new Object();
  private final Set<Listener> listeners = new HashSet<>();

  private final JPanel searchPanel;
  private final JTextField textField;

  public CheckEditorItemSearchField() {
    searchPanel = new JPanel();
    searchPanel.add(new JLabel("Search: "));
    textField = createTextField();
    searchPanel.add(textField);
  }

  private void textChanged() {
    List<Listener> copy = new ArrayList<>();
    synchronized (mutex) {
      copy.addAll(listeners);
    }
    copy.forEach(Listener::textChanged);
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
    synchronized (mutex) {
      listeners.add(listener);
    }
  }

  public void removeListener(Listener listener) {
    synchronized (mutex) {
      listeners.remove(listener);
    }
  }

  public void addToGui(Container container) {
    container.add(searchPanel);
  }
}
