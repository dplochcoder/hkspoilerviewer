package hollow.knight.logic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.TreeMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Items {
  private final CharmIds charmIds;
  private final ImmutableBiMap<Integer, ItemCheck> itemChecks;
  private final ImmutableSet<ItemCheck> startItems;
  private final ImmutableList<Integer> notchCosts;

  private final ImmutableSetMultimap<Term, ItemCheck> locationInfluences;

  // TreeMultimap has no immutable version, so we never allow it to be modified after construction.
  private final ImmutableMap<Term, TreeMultimap<Integer, ItemCheck>> shopChecks;

  private static ImmutableSetMultimap<Term, ItemCheck> buildLocationInfluences(
      Iterable<ItemCheck> checks) {
    ImmutableSetMultimap.Builder<Term, ItemCheck> builder = ImmutableSetMultimap.builder();

    for (ItemCheck itemCheck : checks) {
      itemCheck.location().terms().forEach(t -> builder.put(t, itemCheck));
    }

    return builder.build();
  }

  private static ImmutableMap<Term, TreeMultimap<Integer, ItemCheck>> buildShopChecks(
      Iterable<ItemCheck> checks) {
    Map<Term, TreeMultimap<Integer, ItemCheck>> map = new HashMap<>();

    for (ItemCheck itemCheck : checks) {
      Optional<Term> costTerm = Term.costTerms().stream().filter(itemCheck.costs()::hasCostTerm)
          .collect(MoreCollectors.toOptional());
      if (!costTerm.isPresent()) {
        continue;
      }

      Term term = costTerm.get();
      TreeMultimap<Integer, ItemCheck> tree =
          map.computeIfAbsent(term, t -> TreeMultimap.<Integer, ItemCheck>create());
      tree.put(itemCheck.costs().getCostTerm(term), itemCheck);
    }

    return ImmutableMap.copyOf(map);
  }

  private Items(CharmIds charmIds, BiMap<Integer, ItemCheck> itemChecks, List<Integer> notchCosts) {
    this.charmIds = charmIds;
    this.itemChecks = ImmutableBiMap.copyOf(itemChecks);
    this.startItems =
        itemChecks.values().stream().filter(c -> c.location().scene().contentEquals("Start"))
            .collect(ImmutableSet.toImmutableSet());
    this.notchCosts = ImmutableList.copyOf(notchCosts);
    this.locationInfluences = buildLocationInfluences(itemChecks.values());
    this.shopChecks = buildShopChecks(itemChecks.values());
  }

  public ItemCheck get(int id) {
    return itemChecks.get(id);
  }

  public int notchCost(int charmId) {
    return notchCosts.get(charmId - 1);
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

  public ImmutableSet<ItemCheck> influences(Term term) {
    return locationInfluences.get(term);
  }

  public Stream<ItemCheck> costChecks(Term cost, int lowExclusive, int highInclusive) {
    TreeMultimap<Integer, ItemCheck> tree = shopChecks.get(cost);
    if (tree == null) {
      return Stream.empty();
    }

    return tree.asMap().subMap(lowExclusive + 1, highInclusive + 1).values().stream()
        .flatMap(c -> c.stream());
  }

  private static void parseItem(int id, JsonElement elem, RoomLabels rooms, boolean vanilla,
      Map<Integer, ItemCheck> items) throws ParseException {
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

    Costs costs = Costs.none();
    JsonElement costsObj = locObj.get("costs");
    if (costsObj != null && !costsObj.isJsonNull()) {
      costs = Costs.parse(costsObj.getAsJsonArray());
    }

    items.put(id, new ItemCheck(id, new Location(locName, locAccess, rooms), item, costs, vanilla));
  }

  public static Items parse(JsonObject json, RoomLabels rooms) throws ParseException {
    BiMap<Integer, ItemCheck> items = HashBiMap.create();

    // Parse locations.
    JsonArray itemPlacements = json.get("itemPlacements").getAsJsonArray();
    JsonArray vanillaPlacements = json.get("Vanilla").getAsJsonArray();
    int id = 1;
    for (JsonElement elem : itemPlacements) {
      try {
        parseItem(id++, elem, rooms, false, items);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }
    for (JsonElement elem : vanillaPlacements) {
      try {
        parseItem(id++, elem, rooms, true, items);
      } catch (Exception ex) {
        throw new ParseException(ex.getMessage() + ": " + elem, ex);
      }
    }

    // Parse notch costs.
    ImmutableList.Builder<Integer> notchCosts = ImmutableList.builder();
    for (JsonElement elem : json.get("notchCosts").getAsJsonArray()) {
      notchCosts.add(elem.getAsInt());
    }

    return new Items(CharmIds.load(), items, notchCosts.build());
  }
}
