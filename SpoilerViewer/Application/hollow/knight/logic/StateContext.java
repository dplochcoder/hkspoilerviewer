package hollow.knight.logic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.main.Main;

/** Mostly immutable context for a State object. */
public final class StateContext {

  public interface Mutable {
    String saveName();

    JsonObject save() throws ICDLException;

    void load(JsonObject obj) throws ICDLException, ParseException;
  }

  private final boolean isHKS;
  private final JsonObject rawSpoilerJson;
  private final JsonObject icdlJson;
  private final String startLoc;
  private final CharmIds charmIds;
  private final RoomLabels roomLabels;
  private final Pools pools;
  private final NotchCosts notchCosts;
  private final LogicEdits logicEdits;
  private final DarknessOverrides darkness;
  private final ItemChecks checks;

  private final MutableTermMap tolerances;
  private final ImmutableTermMap setters;

  private final List<Mutable> mutables;

  public StateContext(boolean isHKS, JsonObject rawSpoilerJson, JsonObject icdlJson,
      String startLoc, CharmIds charmIds, RoomLabels roomLabels, Pools pools, NotchCosts notchCosts,
      LogicEdits logicEdits, DarknessOverrides darkness, ItemChecks checks, TermMap tolerances,
      TermMap setters) {
    this.isHKS = isHKS;
    this.rawSpoilerJson = rawSpoilerJson;
    this.icdlJson = icdlJson;
    this.startLoc = startLoc;
    this.charmIds = charmIds;
    this.roomLabels = roomLabels;
    this.pools = pools;
    this.notchCosts = notchCosts;
    this.logicEdits = logicEdits;
    this.darkness = darkness;
    this.checks = checks;
    this.tolerances = new MutableTermMap(tolerances);
    this.setters = ImmutableTermMap.copyOf(setters);

    this.mutables = new ArrayList<>();
    this.mutables.add(checks);
    this.mutables.add(notchCosts);
    this.mutables.add(logicEdits);
    this.mutables.add(mutableTolerancesWrapper());
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

  public String startLoc() {
    return startLoc;
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

  public LogicEdits logicEdits() {
    return logicEdits;
  }

  public DarknessOverrides darkness() {
    return darkness;
  }

  public ItemChecks checks() {
    return checks;
  }

  public TermMap setters() {
    return setters;
  }

  public TermMap tolerances() {
    return tolerances;
  }

  private Mutable mutableTolerancesWrapper() {
    return new Mutable() {
      @Override
      public String saveName() {
        return "ICDLTolerances";
      }

      @Override
      public JsonObject save() throws ICDLException {
        return tolerances.toJson();
      }

      @Override
      public void load(JsonObject obj) throws ICDLException {
        setTolerances(TermMap.fromJson(obj));
      }
    };
  }

  public void setTolerances(TermMap newTolerances) {
    tolerances.clear();
    tolerances.add(newTolerances);
  }

  public void saveMutables(JsonObject obj) throws ICDLException {
    for (Mutable m : mutables) {
      obj.add(m.saveName(), m.save());
    }
  }

  public void loadMutables(JsonObject obj) throws ICDLException, ParseException {
    for (Mutable m : mutables) {
      JsonElement elem = obj.get(m.saveName());
      if (elem != null) {
        m.load(elem.getAsJsonObject());
      }
    }
  }

  public static StateContext parse(boolean isHKS, JsonObject rawSpoilerJson, JsonObject icdlJson,
      JsonObject darknessJson) throws ParseException {
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

    String startLoc = rawSpoilerJson.get("GenerationSettings").getAsJsonObject()
        .get("StartLocationSettings").getAsJsonObject().get("StartLocation").getAsString();

    NotchCosts notchCosts = new NotchCosts();
    notchCosts.load(rawSpoilerJson);

    DarknessOverrides darkness =
        DarknessOverrides.parse((darknessJson != null) ? darknessJson : icdlJson);

    return new StateContext(isHKS, rawSpoilerJson, icdlJson, startLoc, charmIds, rooms, pools,
        notchCosts, new LogicEdits(), darkness, ItemChecks.parse(rawSpoilerJson, rooms), tolerances,
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

  private static int containerCompare(ItemCheck c1, ItemCheck c2) {
    boolean mimic1 = c1.item().term().name().toLowerCase().contains("mimic_grub");
    boolean mimic2 = c2.item().term().name().toLowerCase().contains("mimic_grub");
    if (mimic1 != mimic2) {
      return mimic1 ? -1 : 1;
    }

    boolean soul1 = c1.item().term().name().toLowerCase().contains("soul_totem");
    boolean soul2 = c2.item().term().name().toLowerCase().contains("soul_totem");
    if (soul1 != soul2) {
      return soul1 ? -1 : 1;
    }

    return Integer.compare(c1.id().id(), c2.id().id());
  }

  private JsonObject calculatePlacementsJson(Map<Term, JsonObject> itemJsons,
      Map<String, JsonObject> locationJsons) throws ICDLException {
    JsonObject placements = new JsonObject();

    // Group ItemChecks by Location.
    ListMultimap<String, ItemCheck> checksByLocation = ArrayListMultimap.create();
    checks().allChecks().filter(c -> !c.vanilla() && !c.isTransition())
        .forEach(c -> checksByLocation.put(c.location().name(), c));
    for (String k : checksByLocation.keySet()) {
      Collections.sort(checksByLocation.get(k), StateContext::containerCompare);
    }

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
        JsonObject itemObj;
        if (check.item().isCustom()) {
          itemObj = check.item().toICDLJson();
        } else if (itemJsons.containsKey(check.item().term())) {
          itemObj = itemJsons.get(check.item().term()).deepCopy();
        } else {
          throw new ICDLException("No item json for: " + check.item().term());
        }
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

        itemObj.add("tags", tagsArr);
        itemsArr.add(itemObj);
      }
      locObj.add("Items", itemsArr);

      placements.add(locName, locObj);
    }

    JsonObject origPlacements = icdlJson.get("Placements").getAsJsonObject();
    for (String name : origPlacements.keySet()) {
      if (!placements.keySet().contains(name)) {
        placements.add(name, origPlacements.get(name));
      }
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

  private static void setSceneGate(String tName, JsonObject obj) {
    int l = tName.indexOf("[");
    int r = tName.indexOf("]");
    obj.addProperty("SceneName", tName.substring(0, l));
    obj.addProperty("GateName", tName.substring(l + 1, r));
  }

  private JsonArray calculateTransitionOverrides() {
    JsonArray out = new JsonArray();
    checks().allChecks().filter(c -> !c.vanilla() && c.isTransition()).forEach(c -> {
      JsonObject override = new JsonObject();
      JsonObject key = new JsonObject();
      setSceneGate(c.location().name(), key);
      override.add("Key", key);
      JsonObject value = new JsonObject();
      value.addProperty("$type", "ItemChanger.Transition, ItemChanger");
      setSceneGate(c.item().term().name(), value);
      override.add("Value", value);
      out.add(override);
    });

    return out;
  }

  private JsonArray createNewSpoilerItemPlacements(JsonArray origPlacements) throws ICDLException {
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
    ImmutableList<ItemCheck> checks = checks().allChecks()
        .filter(c -> !c.vanilla() && !c.isTransition())
        .sorted(Comparator.comparing(c -> c.id().id())).collect(ImmutableList.toImmutableList());
    for (int i = 0; i < checks.size(); i++) {
      ItemCheck c = checks.get(i);
      if (c.id().id() != i) {
        throw new ICDLException("Placement indices don't match ids");
      }

      Item item = c.item();
      JsonObject placement = new JsonObject();
      placement.add("Item", item.toRawSpoilerJson(itemsJson));

      JsonObject locObj = locationsJson.get(c.location().name()).deepCopy();
      locObj.add("costs", c.costs().toRawSpoilerJson());
      placement.add("Location", locObj);

      placement.addProperty("Index", i);
      arr.add(placement);
    }

    return arr;
  }

  private JsonArray createNewSpoilerTransitionPlacements(JsonElement orig) throws ICDLException {
    Map<String, JsonObject> transitionsJson = new HashMap<>();
    if (orig != null && !orig.isJsonNull()) {
      for (JsonElement elem : orig.getAsJsonArray()) {
        JsonObject target = elem.getAsJsonObject().get("Target").getAsJsonObject();
        transitionsJson.put(target.get("Name").getAsString(), target);
        JsonObject source = elem.getAsJsonObject().get("Source").getAsJsonObject();
        transitionsJson.put(source.get("Name").getAsString(), source);
      }
    }

    JsonArray arr = new JsonArray();
    checks().allChecks().filter(c -> !c.vanilla() && c.isTransition()).forEach(c -> {
      JsonObject obj = new JsonObject();
      obj.add("Target", transitionsJson.get(c.item().term().name()));
      obj.add("Source", transitionsJson.get(c.location().name()));
      arr.add(obj);
    });

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
      try {
        JsonObject locJson = placements.get(locName).getAsJsonObject().deepCopy();
        for (JsonElement item : locJson.get("Items").getAsJsonArray()) {
          JsonObject itemObj = item.getAsJsonObject().deepCopy();
          String itemName = itemObj.get("name").getAsString();
          sanitizeItem(itemObj);
          itemJsons.put(Term.create(itemName), itemObj);
        }
        sanitizeLocation(locJson);
        locationJsons.put(locName, locJson);
      } catch (RuntimeException ex) {
        throw new RuntimeException("Location: " + locName, ex);
      }
    }

    JsonObject newICDLJson = icdlJson.deepCopy();
    newICDLJson.add("Placements", calculatePlacementsJson(itemJsons, locationJsons));
    newICDLJson.add("mods", withICDLCharmCosts(newICDLJson.get("mods").getAsJsonObject()));
    newICDLJson.add("TransitionOverrides", calculateTransitionOverrides());

    JsonObject newRawSpoilerJson = rawSpoilerJson.deepCopy();
    newRawSpoilerJson.add("itemPlacements",
        createNewSpoilerItemPlacements(rawSpoilerJson.get("itemPlacements").getAsJsonArray()));
    newRawSpoilerJson.add("transitionPlacements",
        createNewSpoilerTransitionPlacements(rawSpoilerJson.get("transitionPlacements")));
    newRawSpoilerJson.add("notchCosts", notchCosts().toRawSpoilerJsonArray());
    newRawSpoilerJson.add("InitialProgression",
        updateInitialProgression(newRawSpoilerJson.get("InitialProgression").getAsJsonObject()));
    logicEdits.updateLM(newRawSpoilerJson.get("LM").getAsJsonObject());

    String packName = JOptionPane.showInputDialog(null, "Name?");
    if (packName == null || packName.trim().isEmpty()) {
      throw new ICDLException("Must enter name");
    }
    String packDesc = JOptionPane.showInputDialog(null, "Description?");
    if (packDesc == null || packDesc.trim().isEmpty()) {
      packDesc = "Hollow Knight Plando (" + packName.trim() + ")";
    }
    String authorName = JOptionPane.showInputDialog(null, "Author?");
    if (authorName == null || authorName.trim().isEmpty()) {
      authorName = "<unknown> ";
    } else {
      authorName = authorName.trim() + " ";
    }

    JsonObject packJson = new JsonObject();
    packJson.addProperty("Author", authorName + "(HKSV " + Main.version() + ", "
        + LocalDate.now().format(DateTimeFormatter.ISO_DATE) + ")");
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
