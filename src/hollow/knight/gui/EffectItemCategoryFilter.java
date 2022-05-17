package hollow.knight.gui;

import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;

public final class EffectItemCategoryFilter extends ItemCategoryFilter {
  private final Term effectTerm;

  protected EffectItemCategoryFilter(String name, Term effectTerm) {
    super(name);
    this.effectTerm = effectTerm;
  }

  @Override
  public boolean accept(StateContext ctx, ItemCheck itemCheck) {
    return itemCheck.item().hasEffectTerm(effectTerm);
  }

  public static EffectItemCategoryFilter parse(String name, String effectTerm) {
    return new EffectItemCategoryFilter(name, Term.create(effectTerm));
  }
}
