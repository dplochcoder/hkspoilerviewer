package hollow.knight.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/** Immutable context for a State object. */
public final class StateContext {

  private final JsonObject originalJson;
  private final RoomLabels roomLabels;
  private final Waypoints waypoints;
  private final Items items;

  private final ImmutableTermMap tolerances;
  private final ImmutableTermMap setters;

  public StateContext(JsonObject originalJson, RoomLabels roomLabels, Waypoints waypoints,
      Items items, TermMap tolerances, TermMap setters) {
    this.originalJson = originalJson;
    this.roomLabels = roomLabels;
    this.waypoints = waypoints;
    this.items = items;
    this.tolerances = ImmutableTermMap.copyOf(tolerances);
    this.setters = ImmutableTermMap.copyOf(setters);
  }

  public JsonObject originalJson() {
    return originalJson;
  }

  public RoomLabels roomLabels() {
    return roomLabels;
  }

  public Waypoints waypoints() {
    return waypoints;
  }

  public Items items() {
    return items;
  }

  public ImmutableTermMap tolerances() {
    return tolerances;
  }

  public State newInitialState() {
    State state = new State(this);

    // Automatically acquire all items at Start
    items.startItems().forEach(state::acquireItemCheck);
    for (Term t : setters.terms()) {
      state.set(t, setters.get(t));
    }

    state.normalize();
    return state;
  }

  public static StateContext parse(JsonObject json) throws ParseException {
    RoomLabels rooms = RoomLabels.load();

    MutableTermMap setters = new MutableTermMap();
    MutableTermMap tolerances = new MutableTermMap();

    JsonArray jsonSetters =
        json.get("InitialProgression").getAsJsonObject().get("Setters").getAsJsonArray();
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

    return new StateContext(json, rooms, Waypoints.parse(json), Items.parse(json, rooms),
        tolerances, setters);
  }

}
