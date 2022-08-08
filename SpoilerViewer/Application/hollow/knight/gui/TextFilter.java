package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
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

  private final TransitionData transitionData;
  private final RoomLabels roomLabels;
  private final JTextField textField;
  private Mode mode = Mode.BOTH;

  public TextFilter(TransitionData transitionData, RoomLabels roomLabels) {
    this.transitionData = transitionData;
    this.roomLabels = roomLabels;

    searchPanel.add(new JLabel("Search:"));
    this.textField = createTextField();
    searchPanel.add(textField);

    ButtonGroup group = new ButtonGroup();
    Arrays.stream(Mode.values()).forEach(m -> addModeButton(modePanel, group, m));
  }

  private JTextField createTextField() {
    JTextField field = new JTextField(16);
    field.getDocument().addDocumentListener(GuiUtil.newDocumentListener(this::filterChanged));
    return field;
  }

  private void addModeButton(JPanel parent, ButtonGroup group, Mode m) {
    JRadioButton button = new JRadioButton(m.name, m.defaultSelected);
    group.add(button);
    parent.add(button);

    button.addActionListener(GuiUtil.newActionListener(null, () -> {
      mode = m;
      filterChanged();
    }));
  }

  @Override
  public boolean accept(StateContext ctx, SearchResult result) {
    String t = textField.getText().trim().toLowerCase();
    if (t.isEmpty()) {
      return true;
    }

    List<String> aliases = getAliases(result);
    return Arrays.stream(t.split("\\s"))
        .allMatch(term -> aliases.stream().anyMatch(s -> s.contains(term)));
  }

  public void addGuiToPanel(JPanel panel) {
    panel.add(searchPanel);
    panel.add(modePanel);
  }

  private List<String> getAliases(SearchResult result) {
    List<String> tokens = new ArrayList<>();
    if (mode != Mode.LOCATION) {
      tokens.add(result.item().term().name().toLowerCase());
      tokens.add(result.item().displayName(transitionData).toLowerCase());
    }

    if (mode != Mode.ITEM) {
      tokens.add(result.location().name().toLowerCase());
      tokens.add(result.location().displayName(transitionData).toLowerCase());
      tokens.add(result.location().scene().toLowerCase());
      tokens.add(roomLabels.get(result.location().scene(), RoomLabels.Type.MAP).toLowerCase());
      tokens.add(roomLabels.get(result.location().scene(), RoomLabels.Type.TITLE).toLowerCase());
    }

    return tokens;
  }

}
