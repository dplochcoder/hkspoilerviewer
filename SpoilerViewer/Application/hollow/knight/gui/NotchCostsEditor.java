package hollow.knight.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.common.primitives.Ints;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

// TODO: Figure out how to plug in Transcendence
public final class NotchCostsEditor extends ComplexTextEditor {

  private final StateContext ctx;
  private final Map<Term, Integer> costs;

  public NotchCostsEditor(StateContext ctx) {
    this.ctx = ctx;
    this.costs = new HashMap<>();
  }

  private static String render(StateContext ctx, Term term) {
    return term.name() + ": " + ctx.notchCosts().notchCost(ctx.charmIds().charmId(term));
  }

  @Override
  protected List<String> getInitialLines() {
    return ctx.charmIds().charmTerms().stream().sorted().map(t -> render(ctx, t))
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  protected void parseLine(String line) throws FormException {
    ImmutableList<String> parse =
        Arrays.stream(line.split(":")).map(String::trim).collect(ImmutableList.toImmutableList());
    if (parse.size() != 2) {
      throw new FormException("Too many colons");
    }

    Term term = Term.create(parse.get(0));
    Integer charmId = ctx.charmIds().charmId(term);
    if (charmId == null) {
      throw new FormException("Not a known charm name");
    }

    Integer cost = Ints.tryParse(parse.get(1));
    if (cost == null || cost < 0) {
      throw new FormException("Notch cost must be a non-negative integer");
    }

    if (costs.put(term, cost) != null) {
      throw new FormException("Duplicate charm cost");
    }
  }

  @Override
  protected void finish() throws FormException {
    SetView<Term> diff = Sets.difference(costs.keySet(), ctx.charmIds().charmTerms());
    if (!diff.isEmpty()) {
      throw new FormException("Missing charm costs: "
          + diff.stream().map(Term::name).sorted().collect(ImmutableList.toImmutableList()));
    }

    List<Integer> listCosts = new ArrayList<>(costs.values());
    costs.forEach((t, c) -> listCosts.set(ctx.charmIds().charmId(t) - 1, c));
    ctx.notchCosts().setCosts(listCosts);
  }
}
