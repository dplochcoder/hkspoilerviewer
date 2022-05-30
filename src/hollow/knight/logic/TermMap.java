package hollow.knight.logic;

import java.util.Set;
import com.google.gson.JsonObject;

public interface TermMap {
  Set<Term> terms();

  int get(Term term);

  default JsonObject toJson() {
    JsonObject obj = new JsonObject();
    terms().forEach(t -> obj.addProperty(t.name(), get(t)));
    return obj;
  }

  static TermMap fromJson(JsonObject obj) {
    MutableTermMap map = new MutableTermMap();
    obj.keySet().forEach(t -> map.set(Term.create(t), obj.get(t).getAsInt()));
    return map;
  }

  static TermMap empty() {
    return ImmutableTermMap.empty();
  }
}
