package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** A mutable mapping of locations to items. */
public final class ItemChecks {
  private final Map<CheckId, ItemCheck> placementsById = new HashMap<>();
  private final Multimap<Condition, CheckId> idsByCondition =
      MultimapBuilder.hashKeys().hashSetValues().build();
  private final Multimap<String, CheckId> idsByLocation =
      MultimapBuilder.hashKeys().hashSetValues().build();

  private long nextId = 1;

  public ItemChecks() {}

  private CheckId newId() {
    return CheckId.of(nextId++);
  }

  public CheckId placeNew(Location loc, Item item, Costs costs, boolean vanilla) {
    CheckId id = newId();
    ItemCheck p = ItemCheck.create(id, loc, item, costs, vanilla);
    placementsById.put(id, p);
    idsByCondition.put(p.condition(), id);
    idsByLocation.put(loc.name(), id);

    return id;
  }

  public void clear() {
    placementsById.clear();
    idsByCondition.clear();
    idsByLocation.clear();
    nextId = 1;
  }

  public ItemCheck get(CheckId id) {
    return placementsById.get(id);
  }

  public Stream<ItemCheck> allChecks() {
    return placementsById.values().stream();
  }

  public Stream<ItemCheck> startChecks() {
    return idsByLocation.get("Start").stream().map(placementsById::get);
  }

  public Stream<ItemCheck> getByCondition(Condition c) {
    return idsByCondition.get(c).stream().map(placementsById::get);
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

  public void parse(JsonObject json, ConditionParser.Context parseCtx, RoomLabels roomLabels)
      throws ParseException {
    clear();

    // Parse locations.
    for (JsonElement elem : json.get("itemPlacements").getAsJsonArray()) {
      try {
        parseCheck(elem, parseCtx, roomLabels, false);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }

    for (JsonElement elem : json.get("Vanilla").getAsJsonArray()) {
      try {
        parseCheck(elem, parseCtx, roomLabels, true);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }
  }
}
