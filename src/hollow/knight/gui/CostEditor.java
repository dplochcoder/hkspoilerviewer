package hollow.knight.gui;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.primitives.Ints;
import hollow.knight.logic.Cost;
import hollow.knight.logic.Term;
import hollow.knight.util.GuiUtil;

public final class CostEditor {
  private static final ImmutableBiMap<String, Term> COST_TYPES = ImmutableBiMap.of("Charms",
      Term.charms(), "Essence", Term.essence(), "Geo", Term.geo(), "Grubs", Term.grubs(),
      "Rancid Eggs", Term.rancidEggs(), "Howling Wraiths", Term.scream());

  private final JPanel panel;
  private final JComboBox<String> costType;
  private final JTextField numericField;

  private CostEditor upNeighbor;
  private final JButton moveUp;
  private CostEditor downNeighbor;
  private final JButton moveDown;

  public CostEditor(Cost cost, Consumer<CostEditor> onDelete) {
    this.panel = new JPanel();

    this.costType = new JComboBox<>();
    for (String name : COST_TYPES.keySet()) {
      costType.addItem(name);
    }
    panel.add(costType);

    this.numericField = new JTextField(6);
    numericField.getDocument()
        .addDocumentListener(GuiUtil.newDocumentListener(this::updateNumericFieldColor));
    panel.add(numericField);

    moveUp = moveButton("Up", () -> this.upNeighbor);
    moveDown = moveButton("Down", () -> this.downNeighbor);

    JButton delete = new JButton("Delete");
    delete.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        onDelete.accept(CostEditor.this);
      }
    });

    panel.add(moveUp);
    panel.add(moveDown);
    panel.add(delete);

    setCost(cost);
  }

  private void updateNumericFieldColor() {
    String value = numericField.getText();
    Integer intValue = Ints.tryParse(value.trim());
    if (intValue == null || intValue < 0) {
      numericField.setBackground(new Color(0.7f, 1.0f, 1.0f));
    } else {
      numericField.setBackground(Color.white);
    }
  }

  public void setUpNeighbor(CostEditor costEditor) {
    upNeighbor = costEditor;
    moveUp.setEnabled(costEditor != null);
  }

  public void setDownNeighbor(CostEditor costEditor) {
    downNeighbor = costEditor;
    moveDown.setEnabled(costEditor != null);
  }

  private JButton moveButton(String name, Supplier<CostEditor> neighbor) {
    JButton button = new JButton(name);
    button.setEnabled(false);
    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        CostEditor swap = neighbor.get();
        if (swap == null) {
          return;
        }

        Cost c1 = getCost();
        Cost c2 = swap.getCost();
        if (c1 == null || c2 == null) {
          return;
        }

        setCost(c2);
        swap.setCost(c1);
      }
    });
    return button;
  }

  public void setCost(Cost cost) {
    costType.setSelectedIndex(COST_TYPES.values().asList()
        .indexOf(cost.type() == Cost.Type.GEO ? Term.geo() : cost.term()));
    numericField.setText(String.valueOf(cost.value()));
    numericField.setBackground(Color.white);
  }

  public Cost getCost() {
    Integer value = Ints.tryParse(numericField.getText().trim());
    if (value == null || value < 0) {
      return null;
    }

    Term term = COST_TYPES.get(costType.getSelectedItem());
    if (term.equals(Term.geo())) {
      return Cost.createGeo(value);
    } else {
      return Cost.createTerm(term, value);
    }
  }

  public JPanel panel() {
    return panel;
  }
}
