package hollow.knight.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableList;
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

  public CheckId placeNew(Location loc, Item item, Costs costs, boolean vanilla) {
    CheckId id = newId();
    ItemCheck p = ItemCheck.create(id, loc, item, costs, vanilla);
    checksById.put(id, p);
    idsByCondition.put(p.condition(), id);
    idsByLocation.put(loc.name(), id);

    listeners().forEach(l -> l.checkAdded(p));
    return id;
  }

  public CheckId replace(CheckId prevId, Location loc, Item item, Costs costs, boolean vanilla) {
    CheckId id = newId();
    ItemCheck before = checksById.get(prevId);
    ItemCheck after = ItemCheck.create(id, loc, item, costs, vanilla);

    checksById.remove(prevId);
    idsByCondition.removeValue(prevId);
    idsByLocation.removeValue(prevId);
    checksById.put(id, after);
    idsByCondition.put(after.condition(), id);
    idsByLocation.put(loc.name(), id);

    listeners().forEach(l -> l.checkReplaced(before, after));
    return id;
  }

  public void remove(CheckId id) {
    ItemCheck check = checksById.get(id);
    checksById.remove(id);
    idsByCondition.removeValue(id);
    idsByLocation.removeValue(id);

    listeners().forEach(l -> l.checkRemoved(check));
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

  public Stream<ItemCheck> startChecks() {
    return idsByLocation.getKey("Start").stream().map(checksById::get);
  }

  public Stream<ItemCheck> getByCondition(Condition c) {
    return idsByCondition.getKey(c).stream().map(checksById::get);
  }

  private void parseCheck(JsonElement elem, ConditionParser.Context parseCtx, RoomLabels rooms,
      boolean vanilla) throws ParseException {
    Item item = Item.parse(parseCtx, elem.getAsJsonObject().get("Item").getAsJsonObject());
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
    Condition locAccess = ConditionParser.parse(parseCtx, logicObj.get("Logic").getAsString());
    Location loc = Location.create(rooms, locName, locAccess);

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (costsObj != null && !costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    placeNew(loc, item, costs, vanilla);
  }

  public static ItemChecks parse(JsonObject json, ConditionParser.Context parseCtx,
      RoomLabels roomLabels) throws ParseException {
    ItemChecks checks = new ItemChecks();

    // Parse locations.
    for (JsonElement elem : json.get("itemPlacements").getAsJsonArray()) {
      try {
        checks.parseCheck(elem, parseCtx, roomLabels, false);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }
    for (JsonElement elem : json.get("Vanilla").getAsJsonArray()) {
      try {
        checks.parseCheck(elem, parseCtx, roomLabels, true);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }

    return checks;
  }
}
