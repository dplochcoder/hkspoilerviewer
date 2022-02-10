package hollow.knight.logic;

// A unique check, identified by hash code.
public final class ItemCheck {
  private final Location location;
  private final Item item;
  private final Costs costs;
  private final boolean vanilla;

  public ItemCheck(Location location, Item item, Costs costs, boolean vanilla) {
    this.location = location;
    this.item = item;
    this.costs = costs;
    this.vanilla = vanilla;
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

  public boolean vanilla() {
    return vanilla;
  }
}
