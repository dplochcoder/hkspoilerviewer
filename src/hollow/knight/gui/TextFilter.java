package hollow.knight.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.StateContext;

public final class TextFilter extends SearchResult.Filter {

  private static enum Mode {
    ITEM("Item", false), LOCATION("Location", false), BOTH("Both", true);

    private final String name;
    private final boolean defaultSelected;

    Mode(String name, boolean defaultSelected) {
      this.name = name;
      this.defaultSelected = defaultSelected;
    }
  }

  private final JPanel searchPanel = new JPanel();
  private final JPanel modePanel = new JPanel();

  private final RoomLabels roomLabels;
  private final JTextField textField;
  private Mode mode = Mode.BOTH;

  public TextFilter(RoomLabels roomLabels) {
    this.roomLabels = roomLabels;

    searchPanel.add(new JLabel("Search:"));
    this.textField = createTextField();
    searchPanel.add(textField);

    ButtonGroup group = new ButtonGroup();
    Arrays.stream(Mode.values()).forEach(m -> addModeButton(modePanel, group, m));
  }

  private JTextField createTextField() {
    JTextField field = new JTextField(16);
    field.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent e) {
        TextFilter.this.filterChanged();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        TextFilter.this.filterChanged();
      }

      @Override
      public void removeUpdate(DocumentEvent e) {
        TextFilter.this.filterChanged();
      }
    });
    return field;
  }

  private void addModeButton(JPanel parent, ButtonGroup group, Mode m) {
    JRadioButton button = new JRadioButton(m.name, m.defaultSelected);
    group.add(button);
    parent.add(button);

    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TextFilter.this.mode = m;
        TextFilter.this.filterChanged();
      }
    });
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    String t = textField.getText().trim().toLowerCase();
    if (t.isEmpty()) {
      return true;
    }

    return Arrays.stream(t.split("\\s")).allMatch(term -> matchesTerm(result, term));
  }

  @Override
  public void addGuiToPanel(JPanel panel) {
    panel.add(searchPanel);
    panel.add(modePanel);
  }

  private boolean matchesTerm(SearchResult result, String term) {
    if (mode != Mode.LOCATION) {
      String item = result.item().term().name().toLowerCase();
      if (item.contains(term)) {
        return true;
      }
    }

    if (mode != Mode.ITEM) {
      List<String> locations = new ArrayList<>();
      locations.add(result.location().name());
      locations.add(result.location().scene());
      locations.add(roomLabels.get(result.location().scene(), RoomLabels.Type.MAP));
      locations.add(roomLabels.get(result.location().scene(), RoomLabels.Type.TITLE));
      if (locations.stream().anyMatch(l -> l.toLowerCase().contains(term))) {
        return true;
      }
    }

    return false;
  }

}
