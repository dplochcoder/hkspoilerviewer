package hollow.knight.gui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.State;
import hollow.knight.logic.Term;
import hollow.knight.util.JsonUtil;

// UI object for enabling skips not part of the seed settings, to expand logic.
public final class Skips {

  public static interface Listener {
    void skipsUpdated();
  }

  @AutoValue
  abstract static class SkipTerm {
    public abstract Term effectTerm();

    public abstract JCheckBox box();

    public static SkipTerm create(Term effectTerm, JCheckBox box) {
      return new AutoValue_Skips_SkipTerm(effectTerm, box);
    }
  }

  private final ImmutableList<SkipTerm> skipTerms;
  private final JPanel panel = new JPanel();
  private final Set<Listener> listeners = new HashSet<>();

  private SkipTerm createSkipTerm(String name, Term effectTerm) {
    JCheckBox box = new JCheckBox(name);
    box.setSelected(false);
    box.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        notifyListeners();
      }
    });

    return SkipTerm.create(effectTerm, box);
  }

  private Skips(Map<String, Term> skipTerms) {
    this.skipTerms = skipTerms.keySet().stream().sorted()
        .map(s -> createSkipTerm(s, skipTerms.get(s))).collect(ImmutableList.toImmutableList());

    int numRows = (this.skipTerms.size() + 3) / 2;
    this.panel.setLayout(new GridLayout(numRows, 2));
    this.panel.add(new JLabel("Skips"));
    this.panel.add(new JLabel(""));
    this.skipTerms.forEach(st -> this.panel.add(st.box()));
  }

  public void addToGui(JPanel panel) {
    panel.add(this.panel);
  }

  public synchronized void addListener(Listener listener) {
    listeners.add(listener);
  }

  public synchronized void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private synchronized void notifyListeners() {
    ImmutableList<Listener> listeners =
        this.listeners.stream().collect(ImmutableList.toImmutableList());
    listeners.forEach(Listener::skipsUpdated);
  }

  public void setInitialState(State state) {
    for (SkipTerm st : skipTerms) {
      boolean isInSettings = state.get(st.effectTerm()) > 0;
      st.box().setEnabled(!isInSettings);
      st.box().setSelected(isInSettings);
    }
  }

  public void applySkips(State state) {
    for (SkipTerm st : skipTerms) {
      if (st.box().isSelected()) {
        Term t = st.effectTerm();
        if (state.get(t) == 0) {
          state.set(t, 1);
        }
      }
    }
  }

  public static Skips load() throws ParseException {
    JsonArray arr = JsonUtil.loadResource(Skips.class, "skips.json").getAsJsonArray();

    Map<String, Term> terms = new HashMap<>();
    for (JsonElement elem : arr) {
      JsonObject obj = elem.getAsJsonObject();
      terms.put(obj.get("Name").getAsString(), Term.create(obj.get("Effect").getAsString()));
    }
    return new Skips(terms);
  }
}
