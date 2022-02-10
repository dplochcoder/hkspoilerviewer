package hollow.knight.logic;

import com.google.gson.JsonObject;

public final class Item {
  private final Term term;
  private final Condition logic;
  private final TermMap trueEffects;
  private final TermMap falseEffects;
  private final TermMap caps;

  private Item(Term term, Condition logic, TermMap trueEffects, TermMap falseEffects,
      TermMap caps) {
    this.term = term;
    this.logic = logic;
    this.trueEffects = trueEffects;
    this.falseEffects = falseEffects;
    this.caps = caps;
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

  private static TermMap parseEffects(JsonObject obj) {
    if (obj.get("Effect") != null) {
      return TermMap.parse(obj.get("Effect").getAsJsonObject());
    } else if (obj.get("Effects") != null) {
      return TermMap.parse(obj.get("Effects").getAsJsonArray());
    } else if (obj.get("item") != null) {
      return parseEffects(obj.get("item").getAsJsonObject());
    } else {
      return TermMap.none();
    }
  }

  public static Item parse(JsonObject item) throws ParseException {
    Term name = Term.create(item.get("Name").getAsString());

    Condition logic = Condition.alwaysTrue();
    TermMap trueEffects = TermMap.none();
    TermMap falseEffects = TermMap.none();
    TermMap caps = TermMap.none();
    if (item.get("Logic") != null) {
      logic = ConditionParser.parse(item.get("Logic").getAsJsonObject().get("logic").getAsString());
      trueEffects = parseEffects(item.get("TrueItem").getAsJsonObject());
      falseEffects = parseEffects(item.get("FalseItem").getAsJsonObject());
    } else {
      trueEffects = parseEffects(item);
    }

    if (item.get("Cap") != null) {
      caps = TermMap.parse(item.get("Cap").getAsJsonObject());
    }

    return new Item(name, logic, trueEffects, falseEffects, caps);
  }
}
