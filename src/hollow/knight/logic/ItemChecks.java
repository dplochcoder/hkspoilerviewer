package hollow.knight.logic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A mutable mapping of locations to items. */
public final class ItemChecks {
  public interface Listener {
    void checkAdded(ItemCheck check);

    void checkRemoved(ItemCheck check);

    void checkReplaced(ItemCheck before, ItemCheck after);
  }

  private final Object mutex = new Object();
  private final Set<Listener> listeners = new HashSet<>();

  private final Map<CheckId, ItemCheck> checksById = new HashMap<>();
  private final BiMultimap<Condition, CheckId> idsByCondition = new BiMultimap<>();
  private final BiMultimap<String, CheckId> idsByLocation = new BiMultimap<>();

  private final Map<String, Location> locationsByName = new HashMap<>();
  private final Map<Term, Item> itemsByName = new HashMap<>();
  private final BiMultimap<Term, CheckId> idsByItemName = new BiMultimap<>();

  private long nextId = 1;

  private ItemChecks() {}

  public void addListener(Listener listener) {
    synchronized (mutex) {
      listeners.add(listener);
    }
  }

  public void removeListener(Listener listener) {
    synchronized (mutex) {
      listeners.remove(listener);
    }
  }

  private Stream<Listener> listeners() {
    List<Listener> listeners;
    synchronized (mutex) {
      listeners = new ArrayList<>(this.listeners);
    }
    return listeners.stream();
  }

  private CheckId newId() {
    return CheckId.of(nextId++);
  }

  public void addItem(Item item) {
    itemsByName.put(item.term(), item);
  }

  private void addInternal(ItemCheck check) {
    checksById.put(check.id(), check);
    idsByCondition.put(check.condition(), check.id());
    idsByLocation.put(check.location().name(), check.id());

    locationsByName.put(check.location().name(), check.location());
    itemsByName.put(check.item().term(), check.item());
    idsByItemName.put(check.item().term(), check.id());
  }

  private void removeInternal(CheckId id) {
    checksById.remove(id);
    idsByCondition.removeValue(id);
    idsByLocation.removeValue(id);
    idsByItemName.removeValue(id);
  }

  public CheckId placeNew(Location loc, Item item, Costs costs, boolean vanilla) {
    ItemCheck check = ItemCheck.create(newId(), loc, item, costs, vanilla);
    addInternal(check);

    listeners().forEach(l -> l.checkAdded(check));
    return check.id();
  }

  public CheckId replace(CheckId prevId, Location loc, Item item, Costs costs, boolean vanilla) {
    CheckId id = newId();
    ItemCheck before = checksById.get(prevId);
    ItemCheck after = ItemCheck.create(id, loc, item, costs, vanilla);

    removeInternal(prevId);
    addInternal(after);

    listeners().forEach(l -> l.checkReplaced(before, after));
    return id;
  }

  public void remove(CheckId id) {
    ItemCheck check = checksById.get(id);
    removeInternal(id);

    listeners().forEach(l -> l.checkRemoved(check));
  }

  public Location getLocation(String name) {
    return locationsByName.get(name);
  }

  public Item getItem(Term term) {
    return itemsByName.get(term);
  }

  public void reduceToNothing(Predicate<ItemCheck> filter) {
    // Keep at least one instance of each location alive.
    Set<CheckId> toRemove = checksById.values().stream().filter(filter).filter(c -> !c.vanilla())
        .map(ItemCheck::id).collect(ImmutableSet.toImmutableSet());
    Multimap<String, CheckId> modifiedLocations =
        Multimaps.index(toRemove, id -> checksById.get(id).location().name());

    for (String loc : modifiedLocations.keySet()) {
      if (loc.equals("Start")) {
        continue;
      }

      Collection<CheckId> removing = modifiedLocations.get(loc);
      ItemCheck template = checksById.get(idsByLocation.getKey(loc).iterator().next());

      removing.forEach(this::remove);
      if (!idsByLocation.containsKey(loc)) {
        placeNew(template.location(), nothing(), Costs.defaultCosts(template.location().name()),
            false);
      }
    }
  }

  public void compact() {
    ImmutableList<CheckId> sorted =
        checksById.keySet().stream().sorted().collect(ImmutableList.toImmutableList());

    for (int i = 0; i < sorted.size(); i++) {
      CheckId id = sorted.get(i);

      if (id.id() != i + 1) {
        ItemCheck check = checksById.get(id);
        replace(id, check.location(), check.item(), check.costs(), check.vanilla());
      }
    }
  }

  public ItemCheck get(CheckId id) {
    return checksById.get(id);
  }

  public Stream<ItemCheck> allChecks() {
    return checksById.values().stream();
  }

  public Stream<Item> allItems() {
    return itemsByName.values().stream();
  }

  public Stream<ItemCheck> startChecks() {
    return idsByLocation.getKey("Start").stream().map(checksById::get);
  }

  public Item nothing() {
    return itemsByName.get(Term.nothing());
  }

  public Stream<ItemCheck> getByCondition(Condition c) {
    return idsByCondition.getKey(c).stream().map(checksById::get);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    JsonArray arr = new JsonArray();
    checksById.values().forEach(c -> arr.add(c.toJson()));
    obj.add("checks", arr);
    return obj;
  }

  public void fromJson(JsonObject obj) {
    // Remove all item checks
    ImmutableSet<CheckId> ids = ImmutableSet.copyOf(checksById.keySet());
    ids.forEach(this::removeInternal);

    JsonArray arr = obj.get("checks").getAsJsonArray();
    arr.forEach(e -> addInternal(ItemCheck.fromJson(this, e.getAsJsonObject())));
  }

  private void parseCheck(JsonElement elem, RoomLabels rooms, boolean vanilla)
      throws ParseException {
    Item item = Item.parse(elem.getAsJsonObject().get("Item").getAsJsonObject());
    if (!item.types().contains("RandomizerMod.RC.SplitCloakItem")
        && item.types().stream().noneMatch(s -> s.startsWith("RandomizerCore.LogicItems."))) {
      return;
    }

    JsonObject locObj = elem.getAsJsonObject().get("Location").getAsJsonObject();
    JsonObject logicObj = locObj;
    if (logicObj.has("logic")) {
      logicObj = logicObj.get("logic").getAsJsonObject();
    }

    String locName = logicObj.get("Name").getAsString();
    Condition locAccess = ConditionParser.parse(logicObj.get("Logic").getAsString());
    Location loc = Location.create(rooms, locName, locAccess);

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (costsObj != null && !costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    placeNew(loc, item, costs, vanilla);
  }

  public static ItemChecks parse(JsonObject json, RoomLabels roomLabels) throws ParseException {
    ItemChecks checks = new ItemChecks();

    // Parse locations.
    for (JsonElement elem : json.get("itemPlacements").getAsJsonArray()) {
      try {
        checks.parseCheck(elem, roomLabels, false);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }
    for (JsonElement elem : json.get("Vanilla").getAsJsonArray()) {
      try {
        checks.parseCheck(elem, roomLabels, true);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }

    return checks;
  }
}
