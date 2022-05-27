package hollow.knight.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.primitives.Ints;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

public final class NotchCostsEditor {

  private static class NotchParseException extends Exception {
    private static final long serialVersionUID = 1L;

    NotchParseException(String msg) {
      super(msg);
    }

    NotchParseException(String line, int lineNum, String msg) {
      super("Couldn't parse line " + lineNum + ": \"" + line + "\": " + msg);
    }
  }

  private static String render(StateContext ctx, Term term) {
    return term.name() + ": " + ctx.notchCosts().notchCost(ctx.charmIds().charmId(term));
  }

  // Returns true iff NotchCosts were edited.
  public static boolean editNotchCosts(Component parent, StateContext ctx) {
    String txtForm = ctx.charmIds().charmTerms().stream().sorted().map(t -> render(ctx, t))
        .collect(Collectors.joining("\n"));

    while (true) {
      JTextArea textArea = new JTextArea(txtForm);
      textArea.setColumns(64);
      textArea.setRows(ctx.charmIds().charmTerms().size());
      textArea.setSize(textArea.getPreferredSize());

      int code = JOptionPane.showConfirmDialog(parent,
          new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
              JScrollPane.HORIZONTAL_SCROLLBAR_NEVER),
          "Edit Notch Costs", JOptionPane.OK_CANCEL_OPTION);
      if (code != JOptionPane.OK_OPTION) {
        return false;
      }

      txtForm = textArea.getText();
      List<Integer> notchCosts;
      try {
        notchCosts = parseNotchCosts(ctx, txtForm);
      } catch (NotchParseException ex) {
        JOptionPane.showMessageDialog(parent, ex.getMessage());
        continue;
      }

      ctx.notchCosts().setCosts(notchCosts);
      return true;
    }
  }

  private static List<Integer> parseNotchCosts(StateContext ctx, String str)
      throws NotchParseException {
    ImmutableList<String> lines =
        Arrays.stream(str.split("\\n")).collect(ImmutableList.toImmutableList());

    Map<Term, Integer> costs = new HashMap<>();
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i).trim();
      int lineNum = i + 1;
      if (line.isEmpty()) {
        continue;
      }

      ImmutableList<String> parse =
          Arrays.stream(line.split(":")).map(String::trim).collect(ImmutableList.toImmutableList());
      if (parse.size() != 2) {
        throw new NotchParseException(line, lineNum, "Too many colons");
      }

      Term term = Term.create(parse.get(0));
      Integer charmId = ctx.charmIds().charmId(term);
      if (charmId == null) {
        throw new NotchParseException(line, lineNum, "Not a known charm name");
      }

      Integer cost = Ints.tryParse(parse.get(1));
      if (cost == null || cost < 0) {
        throw new NotchParseException(line, lineNum, "Notch cost must be a non-negative integer");
      }

      if (costs.put(term, cost) != null) {
        throw new NotchParseException(line, lineNum, "Duplicate charm cost");
      }
    }

    SetView<Term> diff = Sets.difference(costs.keySet(), ctx.charmIds().charmTerms());
    if (!diff.isEmpty()) {
      throw new NotchParseException("Missing charm costs: " + diff);
    }

    List<Integer> listCosts = new ArrayList<>(costs.values());
    costs.forEach((t, c) -> listCosts.set(ctx.charmIds().charmId(t) - 1, c));
    return listCosts;
  }
}
