package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class TermMap {
  private final ImmutableMap<Term, Integer> values;
  
  public TermMap(Map<Term, Integer> values) {
    this.values = ImmutableMap.copyOf(values);
  }
  
  public ImmutableSet<Term> terms() {
    return values.keySet();
  }
  
  public int get(Term term) {
    return values.getOrDefault(term, 0);
  }
  
  public TermMap with(TermMap other) {
    Map<Term, Integer> sum = new HashMap<>(this.values);
    for (Term t : other.values.keySet()) {
      sum.put(t, other.values.get(t) + this.values.getOrDefault(t, 0));
    }
    return new TermMap(sum);
  }
  
  public static TermMap parse(JsonObject effect) {
    Term term = Term.create(effect.get("Term").getAsString());
    int value = effect.get("Value").getAsInt();
    
    return new TermMap(ImmutableMap.of(term, value));
  }
  
  public static TermMap parse(JsonArray effects) {
    TermMap out = NONE;
    for (JsonElement effect : effects) {
      out = out.with(parse(effect.getAsJsonObject()));
    }
    return out;
  }
  
  private static final TermMap NONE = new TermMap(ImmutableMap.of());
  
  public static TermMap none() { return NONE; }
}