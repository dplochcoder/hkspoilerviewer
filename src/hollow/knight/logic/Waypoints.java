package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// The full set of Waypoints and Transitions in the random seed.
// All are auto-obtained when items are obtained.
public final class Waypoints {

  private final ImmutableMap<Term, Condition> waypoints;

  // Inverse index; reaching a key in this map may grant access to any of its mapped values.
  private final ImmutableSetMultimap<Term, Term> influences;

  private static ImmutableSetMultimap<Term, Term> indexInfluences(Map<Term, Condition> waypoints) {
    ImmutableSetMultimap.Builder<Term, Term> builder = ImmutableSetMultimap.builder();
    for (Term t : waypoints.keySet()) {
      for (Term influencer : waypoints.get(t).terms()) {
        builder.put(influencer, t);
      }
    }
    return builder.build();
  }

  private Waypoints(Map<Term, Condition> waypoints) {
    this.waypoints = ImmutableMap.copyOf(waypoints);
    this.influences = indexInfluences(this.waypoints);
  }

  public ImmutableSet<Term> allWaypoints() {
    return waypoints.keySet();
  }

  public Condition get(Term waypoint) {
    return waypoints.get(waypoint);
  }

  public ImmutableSet<Term> influences(Term term) {
    return influences.get(term);
  }

  public static Waypoints parse(JsonObject json) throws ParseException {
    JsonObject lm = json.get("LM").getAsJsonObject();

    Map<Term, Condition> waypoints = new HashMap<>();
    Map<Term, Term> transitions = new HashMap<>();

    JsonArray jsonWaypoints = lm.get("Waypoints").getAsJsonArray();
    for (JsonElement elem : jsonWaypoints) {
      JsonObject obj = elem.getAsJsonObject();

      Term term = Term.create(obj.get("name").getAsString());
      Condition cond = ConditionParser.parse(obj.get("logic").getAsString());
      waypoints.put(term, cond);
    }

    JsonElement transitionPlacements = json.get("transitionPlacements");
    if (!transitionPlacements.isJsonNull()) {
      throw new ParseException("TODO: Support Room Rando");
    } else {
      // Load transitions from vanilla.
      JsonArray vanilla = json.get("Vanilla").getAsJsonArray();
      for (JsonElement item : vanilla) {
        JsonObject obj = item.getAsJsonObject();
        String type = obj.get("Item").getAsJsonObject().get("$type").getAsString();
        if (!type.contains("RandomizerMod.RC.RandoModTransition")) {
          continue;
        }

        JsonObject targetObj = obj.get("Item").getAsJsonObject().get("lt").getAsJsonObject();
        Term targetTerm = Term.create(targetObj.get("term").getAsString());
        Condition targetLogic = ConditionParser
            .parse(targetObj.get("logic").getAsJsonObject().get("logic").getAsString());
        waypoints.put(targetTerm, targetLogic);

        JsonObject sourceObj = obj.get("Location").getAsJsonObject().get("lt").getAsJsonObject();
        Term sourceTerm = Term.create(sourceObj.get("term").getAsString());
        Condition sourceLogic = ConditionParser
            .parse(sourceObj.get("logic").getAsJsonObject().get("logic").getAsString());
        waypoints.put(sourceTerm, sourceLogic);

        transitions.put(sourceTerm, targetTerm);
      }
    }

    // Modify logic by adding transition disjunctions.
    for (Term source : transitions.keySet()) {
      Term target = transitions.get(source);
      waypoints.put(target, new Disjunction(new TermCondition(source), waypoints.get(target)));
    }

    return new Waypoints(waypoints);
  }

}
