package hollow.knight.logic;

public final class ItemCheck implements Comparable<ItemCheck> {
  private final int id;
  private final Location location;
  private final Item item;
  private final Costs costs;
  private final Condition condition;
  private final boolean vanilla;

  public ItemCheck(int id, Location location, Item item, Costs costs, boolean vanilla) {
    this.id = id;
    this.location = location;
    this.item = item;
    this.costs = costs;
    this.condition = Conjunction.of(location.accessCondition(), costs.asCondition());
    this.vanilla = vanilla;
  }

  @Override
  public int compareTo(ItemCheck that) {
    return Integer.compare(this.id, that.id);
  }

  public int id() {
    return id;
  }

  public Location location() {
    return location;
  }

  public Item item() {
    return item;
  }

  public Costs costs() {
    return costs;
  }

  public Condition condition() {
    return condition;
  }

  public boolean vanilla() {
    return vanilla;
  }
}
