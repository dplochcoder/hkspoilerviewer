package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

// The full set of Waypoints and Transitions in the random seed.
// All are auto-obtained when items are obtained.
public final class Waypoints {

  private final ImmutableMap<Term, Condition> waypoints;
  private final ImmutableMultimap<Condition, Term> inverse;


  private Waypoints(Map<Term, Condition> waypoints) {
    this.waypoints = ImmutableMap.copyOf(waypoints);

    ImmutableMultimap.Builder<Condition, Term> inverse = ImmutableMultimap.builder();
    for (Term t : waypoints.keySet()) {
      inverse.put(waypoints.get(t), t);
    }
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

  public static Waypoints parse(JsonObject json, ConditionParser.Context parseCtx)
      throws ParseException {
    JsonObject lm = json.get("LM").getAsJsonObject();

    Map<Term, Condition> waypoints = new HashMap<>();
    Map<Term, Term> transitions = new HashMap<>();

    JsonArray jsonWaypoints = lm.get("Waypoints").getAsJsonArray();
    for (JsonElement elem : jsonWaypoints) {
      JsonObject obj = elem.getAsJsonObject();

      Term term = Term.create(obj.get("name").getAsString());
      Condition cond = ConditionParser.parse(parseCtx, obj.get("logic").getAsString());
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
        if (!type.contains("RandomizerCore.Logic.LogicTransition")) {
          continue;
        }

        JsonObject targetObj = obj.get("Item").getAsJsonObject();
        Term targetTerm = Term.create(targetObj.get("term").getAsString());
        Condition targetLogic = ConditionParser.parse(parseCtx,
            targetObj.get("logic").getAsJsonObject().get("Logic").getAsString());
        waypoints.put(targetTerm, targetLogic);

        JsonObject sourceObj = obj.get("Location").getAsJsonObject();
        Term sourceTerm = Term.create(sourceObj.get("term").getAsString());
        Condition sourceLogic = ConditionParser.parse(parseCtx,
            sourceObj.get("logic").getAsJsonObject().get("Logic").getAsString());
        waypoints.put(sourceTerm, sourceLogic);

        transitions.put(sourceTerm, targetTerm);
      }
    }

    // Modify logic by adding transition disjunctions.
    for (Term source : transitions.keySet()) {
      Term target = transitions.get(source);
      waypoints.put(target,
          Disjunction.of(TermGreaterThanCondition.of(source), waypoints.get(target)));
    }

    return new Waypoints(waypoints);
  }

}
