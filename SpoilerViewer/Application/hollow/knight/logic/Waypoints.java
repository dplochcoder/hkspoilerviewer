package hollow.knight.logic;

import java.util.Map;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// The full set of Waypoints in the seed.
// All are auto-obtained when items are obtained.
public final class Waypoints {

  private final ImmutableMap<Term, Condition> waypoints;
  private final ImmutableMultimap<Condition, Term> inverse;

  private Waypoints(Map<Term, Condition> waypoints) {
    this.waypoints = ImmutableMap.copyOf(waypoints);

    ImmutableMultimap.Builder<Condition, Term> inverse = ImmutableMultimap.builder();
    this.waypoints.forEach((t, c) -> inverse.put(c, t));
    this.inverse = inverse.build();
  }

  public ImmutableSet<Term> allWaypoints() {
    return waypoints.keySet();
  }

  public Condition getCondition(Term waypoint) {
    return waypoints.get(waypoint);
  }

  public ImmutableCollection<Term> getTerms(Condition c) {
    return inverse.get(c);
  }

  public static Waypoints parse(JsonObject json) throws ParseException {
    JsonObject lm = json.get("LM").getAsJsonObject();

    ImmutableMap.Builder<Term, Condition> waypoints = ImmutableMap.builder();

    JsonArray jsonWaypoints = lm.get("Waypoints").getAsJsonArray();
    for (JsonElement elem : jsonWaypoints) {
      JsonObject obj = elem.getAsJsonObject();

      Term term = Term.create(obj.get("name").getAsString());
      Condition cond = ConditionParser.parse(obj.get("logic").getAsString());
      waypoints.put(term, cond);
    }

    return new Waypoints(waypoints.build());
  }

}
