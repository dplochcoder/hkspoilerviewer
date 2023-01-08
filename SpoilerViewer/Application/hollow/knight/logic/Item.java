package hollow.knight.logic;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MoreCollectors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import hollow.knight.gui.TransitionData;

public final class Item {
  private final Term term;
  private final Optional<String> pool;
  private final ImmutableSet<String> types;
  private final ItemEffects effects;
  private final boolean fromOriginalJson;

  private Item(Term term, Optional<String> pool, Set<String> types, ItemEffects effects) {
    this.term = term;
    this.pool = pool;
    this.types = ImmutableSet.copyOf(types);
    this.effects = effects;
    this.fromOriginalJson = true;
  }

  private Item(Term term, String pool, Set<String> types, Term effectTerm, int effectValue) {
    this.term = term;
    this.pool = Optional.of(pool);
    this.types = ImmutableSet.copyOf(types);

    MutableTermMap termMap = new MutableTermMap();
    termMap.add(effectTerm, effectValue);
    this.effects = new TermMapItemEffects(termMap, TermMap.empty());

    this.fromOriginalJson = false;
  }

  private static final ImmutableSet<String> CUSTOM_ITEM_TYPES =
      ImmutableSet.of("RandomizerCore.LogicItems.SingleItem", "RandomizerCore");

  public static Item newGeoItem(int value) {
    return new Item(Term.create(value + "_Geo"), "Geo", CUSTOM_ITEM_TYPES, Term.geo(), value);
  }

  public static Item newEssenceItem(int value) {
    return new Item(Term.create(value + "_Essence"), "DreamWarrior", CUSTOM_ITEM_TYPES,
        Term.essence(), value);
  }

  public Term term() {
    return term;
  }

  public String getPool(Pools pools) {
    return pool.orElseGet(() -> pools.getPool(term()));
  }

  public String displayName(TransitionData transitionData) {
    if (isTransition()) {
      return transitionData.alias(term.name());
    } else if (term().equals(Term.nothing())) {
      return "Nothing?";
    } else {
      return term().name();
    }
  }

  public ImmutableSet<String> types() {
    return types;
  }

  public boolean isTransition() {
    return types().contains("RandomizerCore.Logic.LogicTransition");
  }

  public boolean hasEffectTerm(Term term) {
    return effects.hasEffectTerm(term);
  }

  public int getEffectValue(Term term) {
    return effects.getEffectValue(term);
  }

  public Stream<Term> effectTerms() {
    return effects.effectTerms();
  }

  public String valueSuffix() {
    if (hasEffectTerm(Term.geo())) {
      return "(" + getEffectValue(Term.geo()) + " Geo) ";
    } else if (hasEffectTerm(Term.essence())) {
      return "(" + getEffectValue(Term.essence()) + " Essence) ";
    } else {
      return "";
    }
  }

  void apply(MutableTermMap ctx) {
    effects.apply(ctx);
  }

  public boolean isCustom() {
    return !fromOriginalJson;
  }

  private static ItemEffects parseEffects(JsonObject obj) throws ParseException {
    ImmutableSet<String> types = Arrays.stream(obj.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());

    if (types.contains("RandomizerMod.RC.SplitCloakItem")) {
      return new SplitCloakItemEffects(obj.get("LeftBiased").getAsBoolean());
    }

    if (types.contains("RandomizerCore.LogicItems.BoolItem")) {
      MutableTermMap effects = new MutableTermMap();
      effects.add(Term.create(obj.get("Term").getAsString()), 1);

      return new TermMapItemEffects(effects, effects);
    }

    if (types.contains("RandomizerCore.Logic.LogicTransition")) {
      MutableTermMap effects = new MutableTermMap();
      effects.add(Term.create(obj.get("term").getAsString()), 1);

      return new TermMapItemEffects(effects, effects);
    }

    MutableTermMap effects = new MutableTermMap();
    MutableTermMap caps = new MutableTermMap();
    if (obj.get("Logic") != null) {
      // Ignore the condition.
      parseEffectsMap(obj.get("FalseItem").getAsJsonObject(), effects);
    } else {
      parseEffectsMap(obj, effects);
    }
    if (obj.get("Cap") != null) {
      parseEffectsMap(obj.get("Cap").getAsJsonObject(), caps);
    }

    return new TermMapItemEffects(effects, caps);
  }

