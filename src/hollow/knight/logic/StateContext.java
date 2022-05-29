package hollow.knight.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import hollow.knight.util.JsonUtil;

/** Mostly immutable context for a State object. */
public final class StateContext {

  private final JsonObject rawSpoilerJson;
  private final JsonObject icdlJson;
  private final JsonObject packJson;
  private final CharmIds charmIds;
  private final RoomLabels roomLabels;
  private final Pools pools;
  private final NotchCosts notchCosts;
  private final Waypoints waypoints;
  private final ItemChecks checks;

  private final ImmutableTermMap tolerances;
  private final ImmutableTermMap setters;

  public StateContext(JsonObject rawSpoilerJson, JsonObject icdlJson, JsonObject packJson,
      CharmIds charmIds, RoomLabels roomLabels, Pools pools, NotchCosts notchCosts,
      Waypoints waypoints, ItemChecks checks, TermMap tolerances, TermMap setters) {
    this.rawSpoilerJson = rawSpoilerJson;
    this.icdlJson = icdlJson;
    this.packJson = packJson;
    this.charmIds = charmIds;
    this.roomLabels = roomLabels;
    this.pools = pools;
    this.notchCosts = notchCosts;
    this.waypoints = waypoints;
    this.checks = checks;
    this.tolerances = ImmutableTermMap.copyOf(tolerances);
    this.setters = ImmutableTermMap.copyOf(setters);
  }

  public JsonObject rawSpoilerJson() {
    return rawSpoilerJson;
  }

  public JsonObject icdlJson() {
    return icdlJson;
  }

  public JsonObject packJson() {
    return packJson;
  }

  public CharmIds charmIds() {
    return charmIds;
  }

  public RoomLabels roomLabels() {
    return roomLabels;
  }

  public Pools pools() {
    return pools;
  }

  public NotchCosts notchCosts() {
    return notchCosts;
  }

  public Waypoints waypoints() {
    return waypoints;
  }

  public ItemChecks checks() {
    return checks;
  }

  public ImmutableTermMap tolerances() {
    return tolerances;
  }

  public State newInitialState() {
    State state = new State(this);

    // Automatically acquire all items at Start
    checks.startChecks().forEach(state::acquireCheck);
    for (Term t : setters.terms()) {
      state.set(t, setters.get(t));
    }

    state.normalize();
    return state;
  }

  public void saveMutables(JsonObject obj) {
    obj.add("ICDLItemChecks", checks().toJson());
    obj.add("ICDLNotchCosts", notchCosts().toJson());
  }

  public void loadMutables(JsonObject obj) {
    if (obj.has("ICDLItemChecks")) {
      checks().fromJson(obj.get("ICDLItemChecks").getAsJsonObject());
      notchCosts().parse(obj.get("ICDLNotchCosts").getAsJsonObject());
    }
  }

  public static StateContext parse(JsonObject rawSpoilerJson, JsonObject icdlJson,
      JsonObject packJson) throws ParseException {
    CharmIds charmIds = CharmIds.load();
    RoomLabels rooms = RoomLabels.load();
    Pools pools = Pools.load();

    MutableTermMap setters = new MutableTermMap();
    MutableTermMap tolerances = new MutableTermMap();

    JsonArray jsonSetters =
        rawSpoilerJson.get("InitialProgression").getAsJsonObject().get("Setters").getAsJsonArray();
    for (JsonElement termValue : jsonSetters) {
      JsonObject obj = termValue.getAsJsonObject();

      Term term = Term.create(obj.get("Term").getAsString());
      int value = obj.get("Value").getAsInt();
      if (Term.costTerms().contains(term)) {
        tolerances.set(term, value);
      } else {
        setters.set(term, value);
      }
    }

    NotchCosts notchCosts = new NotchCosts();
    notchCosts.parse(rawSpoilerJson);

    return new StateContext(rawSpoilerJson, icdlJson, packJson, charmIds, rooms, pools, notchCosts,
        Waypoints.parse(rawSpoilerJson), ItemChecks.parse(rawSpoilerJson, rooms), tolerances,
        setters);
  }

