package hollow.knight.gui;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.State;
import hollow.knight.logic.SynchronizedEntityManager;

public final class TransitionRouting implements RouteListModel.StateInitializer {

  public static interface Listener {
    void transitionRoutingUpdated();
  }

  private final JPanel labelPanel = new JPanel();
  private final JPanel radioPanel = new JPanel();
  private final SynchronizedEntityManager<Listener> listeners = new SynchronizedEntityManager<>();

  private final ImmutableMap<State.TransitionStrategy, JRadioButton> radioButtons;

  private void notifyListeners() {
    listeners.forEach(Listener::transitionRoutingUpdated);
  }

  private JRadioButton createButton(String name, boolean selected) {
    JRadioButton b = new JRadioButton(name);
    b.setSelected(selected);
    b.addActionListener(GuiUtil.newActionListener(null, this::notifyListeners));
    return b;
  }

  public TransitionRouting() {
    this.radioButtons = ImmutableMap.of(State.TransitionStrategy.NONE, createButton("None", false),
        State.TransitionStrategy.VANILLA, createButton("Vanilla Only", true),
        State.TransitionStrategy.ALL, createButton("Vanilla & Randomized", false));

    labelPanel.add(new JLabel("Transition Routing"));
    ButtonGroup group = new ButtonGroup();
    radioButtons.values().forEach(b -> {
      group.add(b);
      radioPanel.add(b);
    });
  }

  public void addToGui(JPanel panel) {
    panel.add(this.labelPanel);
    panel.add(this.radioPanel);
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public void initializeState(State state) {
    radioButtons.forEach((s, b) -> {
      if (b.isSelected()) {
        state.setTransitionStrategy(s);
      }
    });
  }

}
