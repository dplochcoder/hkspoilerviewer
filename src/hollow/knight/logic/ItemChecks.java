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
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A mutable mapping of locations to items. */
public final class ItemChecks {
  public interface Listener {
    void checkAdded(ItemCheck check);

    default void multipleChecksAdded(ImmutableSet<ItemCheck> checks) {
      checks.forEach(this::checkAdded);
    }

    void checkRemoved(ItemCheck check);

    default void multipleChecksRemoved(ImmutableSet<ItemCheck> checks) {
      checks.forEach(this::checkRemoved);
    }

    void checkReplaced(ItemCheck before, ItemCheck after);

    default void multipleChecksReplaced(ImmutableMap<ItemCheck, ItemCheck> replacements) {
      replacements.forEach(this::checkReplaced);
    }
  }

  private final Object mutex = new Object();
  private final Set<Listener> listeners = new HashSet<>();

  private final Map<CheckId, ItemCheck> checksById = new HashMap<>();
  private final BiMultimap<Condition, CheckId> idsByCondition = new BiMultimap<>();
  private final BiMultimap<String, CheckId> idsByLocation = new BiMultimap<>();

  private final Map<String, Location> locationsByName = new HashMap<>();
  private final Map<Term, Item> itemsByName = new HashMap<>();
  private final BiMultimap<Term, CheckId> idsByItemName = new BiMultimap<>();

  private int nextId = 1;

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
    ImmutableSet<ItemCheck> toRemove = checksById.values().stream().filter(filter)
        .filter(c -> !c.vanilla()).collect(ImmutableSet.toImmutableSet());
    Multimap<String, ItemCheck> modifiedLocations =
        Multimaps.index(toRemove, c -> checksById.get(c.id()).location().name());

    ImmutableSet.Builder<ItemCheck> addedBuilder = ImmutableSet.builder();
    for (String loc : modifiedLocations.keySet()) {
      if (loc.equals("Start")) {
        continue;
      }

      Collection<ItemCheck> removing = modifiedLocations.get(loc);
      ItemCheck template = removing.iterator().next();

      removing.forEach(c -> removeInternal(c.id()));
      if (!idsByLocation.containsKey(loc)) {
        ItemCheck toAdd = ItemCheck.create(newId(), template.location(), nothing(),
            Costs.defaultCosts(template.location().name()), false);
        addedBuilder.add(toAdd);
        addInternal(toAdd);
      }
    }

    if (!toRemove.isEmpty()) {
      listeners().forEach(l -> l.multipleChecksRemoved(toRemove));
    }
    ImmutableSet<ItemCheck> added = addedBuilder.build();
    if (!added.isEmpty()) {
      listeners().forEach(l -> l.multipleChecksAdded(added));
    }
  }

  public void compact() {
    ImmutableList<CheckId> sorted =
        checksById.keySet().stream().sorted().collect(ImmutableList.toImmutableList());

    ImmutableMap.Builder<ItemCheck, ItemCheck> replacedBuilder = ImmutableMap.builder();
    for (int i = 0; i < sorted.size(); i++) {
      CheckId id = sorted.get(i);

      if (id.id() != i + 1) {
        ItemCheck check = checksById.get(id);
        ItemCheck newCheck = ItemCheck.create(CheckId.of(i + 1), check.location(), check.item(),
            check.costs(), check.vanilla());

        removeInternal(check.id());
        addInternal(newCheck);
        replacedBuilder.put(check, newCheck);
      }
    }

    ImmutableMap<ItemCheck, ItemCheck> replaced = replacedBuilder.build();
    if (!replaced.isEmpty()) {
      listeners().forEach(l -> l.multipleChecksReplaced(replaced));
    }
  }

  public void overlayImportChecks(ItemChecks other) throws ICDLException {
    // Check that the set of non-vanilla locations being imported is a subset.
    Set<String> ours = allChecks().filter(c -> !c.vanilla()).map(c -> c.location().name())
        .collect(ImmutableSet.toImmutableSet());
    Set<String> theirs = other.allChecks().filter(c -> !c.vanilla()).map(c -> c.location().name())
        .collect(ImmutableSet.toImmutableSet());

    SetView<String> diff = Sets.difference(theirs, ours);
    if (!diff.isEmpty()) {
      throw new ICDLException(
          "Cannot import: " + diff.size() + " locations in the import are not randomized here");
    }

    // Remove all checks at the import locations.
    reduceToNothing(c -> theirs.contains(c.location().name()));

    // Import all checks.
    Map<String, ItemCheck> defaultChecks = new HashMap<>();
    allChecks().forEach(c -> defaultChecks.put(c.location().name(), c));

    Map<ItemCheck, ItemCheck> massReplacement = new HashMap<>();
    Set<ItemCheck> massAdd = new HashSet<>();
    other.allChecks().filter(c -> !c.vanilla()).forEach(c -> {
      ItemCheck newCheck = ItemCheck.create(newId(), getLocation(c.location().name()),
          getItem(c.item().term()), c.costs(), false);
      addInternal(newCheck);

      ItemCheck nothing = defaultChecks.remove(c.location().name());
      if (nothing != null) {
        removeInternal(nothing.id());
        massReplacement.put(nothing, newCheck);
      } else {
        massAdd.add(newCheck);
      }
    });

    ImmutableSet<ItemCheck> massAddFinal = ImmutableSet.copyOf(massAdd);
    listeners().forEach(l -> l.multipleChecksAdded(massAddFinal));

    ImmutableMap<ItemCheck, ItemCheck> massReplacementFinal = ImmutableMap.copyOf(massReplacement);
    listeners().forEach(l -> l.multipleChecksReplaced(massReplacementFinal));
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
