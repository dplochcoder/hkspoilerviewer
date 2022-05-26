package hollow.knight.logic;

import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;

@AutoValue
public abstract class ItemCheck {
  public abstract CheckId id();

  public abstract Location location();

  public abstract Item item();

  public abstract Costs costs();

  public abstract boolean vanilla();

  @Memoized
  public Condition condition() {
    return Conjunction.of(location().accessCondition(), costs().asCondition());
  }

  public static ItemCheck create(CheckId id, Location loc, Item item, Costs costs,
      boolean vanilla) {
    return new AutoValue_ItemCheck(id, loc, item, costs, vanilla);
  }
}
