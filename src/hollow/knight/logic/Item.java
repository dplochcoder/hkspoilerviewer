package hollow.knight.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Item {
  private final Term term;
  private final Condition logic;
  private final ImmutableTermMap trueEffects;
  private final ImmutableTermMap falseEffects;
  private final ImmutableTermMap caps;

  private Item(Term term, Condition logic, TermMap trueEffects, TermMap falseEffects,
      TermMap caps) {
    this.term = term;
    this.logic = logic;
    this.trueEffects = ImmutableTermMap.copyOf(trueEffects);
    this.falseEffects = ImmutableTermMap.copyOf(falseEffects);
    this.caps = ImmutableTermMap.copyOf(caps);
  }

  public Term term() {
    return term;
  }

  public boolean isCharm(Items items) {
    return items.charmIds().charmId(term) != null;
  }

  public int notchCost(Items items) {
    return items.notchCost(items.charmIds().charmId(term));
  }

  public boolean hasEffectTerm(Term term) {
    return trueEffects.get(term) != 0 || falseEffects.get(term) != 0;
  }

  public int getEffectValue(Term term) {
    return trueEffects.get(term);
  }

  void apply(State state) {
    TermMap effects = logic.test(state) ? trueEffects : falseEffects;

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

  public static Item parse(JsonObject item) throws ParseException {
    Term name = Term.create(item.get("Name").getAsString());

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

    return new Item(name, logic, trueEffects, falseEffects, caps);
  }
}