  private static void parseSingleEffect(JsonObject obj, MutableTermMap out) {
    out.add(Term.create(obj.get("Term").getAsString()), obj.get("Value").getAsInt());
  }

  private static void parseEffectsMap(JsonObject obj, MutableTermMap out) {
    if (obj.has("Effect")) {
      parseSingleEffect(obj.get("Effect").getAsJsonObject(), out);
    } else if (obj.has("Effects")) {
      for (JsonElement elem : obj.get("Effects").getAsJsonArray()) {
        parseSingleEffect(elem.getAsJsonObject(), out);
      }
    } else if (obj.has("item")) {
      parseEffectsMap(obj.get("item").getAsJsonObject(), out);
    }
  }

  public static Item fromHKSJson(ItemChecks checks, JsonElement json) throws ICDLException {
    if (json.isJsonPrimitive()) {
      return checks.getItem(Term.create(json.getAsString()));
    }

    JsonObject obj = json.getAsJsonObject();
    String pool = obj.get("pool").getAsString();
    Term term = Term.create(obj.get("term").getAsString());
    LinkedHashSet<String> types = new LinkedHashSet<>();
    obj.get("types").getAsJsonArray().forEach(e -> types.add(e.getAsString()));
    Term effectTerm = Term.create(obj.get("effectTerm").getAsString());
    int effectValue = obj.get("effectValue").getAsInt();

    return new Item(term, pool, types, effectTerm, effectValue);
  }

  public JsonElement toHKSJson() {
    if (fromOriginalJson) {
      return new JsonPrimitive(term().name());
    }

    JsonObject obj = new JsonObject();
    obj.addProperty("pool", pool.get());
    obj.addProperty("term", term().name());
    JsonArray jsonTypes = new JsonArray();
    types().forEach(jsonTypes::add);
    obj.add("types", jsonTypes);
    Term effectTerm = effects.effectTerms().collect(MoreCollectors.onlyElement());
    obj.addProperty("effectTerm", effectTerm.name());
    obj.addProperty("effectValue", effects.getEffectValue(effectTerm));
    return obj;
  }

  public JsonObject toRawSpoilerJson(Map<Term, JsonObject> origJson) {
    if (fromOriginalJson) {
      return origJson.get(term());
    }

    JsonObject obj = new JsonObject();

    JsonObject itemDef = new JsonObject();
    itemDef.addProperty("Name", term().name());
    itemDef.addProperty("Pool", pool.get());
    itemDef.addProperty("PriceCap", 1);;
    itemDef.addProperty("MajorItem", false);
    obj.add("ItemDef", itemDef);

    JsonObject item = new JsonObject();
    item.addProperty("$type", types().stream().collect(Collectors.joining(", ")));
    item.addProperty("Name", term.name());
    Term effectTerm = effects.effectTerms().collect(MoreCollectors.onlyElement());
    JsonObject effect = new JsonObject();
    effect.addProperty("Term", effectTerm.name());
    effect.addProperty("Value", effects.getEffectValue(effectTerm));
    item.add("Effect", effect);
    obj.add("item", item);

    obj.addProperty("Name", term.name());
    obj.addProperty("Placed", "Permanent");
    obj.addProperty("Sphere", 1);;
    obj.addProperty("Required", false);

    return obj;
  }

