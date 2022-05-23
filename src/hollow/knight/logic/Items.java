package hollow.knight.logic;

import java.util.Map;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Items {
  private final CharmIds charmIds;
  private final ImmutableBiMap<Integer, ItemCheck> itemChecks;
  private final ImmutableMultimap<Condition, ItemCheck> checksByCondition;
  private final ImmutableSet<ItemCheck> startItems;

  private Items(CharmIds charmIds, BiMap<Integer, ItemCheck> itemChecks) {
    this.charmIds = charmIds;
    this.itemChecks = ImmutableBiMap.copyOf(itemChecks);
    this.checksByCondition = Multimaps.index(itemChecks.values(), ItemCheck::condition);
    this.startItems =
        itemChecks.values().stream().filter(c -> c.location().scene().contentEquals("Start"))
            .collect(ImmutableSet.toImmutableSet());
  }

  public ItemCheck get(int id) {
    return itemChecks.get(id);
  }

  public ImmutableCollection<ItemCheck> getByCondition(Condition c) {
    return checksByCondition.get(c);
  }

  public CharmIds charmIds() {
    return charmIds;
  }

  public ImmutableSet<ItemCheck> allItemChecks() {
    return itemChecks.values();
  }

  public ImmutableSet<ItemCheck> startItems() {
    return startItems;
  }

  private static void parseItem(int id, JsonElement elem, ConditionParser.Context parseCtx,
      RoomLabels rooms, boolean vanilla, Map<Integer, ItemCheck> items) throws ParseException {
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

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (costsObj != null && !costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    items.put(id, new ItemCheck(id, new Location(locName, locAccess, rooms), item, costs, vanilla));
  }

  public static Items parse(JsonObject json, ConditionParser.Context parseCtx, RoomLabels rooms)
      throws ParseException {
    BiMap<Integer, ItemCheck> items = HashBiMap.create();

    // Parse locations.
    JsonArray itemPlacements = json.get("itemPlacements").getAsJsonArray();
    JsonArray vanillaPlacements = json.get("Vanilla").getAsJsonArray();
    int id = 1;
    for (JsonElement elem : itemPlacements) {
      try {
        parseItem(id++, elem, parseCtx, rooms, false, items);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }
    for (JsonElement elem : vanillaPlacements) {
      try {
        parseItem(id++, elem, parseCtx, rooms, true, items);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }

    return new Items(CharmIds.load(), items);
  }
}
