package hollow.knight.gui;

import java.util.Arrays;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Ints;
import hollow.knight.logic.MutableTermMap;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

public final class TolerancesEditor extends ComplexTextEditor {

  private final StateContext ctx;
  private final MutableTermMap tolerances;

  public TolerancesEditor(StateContext ctx) {
    this.ctx = ctx;
    this.tolerances = new MutableTermMap(ctx.tolerances());
  }

  @Override
  protected List<String> getInitialLines() {
    return Term.costTerms().stream().sorted().map(t -> t.name() + ": " + tolerances.get(t))
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
    if (!Term.costTerms().contains(term)) {
      throw new FormException("Not a supported cost term");
    }

    Integer value = Ints.tryParse(parse.get(1));
    if (value == null || value > 0) {
      throw new FormException("Tolerances should be non-positive");
    }

    tolerances.set(term, value);
  }

  @Override
  protected void finish() {
    ctx.setTolerances(tolerances);
  }
}
