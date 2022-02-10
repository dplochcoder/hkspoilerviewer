package hollow.knight.logic;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Items {
  private final CharmIds charmIds;
  private final ImmutableSet<ItemCheck> allItemChecks;
  private final ImmutableList<Integer> notchCosts;

  private Items(CharmIds charmIds, Set<ItemCheck> items, List<Integer> notchCosts) {
    this.charmIds = charmIds;
    this.allItemChecks = ImmutableSet.copyOf(items);
    this.notchCosts = ImmutableList.copyOf(notchCosts);
  }

  public int notchCost(int charmId) {
    return notchCosts.get(charmId - 1);
  }

  public CharmIds charmIds() {
    return charmIds;
  }

  public ImmutableSet<ItemCheck> allItemChecks() {
    return allItemChecks;
  }

  private static void parseItem(JsonElement elem, String itemField, String locField,
      RoomLabels rooms, Map<Term, Item> itemsByName, boolean vanilla, Set<ItemCheck> items)
      throws ParseException {
    JsonObject itemObj = elem.getAsJsonObject().get(itemField).getAsJsonObject();

    Set<String> types = Arrays.stream(itemObj.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());
    if (!types.contains("RandomizerMod.RC.RandoModItem")
        && !types.contains("RandomizerMod.RC.PlaceholderItem")) {
      return;
    }

    Term itemName = Term.create(itemObj.get("Name").getAsString());

    JsonObject locObj = elem.getAsJsonObject().get(locField).getAsJsonObject();
    JsonObject logicObj = locObj.get("logic").getAsJsonObject();
    String locName = logicObj.get("name").getAsString();
    Condition locAccess = ConditionParser.parse(logicObj.get("logic").getAsString());

    Item item = itemsByName.get(itemName);
    if (item == null) {
      return;
    }

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (!costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    items.add(new ItemCheck(new Location(locName, locAccess, rooms), item, costs, vanilla));
  }

  public static Items parse(JsonObject json, RoomLabels rooms) throws ParseException {
    Set<ItemCheck> items = new HashSet<>();

    // Parse item effects.
    JsonArray itemsArr = json.get("LM").getAsJsonObject().get("Items").getAsJsonArray();
    Map<Term, Item> itemsByName = new HashMap<>();
    for (JsonElement elem : itemsArr) {
      Item item = Item.parse(elem.getAsJsonObject());
      itemsByName.put(item.term(), item);
    }

    // Parse locations.
    JsonArray itemPlacements = json.get("itemPlacements").getAsJsonArray();
    JsonArray vanillaPlacements = json.get("Vanilla").getAsJsonArray();
    for (JsonElement elem : itemPlacements) {
      parseItem(elem, "item", "location", rooms, itemsByName, false, items);
    }
    for (JsonElement elem : vanillaPlacements) {
      parseItem(elem, "Item", "Location", rooms, itemsByName, true, items);
    }

    // Parse notch costs.
    ImmutableList.Builder<Integer> notchCosts = ImmutableList.builder();
    for (JsonElement elem : json.get("notchCosts").getAsJsonArray()) {
      notchCosts.add(elem.getAsInt());
    }

    return new Items(CharmIds.load(), items, notchCosts.build());
  }
}