  private static ImmutableSet<String> getTagTypes(JsonObject tag) {
    return Arrays.stream(tag.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static void sanitizeTags(JsonObject obj, Set<String> filterTags) {
    JsonArray newArr = new JsonArray();
    if (obj.has("tags") && obj.get("tags").isJsonArray()) {
      for (JsonElement tag : obj.get("tags").getAsJsonArray()) {
        ImmutableSet<String> tagTypes = getTagTypes(tag.getAsJsonObject());
        if (Sets.intersection(tagTypes, filterTags).isEmpty()) {
          newArr.add(tag);
        }
      }
    }

    obj.add("tags", newArr);
  }

  private static final ImmutableSet<String> ITEM_TAGS_TO_SANITIZE = ImmutableSet.of(
      "ItemChanger.CostTag", "RandomizerMod.IC.RandoItemTag", "ItemSyncMod.Items.SyncedItemTag");

  private static void sanitizeItem(JsonObject item) {
    sanitizeTags(item, ITEM_TAGS_TO_SANITIZE);
  }

  private static final ImmutableSet<String> LOCATION_TAGS_TO_SANITIZE =
      ImmutableSet.of("RandomizerMod.IC.RandoPlacementTag");

  private static void sanitizeLocation(JsonObject location) {
    location.add("Cost", JsonNull.INSTANCE);
    sanitizeTags(location, LOCATION_TAGS_TO_SANITIZE);
  }

  private JsonObject calculatePlacementsJson(Map<Term, JsonObject> itemJsons,
      Map<String, JsonObject> locationJsons) throws ICDLException {
    JsonObject placements = new JsonObject();

    // Assign ItemSync ids.
    Multiset<String> assignedSyncIds = HashMultiset.create();
    Map<ItemCheck, String> syncIds = new HashMap<>();
    checks().allChecks().forEach(c -> {
      String id = c.location().name() + ";" + c.item().term();
      int prev = assignedSyncIds.add(id, 1);
      syncIds.put(c, id + ((prev > 0) ? String.valueOf(prev + 1) : ""));
    });


    // Group ItemChecks by Location.
    Multimap<String, ItemCheck> checksByLocation = HashMultimap.create();
    checks().allChecks().filter(c -> !c.vanilla())
        .forEach(c -> checksByLocation.put(c.location().name(), c));

    // Output by location.
    for (String locName : checksByLocation.keySet()) {
      JsonObject locObj = locationJsons.get(locName).getAsJsonObject().deepCopy();

      Location loc = checks().getLocation(locName);
      if (!loc.isShop()) {
        Set<Costs> costsSet = checksByLocation.get(locName).stream().map(c -> c.costs())
            .collect(ImmutableSet.toImmutableSet());
        if (costsSet.size() != 1) {
          throw new ICDLException("Error: Multiple items at " + locName
              + " have differing costs. This is only supported for shops.");
        }

        locObj.add("Cost", costsSet.iterator().next().toICDLJson());
      }

      // Add rando placement tags.
      JsonArray tagsArr = locObj.get("tags").getAsJsonArray().deepCopy();
      JsonObject pTags = new JsonObject();
      pTags.addProperty("$type", "RandomizerMod.IC.RandoPlacementTag, RandomizerMod");
      JsonArray ids = new JsonArray();
      checksByLocation.get(locName).stream().mapToInt(c -> c.id().id()).forEach(ids::add);
      pTags.add("ids", ids);
      tagsArr.add(pTags);
      locObj.add("tags", tagsArr);

      // Add items
      JsonArray itemsArr = new JsonArray();
      for (ItemCheck check : checksByLocation.get(locName)) {
        // TODO: Handle custom geo + essence
        JsonObject itemObj = itemJsons.get(check.item().term()).deepCopy();
        tagsArr = itemObj.get("tags").getAsJsonArray().deepCopy();

        if (loc.isShop()) {
          JsonObject costTag = new JsonObject();
          costTag.addProperty("$type", "ItemChanger.CostTag, ItemChanger");
          costTag.add("Cost", check.costs().toICDLJson());
          tagsArr.add(costTag);
        }

        JsonObject randoTag = new JsonObject();
        randoTag.addProperty("$type", "RandomizerMode.IC.RandoItemTag, RandomizerMod");
        randoTag.addProperty("id", check.id().id());
        randoTag.addProperty("obtained", false);
        tagsArr.add(randoTag);

        // TODO: Only do this if ItemSync is enabled? Maybe?
        JsonObject syncTag = new JsonObject();
        syncTag.addProperty("$type", "ItemSyncMod.Items.SyncedItemTag, ItemSync");
        syncTag.addProperty("ItemID", syncIds.get(check));
        syncTag.add("From", JsonNull.INSTANCE);
        syncTag.addProperty("Given", false);
        syncTag.addProperty("WasObtainedLocallySet", false);
        syncTag.addProperty("WasObtainedLocally", false);
        syncTag.addProperty("GetWasObtainedLocally", false);
        syncTag.addProperty("Message", "SyncedItemTag");
        tagsArr.add(syncTag);

        itemsArr.add(itemObj);
      }
      locObj.add("Items", itemsArr);

      placements.add(locName, locObj);
    }

    return placements;
  }

  public void saveICDL(Path p) throws IOException, ICDLException {
    // Sanitize items and locations into maps.
    Map<Term, JsonObject> itemJsons = new HashMap<>();
    Map<String, JsonObject> locationJsons = new HashMap<>();
    JsonObject placements = icdlJson.get("Placements").getAsJsonObject();
    for (String locName : placements.keySet()) {
      JsonObject locJson = placements.get(locName).getAsJsonObject().deepCopy();
      for (JsonElement item : locJson.get("Items").getAsJsonArray()) {
        JsonObject itemObj = item.getAsJsonObject().deepCopy();
        String itemName = itemObj.get("name").getAsString();
        sanitizeItem(itemObj);
        itemJsons.put(Term.create(itemName), itemObj);
      }
      sanitizeLocation(locJson);
      locationJsons.put(locName, locJson);
    }

    JsonObject newICDLJson = icdlJson.deepCopy();
    newICDLJson.add("Placements", calculatePlacementsJson(itemJsons, locationJsons));

    Files.createDirectories(p);
    Path icdlPath = Paths.get(p.toString(), "ic.json");
    JsonUtil.writeJson(icdlPath.toString(), newICDLJson);

    // TODO: Rewrite pack json
    Path packPath = Paths.get(p.toString(), "pack.json");
    JsonUtil.writeJson(packPath.toString(), packJson);

    // TODO: Actually update RawSpoiler.
    Path ctxPath = Paths.get(p.toString(), "ctx.json");
    JsonUtil.writeJson(ctxPath.toString(), rawSpoilerJson);
  }

}