  public JsonObject toICDLJson() throws ICDLException {
    Term effectTerm = effects.effectTerms().collect(MoreCollectors.onlyElement());
    int effectValue = effects.getEffectValue(effectTerm);
    if (effectTerm.equals(Term.geo())) {
      JsonObject obj = new JsonObject();
      obj.addProperty("$type", "ItemChanger.Items.SpawnGeoItem, ItemChanger");
      obj.addProperty("amount", effectValue);
      obj.addProperty("name", term.name());
      obj.addProperty("obtainState", "Unobtained");

      JsonObject uiDef = new JsonObject();
      uiDef.addProperty("$type", "ItemChanger.UIDefs.MsgUIDef, ItemChanger");

      JsonObject name = new JsonObject();
      name.addProperty("$type", "ItemChanger.BoxedString, ItemChanger");
      name.addProperty("Value", effectValue + " Geo");
      uiDef.add("name", name);
      JsonObject shopDesc = new JsonObject();
      shopDesc.addProperty("$type", "ItemChanger.LanguageString, ItemChanger");
      shopDesc.addProperty("sheet", "UI");
      shopDesc.addProperty("key", "ITEMCHANGER_DESC_GEO");
      uiDef.add("shopDesc", shopDesc);
      JsonObject sprite = new JsonObject();
      sprite.addProperty("$type", "ItemChanger.ItemChangerSprite, ItemChanger");
      sprite.addProperty("key", "ShopIcons.Geo");
      uiDef.add("sprite", sprite);

      obj.add("UIDef", uiDef);
      return obj;
    } else if (effectTerm.equals(Term.essence())) {
      JsonObject obj = new JsonObject();
      obj.addProperty("$type", "ItemChanger.Items.EssenceItem, ItemChanger");
      obj.addProperty("amount", effectValue);
      obj.addProperty("name", term.name());
      obj.addProperty("obtainState", "Unobtained");

      JsonObject uiDef = new JsonObject();
      uiDef.addProperty("$type", "ItemChanger.UIDefs.MsgUIDef, ItemChanger");

      JsonObject name = new JsonObject();
      name.addProperty("$type", "ItemChanger.LanguageString, ItemChanger");
      name.addProperty("sheet", "UI");
      name.addProperty("key", "ITEMCHANGER_NAME_ESSENCE_" + effectValue);
      uiDef.add("name", name);
      JsonObject shopDesc = new JsonObject();
      shopDesc.addProperty("$type", "ItemChanger.LanguageString, ItemChanger");
      shopDesc.addProperty("sheet", "UI");
      shopDesc.addProperty("key", "ITEMCHANGER_DESC_ESSENCE");
      uiDef.add("shopDesc", shopDesc);
      JsonObject sprite = new JsonObject();
      sprite.addProperty("$type", "ItemChanger.ItemChangerSprite, ItemChanger");
      sprite.addProperty("key", "ShopIcons.Essence");
      uiDef.add("sprite", sprite);

      obj.add("UIDef", uiDef);
      return obj;
    } else {
      throw new ICDLException("Unsupported custom item type: " + effectTerm);
    }
  }

  private static final ImmutableSet<String> TRANSITION_TYPES =
      ImmutableSet.of("RandomizerCore.Logic.LogicTransition", "RandomizerCore");

  public static Item parse(JsonObject item) throws ParseException {
    if (item.has("TransitionDef")) {
      Term term = Term.create(item.get("Name").getAsString());
      MutableTermMap terms = new MutableTermMap();
      terms.add(term, 1);

      return new Item(term, Optional.of("Transitions"), TRANSITION_TYPES,
          new TermMapItemEffects(terms, terms));
    }

    Optional<String> pool = Optional.empty();
    if (item.has("ItemDef") && item.get("ItemDef").isJsonObject()) {
      pool = Optional.of(item.get("ItemDef").getAsJsonObject().get("Pool").getAsString());
    }

    if (item.get("item") != null) {
      item = item.get("item").getAsJsonObject();
    }

    Term name;
    if (item.get("Name") != null) {
      name = Term.create(item.get("Name").getAsString());
    } else {
      name = Term.create(item.get("logic").getAsJsonObject().get("Name").getAsString());
    }

    Set<String> types = Arrays.stream(item.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());

    return new Item(name, pool, types, parseEffects(item));
  }
}
