package hollow.knight.gui;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

@FunctionalInterface
public interface ItemCategoryFilter {
  boolean accept(StateContext ctx, ItemCheck itemCheck);

  public static ItemCategoryFilter forTerms(String term1, String... rest) {
    ImmutableSet<Term> terms =
        Lists.asList(term1, rest).stream().map(Term::create).collect(ImmutableSet.toImmutableSet());
    return (ctx, itemCheck) -> terms.contains(itemCheck.item().term());
  }

  public static ItemCategoryFilter forPools(String pool1, String... rest) {
    ImmutableSet<String> pools = ImmutableSet.copyOf(Lists.asList(pool1, rest));
    return (ctx, itemCheck) -> pools.contains(itemCheck.item().getPool(ctx.pools()));
  }

  public static ItemCategoryFilter forEffect(Term effectTerm) {
    return (ctx, itemCheck) -> itemCheck.item().hasEffectTerm(effectTerm);
  }
}
