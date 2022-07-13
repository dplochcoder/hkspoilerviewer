package hollow.knight.logic;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Multiset;
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

  private final SynchronizedEntityManager<Listener> listeners = new SynchronizedEntityManager<>();

  private final BiMap<CheckId, ItemCheck> checksById = HashBiMap.create();
  private final BiMultimap<Condition, CheckId> idsByCondition = new BiMultimap<>();
  private final BiMultimap<String, CheckId> idsByLocation = new BiMultimap<>();

  private final Map<String, Location> locationsByName = new HashMap<>();
  private final Map<Term, Item> itemsByName = new HashMap<>();
  private final BiMultimap<Term, CheckId> idsByItemName = new BiMultimap<>();
  private final Multiset<Term> effectTerms = HashMultiset.create();

  private final Multiset<String> originalItemCounts = HashMultiset.create();
  private final Multiset<String> itemCounts = HashMultiset.create();
  private final Set<Term> originalNonVanillaItems = new HashSet<>();

  private int nextId = 1;

  private ItemChecks() {}

  private void calculateOriginalItemCounts() {
    originalItemCounts.addAll(itemCounts);

    checksById.values().stream().filter(c -> !c.vanilla()).map(c -> c.item().term()).distinct()
        .forEach(originalNonVanillaItems::add);
  }

  public boolean isOriginalNonVanilla(Term term) {
    return originalNonVanillaItems.contains(term);
  }

  public int originalItemCount(String name) {
    return originalItemCounts.count(name);
  }

  public ImmutableMap<String, Integer> getICDLItemDiff() {
    Map<String, Integer> counts = new HashMap<>();
    for (String key : Sets.union(originalItemCounts.elementSet(), itemCounts.elementSet())) {
      int diff = itemCounts.count(key) - originalItemCounts.count(key);
      if (diff != 0) {
        counts.put(key, diff);
      }
    }

    return counts.keySet().stream()
        .sorted((k1, k2) -> ComparisonChain.start()
            .compare(Math.abs(counts.getOrDefault(k2, 0)), Math.abs(counts.getOrDefault(k1, 0)))
            .compare(counts.getOrDefault(k2, 0), counts.getOrDefault(k1, 0)).compare(k1, k2)
            .result())
        .collect(ImmutableMap.toImmutableMap(k -> k, k -> counts.get(k)));
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  private CheckId newId() {
    return CheckId.of(nextId++);
  }

  public void addItem(Item item) {
    itemsByName.put(item.term(), item);
  }

  public boolean isEffectTerm(Term term) {
    return effectTerms.count(term) > 0;
  }

  private void addInternal(ItemCheck check) {
    checksById.put(check.id(), check);
    idsByCondition.put(check.condition(), check.id());
    idsByLocation.put(check.location().name(), check.id());

    locationsByName.put(check.location().name(), check.location());
    itemsByName.put(check.item().term(), check.item());
    idsByItemName.put(check.item().term(), check.id());
    itemCounts.add(check.item().term().name());
    check.item().effectTerms().forEach(effectTerms::add);
  }

  private void removeInternal(CheckId id) {
    ItemCheck check = checksById.remove(id);
    idsByCondition.removeValue(id);
    idsByLocation.removeValue(id);
    idsByItemName.removeValue(id);
    itemCounts.remove(check.item().term().name());
    check.item().effectTerms().forEach(effectTerms::remove);
  }

  public CheckId placeNew(Location loc, Item item, Costs costs, boolean vanilla) {
    ItemCheck check = ItemCheck.create(newId(), loc, item, costs, vanilla);
    addInternal(check);

    listeners.forEach(l -> l.checkAdded(check));
    return check.id();
  }

  public void addMultiple(ImmutableSet<ItemCheck> checks) {
    if (checks.isEmpty()) {
      return;
    }

    checks.forEach(this::addInternal);
    listeners.forEach(l -> l.multipleChecksAdded(checks));
  }

  public void removeMultiple(ImmutableSet<CheckId> checkIds) {
    if (checkIds.isEmpty()) {
      return;
    }

    ImmutableSet.Builder<ItemCheck> checksBuilder = ImmutableSet.builder();
    checkIds.forEach(id -> {
      checksBuilder.add(checksById.get(id));
      removeInternal(id);
    });

    ImmutableSet<ItemCheck> checks = checksBuilder.build();
    listeners.forEach(l -> l.multipleChecksRemoved(checks));
  }

  public CheckId replace(CheckId prevId, Location loc, Item item, Costs costs, boolean vanilla) {
    CheckId id = newId();
    ItemCheck before = checksById.get(prevId);
    ItemCheck after = ItemCheck.create(id, loc, item, costs, vanilla);

    removeInternal(prevId);
    addInternal(after);

    listeners.forEach(l -> l.checkReplaced(before, after));
    return id;
  }

  public void remove(CheckId id) {
    ItemCheck check = checksById.get(id);
    removeInternal(id);

    listeners.forEach(l -> l.checkRemoved(check));
  }

  public Location getLocation(String name) throws ICDLException {
    Location loc = locationsByName.get(name);
    if (loc == null) {
      throw new ICDLException("Unknown location: " + name);
    }

    return loc;
  }

  public Stream<ItemCheck> getChecksAtLocation(String loc) throws ICDLException {
    Set<CheckId> ids = idsByLocation.getValue(loc);
    if (ids.isEmpty()) {
      throw new ICDLException("Unknown location: " + loc);
    }

    return ids.stream().map(checksById::get);
  }

  public boolean hasItem(Term term) {
    return itemsByName.containsKey(term);
  }

  public Item getItem(Term term) throws ICDLException {
    Item item = itemsByName.get(term);
    if (item == null) {
      throw new ICDLException("Unknown item: " + term.name());
    }

    return item;
  }

  public void reduceToNothing(Predicate<ItemCheck> filter) throws ICDLException {
    // Keep at least one instance of each location alive.
    ImmutableSet<ItemCheck> toRemove = checksById.values().stream().filter(filter)
        .filter(c -> !c.vanilla()).collect(ImmutableSet.toImmutableSet());
    Multimap<String, ItemCheck> modifiedLocations =
        Multimaps.index(toRemove, c -> checksById.get(c.id()).location().name());

    ImmutableSet.Builder<ItemCheck> addedBuilder = ImmutableSet.builder();
    for (String loc : modifiedLocations.keySet()) {
      Collection<ItemCheck> removing = modifiedLocations.get(loc);
      ItemCheck template = removing.iterator().next();

      removing.forEach(c -> removeInternal(c.id()));
      if (!idsByLocation.containsKey(loc)) {
        ItemCheck toAdd = ItemCheck.create(newId(), template.location(),
            template.isTransition() ? kingsPassTransition() : nothing(),
            Costs.defaultCosts(template.location().name()), false);
        addedBuilder.add(toAdd);
        addInternal(toAdd);
      }
    }

    if (!toRemove.isEmpty()) {
      listeners.forEach(l -> l.multipleChecksRemoved(toRemove));
    }
    ImmutableSet<ItemCheck> added = addedBuilder.build();
    if (!added.isEmpty()) {
      listeners.forEach(l -> l.multipleChecksAdded(added));
    }
  }

  private int compareIds(CheckId id1, CheckId id2) {
    ItemCheck c1 = checksById.get(id1);
    ItemCheck c2 = checksById.get(id2);
    return ComparisonChain.start().compareFalseFirst(c1.vanilla(), c2.vanilla())
        .compareFalseFirst(c1.isTransition(), c2.isTransition()).compare(id1.id(), id2.id())
        .result();
  }

  public void compact() {
    ImmutableList<CheckId> sorted = checksById.keySet().stream().sorted(this::compareIds)
        .collect(ImmutableList.toImmutableList());

    ImmutableMap.Builder<ItemCheck, ItemCheck> replacedBuilder = ImmutableMap.builder();
    for (int i = 0; i < sorted.size(); i++) {
      CheckId id = sorted.get(i);

      if (id.id() != i) {
        ItemCheck check = checksById.get(id);
        ItemCheck newCheck = ItemCheck.create(CheckId.of(i), check.location(), check.item(),
            check.costs(), check.vanilla());

        removeInternal(check.id());
        addInternal(newCheck);
        replacedBuilder.put(check, newCheck);
      }
    }

    ImmutableMap<ItemCheck, ItemCheck> replaced = replacedBuilder.build();
    if (!replaced.isEmpty()) {
      listeners.forEach(l -> l.multipleChecksReplaced(replaced));
    }
  }

  public void overlayImportChecks(ItemChecks other) throws ICDLException {
    // Check that the set of non-vanilla locations being imported is a subset of all our locations.
    Set<String> ours =
        allChecks().map(c -> c.location().name()).collect(ImmutableSet.toImmutableSet());
    Set<String> theirs = other.allChecks().filter(c -> !c.vanilla()).map(c -> c.location().name())
        .collect(ImmutableSet.toImmutableSet());

    // Don't touch Start.
    ours.remove("Start");
    theirs.remove("Start");

    SetView<String> diff = Sets.difference(theirs, ours);
    if (!diff.isEmpty()) {
      throw new ICDLException(
          "Cannot import: " + diff.size() + " locations in the import are not randomized here: "
              + diff.stream().sorted().collect(Collectors.joining("\n")));
    }

    // Remove all checks at the import locations.
    reduceToNothing(c -> theirs.contains(c.location().name()));

    // Import all checks.
    Map<String, ItemCheck> defaultChecks = new HashMap<>();
    allChecks().forEach(c -> defaultChecks.put(c.location().name(), c));

    Map<ItemCheck, ItemCheck> massReplacement = new HashMap<>();
    Set<ItemCheck> massAdd = new HashSet<>();

    Iterable<ItemCheck> checks = () -> other.allChecks().iterator();
    for (ItemCheck c : checks) {
      if (c.vanilla()) {
        continue;
      }

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
    }

    ImmutableSet<ItemCheck> massAddFinal = ImmutableSet.copyOf(massAdd);
    listeners.forEach(l -> l.multipleChecksAdded(massAddFinal));

    ImmutableMap<ItemCheck, ItemCheck> massReplacementFinal = ImmutableMap.copyOf(massReplacement);
    listeners.forEach(l -> l.multipleChecksReplaced(massReplacementFinal));
  }

  public ItemCheck get(CheckId id) {
    return checksById.get(id);
  }

  public Stream<ItemCheck> allChecks() {
    return checksById.values().stream();
  }

  private final Set<ItemCheck> allChecksSet = Collections.unmodifiableSet(checksById.values());

  public Set<ItemCheck> allChecksSet() {
    return allChecksSet;
  }

  public Stream<Item> allItems() {
    return itemsByName.values().stream();
  }

  public Stream<ItemCheck> startChecks() {
    return idsByLocation.getValue("Start").stream().map(checksById::get);
  }

  public Item kingsPassTransition() throws ICDLException {
    return getItem(Term.create("Tutorial_01[top1]"));
  }

  public Item nothing() throws ICDLException {
    return getItem(Term.nothing());
  }

  public Stream<ItemCheck> getByCondition(Condition c) {
    return idsByCondition.getValue(c).stream().map(checksById::get);
  }

  public JsonObject toJson() {
    JsonObject obj = new JsonObject();
    JsonArray arr = new JsonArray();
    checksById.values().forEach(c -> arr.add(c.toJson()));
    obj.add("checks", arr);
    return obj;
  }

  public void fromJson(JsonObject obj) throws ICDLException {
    // Remove all item checks
    ImmutableSet<CheckId> ids = ImmutableSet.copyOf(checksById.keySet());
    ids.forEach(this::removeInternal);

    JsonArray arr = obj.get("checks").getAsJsonArray();
    for (JsonElement e : arr) {
      addInternal(ItemCheck.fromJson(this, e.getAsJsonObject()));
    }
  }

  private void parseCheckUnsafe(JsonElement elem, RoomLabels rooms, boolean vanilla)
      throws ParseException {
    Item item = Item.parse(elem.getAsJsonObject().get("Item").getAsJsonObject());
    if (!item.types().contains("RandomizerMod.RC.SplitCloakItem")
        && !item.types().contains("RandomizerCore.Logic.LogicTransition")
        && item.types().stream().noneMatch(s -> s.startsWith("RandomizerCore.LogicItems."))) {
      return;
    }

    JsonObject locObj = elem.getAsJsonObject().get("Location").getAsJsonObject();
    Location loc = Location.parse(rooms, locObj, item.isTransition());

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (costsObj != null && !costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    placeNew(loc, item, costs, vanilla);
  }

  private void parseCheckSafe(JsonElement elem, RoomLabels rooms, boolean vanilla)
      throws ParseException {
    try {
      parseCheckUnsafe(elem, rooms, vanilla);
    } catch (Exception ex) {
      throw new ParseException(ex.getMessage() + ": " + elem, ex);
    }
  }

  private void parseTransitionUnsafe(JsonElement elem, RoomLabels rooms) throws ParseException {
    JsonObject target = elem.getAsJsonObject().get("Target").getAsJsonObject();
    JsonObject source = elem.getAsJsonObject().get("Source").getAsJsonObject();

    Item item = Item.parse(target);
    Location loc = Location.parse(rooms, source.get("lt").getAsJsonObject(), true);

    placeNew(loc, item, Costs.none(), false);
  }

  private void parseTransitionSafe(JsonElement elem, RoomLabels rooms) throws ParseException {
    try {
      parseTransitionUnsafe(elem, rooms);
    } catch (Exception ex) {
      throw new ParseException(ex.getMessage() + ": " + elem, ex);
    }
  }

  public static ItemChecks parse(JsonObject json, RoomLabels roomLabels) throws ParseException {
    ItemChecks checks = new ItemChecks();

    // Parse locations.
    for (JsonElement elem : json.get("itemPlacements").getAsJsonArray()) {
      checks.parseCheckSafe(elem, roomLabels, false);
    }
    JsonElement transitionPlacements = json.get("transitionPlacements");
    if (!transitionPlacements.isJsonNull()) {
      for (JsonElement elem : transitionPlacements.getAsJsonArray()) {
        checks.parseTransitionSafe(elem, roomLabels);
      }
    }
    for (JsonElement elem : json.get("Vanilla").getAsJsonArray()) {
      checks.parseCheckSafe(elem, roomLabels, true);
    }

    checks.calculateOriginalItemCounts();
    return checks;
  }
}
