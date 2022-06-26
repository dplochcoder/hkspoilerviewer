package hollow.knight.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.main.Main;

/** Mostly immutable context for a State object. */
public final class StateContext {

  private final boolean isHKS;
  private final JsonObject rawSpoilerJson;
  private final JsonObject icdlJson;
  private final CharmIds charmIds;
  private final RoomLabels roomLabels;
  private final Pools pools;
  private final NotchCosts notchCosts;
  private final Waypoints waypoints;
  private final ItemChecks checks;

  private final MutableTermMap tolerances;
  private final ImmutableTermMap setters;

  public StateContext(boolean isHKS, JsonObject rawSpoilerJson, JsonObject icdlJson,
      CharmIds charmIds, RoomLabels roomLabels, Pools pools, NotchCosts notchCosts,
      Waypoints waypoints, ItemChecks checks, TermMap tolerances, TermMap setters) {
    this.isHKS = isHKS;
    this.rawSpoilerJson = rawSpoilerJson;
    this.icdlJson = icdlJson;
    this.charmIds = charmIds;
    this.roomLabels = roomLabels;
    this.pools = pools;
    this.notchCosts = notchCosts;
    this.waypoints = waypoints;
    this.checks = checks;
    this.tolerances = new MutableTermMap(tolerances);
    this.setters = ImmutableTermMap.copyOf(setters);
  }

  public boolean isHKS() {
    return isHKS;
  }

  public JsonObject rawSpoilerJson() {
    return rawSpoilerJson;
  }

