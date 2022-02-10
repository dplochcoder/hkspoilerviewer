package hollow.knight.gui;

import java.util.regex.Pattern;
import hollow.knight.logic.ItemCheck;

public final class RegexItemCategoryFilter extends ItemCategoryFilter {
  private final Pattern pattern;

  private RegexItemCategoryFilter(String name, Pattern pattern) {
    super(name);
    this.pattern = pattern;
  }

  @Override
  public boolean accept(ItemCheck itemCheck) {
    return pattern.matcher(itemCheck.item().term().name()).matches();
  }

  public static RegexItemCategoryFilter parse(String name, String regex) {
    return new RegexItemCategoryFilter(name, Pattern.compile(regex));
  }
}
