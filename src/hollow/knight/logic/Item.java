package hollow.knight.logic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

public final class Item {
  private final Term term;
  private final ImmutableSet<String> types;
  private final Condition logic;
  private final ImmutableTermMap trueEffects;
  private final ImmutableTermMap falseEffects;
  private final ImmutableTermMap caps;
  private final boolean fromJson;

  private Item(Term term, Set<String> types, Condition logic, TermMap trueEffects,
      TermMap falseEffects, TermMap caps) {
    this.term = term;
    this.types = ImmutableSet.copyOf(types);
    this.logic = logic;
    this.trueEffects = ImmutableTermMap.copyOf(trueEffects);
    this.falseEffects = ImmutableTermMap.copyOf(falseEffects);
    this.caps = ImmutableTermMap.copyOf(caps);
    this.fromJson = true;
  }

  private Item(Term term, Set<String> types, Term effectTerm, int value) {
    this.term = term;
    this.types = ImmutableSet.copyOf(types);
    this.logic = Condition.alwaysTrue();

    MutableTermMap termMap = new MutableTermMap();
    termMap.add(effectTerm, value);
    this.trueEffects = ImmutableTermMap.copyOf(termMap);
    this.falseEffects = ImmutableTermMap.empty();
    this.caps = ImmutableTermMap.empty();
    this.fromJson = false;
  }

  private static final ImmutableSet<String> GEO_TYPES =
      ImmutableSet.of("RandomizerMod.RC.CustomGeoItem", "RandomizerMod");

  public static Item newGeoItem(int value) {
    return new Item(Term.create(value + "_Geo"), GEO_TYPES, Term.geo(), value);
  }

  private static final ImmutableSet<String> ESSENCE_TYPES =
      ImmutableSet.of("RandomizerCore.LogicItems.SingleItem", "RandomizerCore");

  public static Item newEssenceItem(int value) {
    return new Item(Term.create(value + "_Essence"), ESSENCE_TYPES, Term.essence(), value);
  }

  public Term term() {
    return term;
  }

  public String displayName() {
    return term().equals(Term.nothing()) ? "Nothing?" : term().name();
  }

  public ImmutableSet<String> types() {
    return types;
  }

  public boolean hasEffectTerm(Term term) {
    return trueEffects.get(term) != 0 || falseEffects.get(term) != 0;
  }

  public int getEffectValue(Term term) {
    return trueEffects.get(term);
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

  void apply(State state) {
    // We can't do state.test() here because item conditions don't necessarily hold to the false ->
    // true paradigm.
    TermMap effects =
        logic.test(state.termValues(), state.ctx().notchCosts()) ? trueEffects : falseEffects;

    for (Term t : effects.terms()) {
      int cap = Integer.MAX_VALUE;
      if (caps.terms().contains(t)) {
        cap = caps.get(t);
      }

      int newVal = Math.min(state.get(t) + effects.get(t), cap);
      state.set(t, newVal);
    }
  }

  private static void parseEffect(JsonObject obj, MutableTermMap out) {
    out.add(Term.create(obj.get("Term").getAsString()), obj.get("Value").getAsInt());
  }

  private static void parseEffects(JsonObject obj, MutableTermMap out) {
    if (obj.get("Effect") != null) {
      parseEffect(obj.get("Effect").getAsJsonObject(), out);
    } else if (obj.get("Effects") != null) {
      for (JsonElement elem : obj.get("Effects").getAsJsonArray()) {
        parseEffect(elem.getAsJsonObject(), out);
      }
    } else if (obj.get("item") != null) {
      parseEffects(obj.get("item").getAsJsonObject(), out);
    }
  }

  public static Item fromJson(ItemChecks checks, JsonElement json) {
    if (json.isJsonPrimitive()) {
      return checks.getItem(Term.create(json.getAsString()));
    }

    JsonObject obj = json.getAsJsonObject();
    Term term = Term.create(obj.get("term").getAsString());
    Set<String> types = new HashSet<>();
    JsonArray typesArr = obj.get("types").getAsJsonArray();
    typesArr.forEach(s -> types.add(s.getAsString()));
    Term effectTerm = Term.create(obj.get("effectTerm").getAsString());
    int value = obj.get("value").getAsInt();

    return new Item(term, types, effectTerm, value);
  }

  public JsonElement toJson() {
    if (fromJson) {
      return new JsonPrimitive(term().name());
    }

    JsonObject obj = new JsonObject();
    obj.add("term", new JsonPrimitive(term().name()));
    JsonArray types = new JsonArray();
    types().forEach(types::add);
    Term effectTerm = trueEffects.terms().iterator().next();
    obj.addProperty("effectTerm", effectTerm.name());
    obj.addProperty("value", trueEffects.get(effectTerm));
    return obj;
  }

  public static Item parse(JsonObject item) throws ParseException {
    if (item.get("item") != null) {
      item = item.get("item").getAsJsonObject();
    }

    Term name;
    if (item.get("Name") != null) {
      name = Term.create(item.get("Name").getAsString());
    } else {
      name = Term.create(item.get("logic").getAsJsonObject().get("Name").getAsString());
    }

    Condition logic = Condition.alwaysTrue();
    MutableTermMap trueEffects = new MutableTermMap();
    MutableTermMap falseEffects = new MutableTermMap();
    MutableTermMap caps = new MutableTermMap();
    if (item.get("Logic") != null) {
      logic = ConditionParser.parse(item.get("Logic").getAsJsonObject().get("Logic").getAsString());
      parseEffects(item.get("TrueItem").getAsJsonObject(), trueEffects);
      parseEffects(item.get("FalseItem").getAsJsonObject(), falseEffects);
    } else {
      parseEffects(item, trueEffects);
    }

    if (item.get("Cap") != null) {
      parseEffect(item.get("Cap").getAsJsonObject(), caps);
    }

    Set<String> types = Arrays.stream(item.get("$type").getAsString().split(", "))
        .collect(ImmutableSet.toImmutableSet());

    return new Item(name, types, logic, trueEffects, falseEffects, caps);
  }
}