  public JsonObject icdlJson() {
    return icdlJson;
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

  public boolean isMutableTerm(Term term) {
    return waypoints.allWaypoints().contains(term) || checks.isEffectTerm(term);
  }

  public TermMap tolerances() {
    return tolerances;
  }

  public void setTolerances(TermMap newTolerances) {
    tolerances.clear();
    tolerances.add(newTolerances);
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
    obj.add("ICDLTolerances", tolerances.toJson());
  }

  public void loadMutables(JsonObject obj) throws ICDLException {
    if (!obj.has("ICDLItemChecks")) {
      return;
    }

    checks().fromJson(obj.get("ICDLItemChecks").getAsJsonObject());
    notchCosts().parse(obj.get("ICDLNotchCosts").getAsJsonObject());
    setTolerances(TermMap.fromJson(obj.get("ICDLTolerances").getAsJsonObject()));
  }

  public static StateContext parse(boolean isHKS, JsonObject rawSpoilerJson, JsonObject icdlJson)
      throws ParseException {
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

    return new StateContext(isHKS, rawSpoilerJson, icdlJson, charmIds, rooms, pools, notchCosts,
        Waypoints.parse(rawSpoilerJson), ItemChecks.parse(rawSpoilerJson, rooms), tolerances,
        setters);
  }

  private static ImmutableSet<String> getTypes(JsonObject tag) {
    return Arrays.stream(tag.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());
  }

  private static void sanitizeTags(JsonObject obj, Set<String> filterTags) {
    JsonArray newArr = new JsonArray();
    if (obj.has("tags") && obj.get("tags").isJsonArray()) {
      for (JsonElement tag : obj.get("tags").getAsJsonArray()) {
        ImmutableSet<String> tagTypes = getTypes(tag.getAsJsonObject());
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

  private boolean hasInherentCost(JsonObject obj) {
    if (!obj.has("Location")) {
      return false;
    }

    JsonElement tags = obj.get("Location").getAsJsonObject().get("tags");
    if (tags.isJsonNull()) {
      return false;
    }

    for (JsonElement tag : tags.getAsJsonArray()) {
      if (tag.getAsJsonObject().has("Inherent")
          && tag.getAsJsonObject().get("Inherent").getAsBoolean()) {
        return true;
      }
    }

    return false;
  }

  private JsonObject calculatePlacementsJson(Map<Term, JsonObject> itemJsons,
      Map<String, JsonObject> locationJsons) throws ICDLException {
    JsonObject placements = new JsonObject();

    // Assign ItemSync ids.
    Multiset<String> assignedSyncIds = HashMultiset.create();
    Map<ItemCheck, String> syncIds = new HashMap<>();
    checks().allChecks().forEach(c -> {
      String id = c.location().name() + ";" + c.item().term().name();
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

        Costs costs = costsSet.iterator().next();
        if (hasInherentCost(locObj) && !costs.isNone()) {
          throw new ICDLException("Error: Location '" + loc.name()
              + "' has an inherent cost. It cannot be assigned a separate cost");
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
        JsonObject itemObj = check.item().isCustom() ? check.item().toICDLJson()
            : itemJsons.get(check.item().term()).deepCopy();
        tagsArr =
            itemObj.has("tags") ? itemObj.get("tags").getAsJsonArray().deepCopy() : new JsonArray();

        if (loc.isShop()) {
          JsonObject costTag = new JsonObject();
          costTag.addProperty("$type", "ItemChanger.CostTag, ItemChanger");
          costTag.add("Cost", check.costs().toICDLJson());
          tagsArr.add(costTag);
        }

        JsonObject randoTag = new JsonObject();
        randoTag.addProperty("$type", "RandomizerMod.IC.RandoItemTag, RandomizerMod");
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

        itemObj.add("tags", tagsArr);
        itemsArr.add(itemObj);
      }
      locObj.add("Items", itemsArr);

      placements.add(locName, locObj);
    }

    return placements;
  }

  private JsonObject withICDLCharmCosts(JsonObject json) {
    JsonArray modules = json.get("Modules").getAsJsonArray();
    JsonArray newModules = new JsonArray();
    for (JsonElement module : modules) {
      ImmutableSet<String> types = getTypes(module.getAsJsonObject());
      if (types.contains("ItemChanger.Modules.PlayerDataEditModule")) {
        JsonArray origEdits = module.getAsJsonObject().get("PDEdits").getAsJsonArray();
        JsonArray newEdits = new JsonArray();
        for (JsonElement edit : origEdits) {
          JsonObject editObj = edit.getAsJsonObject();
          if (!editObj.get("FieldName").getAsString().startsWith("charmCost_")) {
            newEdits.add(editObj);
          }
        }

        for (int i = 0; i < notchCosts().costs().size(); i++) {
          JsonObject notchCost = new JsonObject();
          notchCost.addProperty("Value", notchCosts().notchCost(i + 1));
          notchCost.addProperty("FieldName", "charmCost_" + (i + 1));
          newEdits.add(notchCost);
        }

        JsonObject newModule = module.getAsJsonObject().deepCopy();
        newModule.add("PDEdits", newEdits);
        newModules.add(newModule);
      } else {
        newModules.add(module);
      }
    }

    JsonObject mods = json.deepCopy();
    mods.add("Modules", newModules);
    return mods;
  }

  private JsonArray createNewSpoilerPlacements(JsonArray origPlacements) throws ICDLException {
    Map<Term, JsonObject> itemsJson = new HashMap<>();
    Map<String, JsonObject> locationsJson = new HashMap<>();

    for (JsonElement elem : origPlacements) {
      JsonObject obj = elem.getAsJsonObject();
      JsonObject item = obj.get("Item").getAsJsonObject();
      itemsJson.put(Term.create(item.get("Name").getAsString()), item);

      JsonObject loc = obj.get("Location").getAsJsonObject().deepCopy();
      String locName =
          (loc.has("logic") ? loc.get("logic").getAsJsonObject() : loc).get("Name").getAsString();
      loc.add("costs", JsonNull.INSTANCE);
      locationsJson.put(locName, loc);
    }

    JsonArray arr = new JsonArray();
    ImmutableList<ItemCheck> checks = checks().allChecks().filter(c -> !c.vanilla())
        .sorted(Comparator.comparing(c -> c.id().id())).collect(ImmutableList.toImmutableList());
    for (int i = 0; i < checks.size(); i++) {
      ItemCheck c = checks.get(i);
      if (c.id().id() != i) {
        throw new ICDLException("Placement indices don't match ids");
      }

      JsonObject placement = new JsonObject();
      placement.add("Item", itemsJson.get(c.item().term()));

      JsonObject locObj = locationsJson.get(c.location().name()).deepCopy();
      locObj.add("costs", c.costs().toRawSpoilerJson());
      placement.add("Location", locObj);

      placement.addProperty("Index", i);
      arr.add(placement);
    }

    return arr;
  }

  private JsonObject updateInitialProgression(JsonObject initialProgression) {
    JsonObject obj = initialProgression.deepCopy();
    JsonArray setters = obj.get("Setters").getAsJsonArray();

    JsonArray newSetters = new JsonArray();
    for (JsonElement setter : setters) {
      JsonObject sObj = setter.getAsJsonObject().deepCopy();
      Term t = Term.create(sObj.get("Term").getAsString());
      if (Term.costTerms().contains(t)) {
        sObj.addProperty("Value", tolerances().get(t));
      }
      newSetters.add(sObj);
    }
    obj.add("Setters", newSetters);
    return obj;
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
    newICDLJson.add("mods", withICDLCharmCosts(newICDLJson.get("mods").getAsJsonObject()));

    JsonObject newRawSpoilerJson = rawSpoilerJson.deepCopy();
    newRawSpoilerJson.add("itemPlacements",
        createNewSpoilerPlacements(rawSpoilerJson.get("itemPlacements").getAsJsonArray()));
    newRawSpoilerJson.add("notchCosts", notchCosts().toRawSpoilerJsonArray());
    newRawSpoilerJson.add("InitialProgression",
        updateInitialProgression(newRawSpoilerJson.get("InitialProgression").getAsJsonObject()));

    String packName = JOptionPane.showInputDialog(null, "Name?");
    if (packName == null || packName.trim().isEmpty()) {
      throw new ICDLException("Must enter name");
    }
    String packDesc = JOptionPane.showInputDialog(null, "Description?");
    if (packDesc == null || packDesc.trim().isEmpty()) {
      packDesc = "Hollow Knight Plando (" + packName.trim() + ")";
    }

    JsonObject packJson = new JsonObject();
    packJson.addProperty("Author", "HKSPoilerViewer v" + Main.version() + ", "
        + LocalDate.now().format(DateTimeFormatter.ISO_DATE));
    packJson.addProperty("Name", packName.trim());
    packJson.addProperty("Description", packDesc.trim());
    packJson.addProperty("SupportsRandoTracking", true);

    Files.createDirectories(p);
    Path icdlPath = Paths.get(p.toString(), "ic.json");
    JsonUtil.writeJson(icdlPath.toString(), newICDLJson);

    Path packPath = Paths.get(p.toString(), "pack.json");
    JsonUtil.writeJson(packPath.toString(), packJson);

    Path ctxPath = Paths.get(p.toString(), "ctx.json");
    JsonUtil.writeJson(ctxPath.toString(), newRawSpoilerJson);
  }

}
