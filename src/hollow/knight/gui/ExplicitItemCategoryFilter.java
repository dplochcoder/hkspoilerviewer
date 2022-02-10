package hollow.knight.gui;

import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.Term;

public final class ExplicitItemCategoryFilter extends ItemCategoryFilter {
  private final ImmutableSet<Term> terms;

  private ExplicitItemCategoryFilter(String name, Set<Term> terms) {
    super(name);
    this.terms = ImmutableSet.copyOf(terms);
  }

  @Override
  public boolean accept(ItemCheck itemCheck) {
    return terms.contains(itemCheck.item().term());
  }

  public static ExplicitItemCategoryFilter parse(String name, JsonArray items) {
    ImmutableSet.Builder<Term> builder = ImmutableSet.builder();
    for (JsonElement item : items) {
      builder.add(Term.create(item.getAsString()));
    }
    return new ExplicitItemCategoryFilter(name, builder.build());
  }
}
