package hollow.knight.logic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.util.JsonUtil;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final Map<Term, Integer> termValues = new HashMap<>();
  private final Set<ItemCheck> acquiredItemChecks = new HashSet<>();

  private final RoomLabels roomLabels;
  private final Waypoints waypoints;
  private final Items items;

  private State(RoomLabels rooms, Waypoints waypoints, Items items) {
    this.roomLabels = rooms;
    this.waypoints = waypoints;
    this.items = items;

    // TRUE is always set.
    set(Term.true_(), 1);

    // TODO: handle cursed logic
    set(Term.create("FOCUS"), 1);
    set(Term.create("LEFTSLASH"), 1);
    set(Term.create("DOWNSLASH"), 1);
    set(Term.create("UPSLASH"), 1);
    set(Term.create("RIGHTSLASH"), 1);
  }

  public RoomLabels roomLabels() {
    return roomLabels;
  }

  public Items items() {
    return items;
  }

  public ImmutableSet<ItemCheck> unobtainedItemChecks() {
    Set<ItemCheck> unobtained = new HashSet<>(items.allItemChecks());
    unobtained.removeAll(acquiredItemChecks);
    return ImmutableSet.copyOf(unobtained);
  }

  public int get(Term term) {
    return termValues.getOrDefault(term, 0);
  }

  public void set(Term term, int value) {
    if (value == 0) {
      termValues.remove(term);
    } else {
      termValues.put(term, value);
    }
  }

  public void dumpTerms() {
    for (Term t : termValues.keySet().stream().sorted((t1, t2) -> t1.name().compareTo(t2.name()))
        .collect(ImmutableList.toImmutableList())) {
      System.err.println(t + ": " + termValues.get(t));
    }
  }

  public void acquireItemCheck(ItemCheck itemCheck) {
    if (acquiredItemChecks.contains(itemCheck)) {
      return;
    }

    itemCheck.item().apply(this);
    acquiredItemChecks.add(itemCheck);
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> potWaypoints = new HashSet<>(waypoints.allWaypoints());
    potWaypoints.removeAll(termValues.keySet());

    // Loop: Evaluate all waypoints until there are no more advancements to be made.
    while (!potWaypoints.isEmpty()) {
      Set<Term> influences = new HashSet<>();
      for (Term t : potWaypoints) {
        Condition c = waypoints.get(t);

        if (c.test(this)) {
          set(t, 1);
          influences.addAll(waypoints.influences(t));
        }
      }

      influences.removeAll(termValues.keySet());
      potWaypoints = influences;
    }
  }

  public State deepCopy() {
    State copy = new State(roomLabels, waypoints, items);
    copy.termValues.putAll(this.termValues);
    copy.acquiredItemChecks.addAll(this.acquiredItemChecks);
    return copy;
  }

  public static State parse(Path rawSpoiler) throws IOException, ParseException {
    RoomLabels rooms = RoomLabels.load();
    JsonObject json = JsonUtil.loadPath(rawSpoiler).getAsJsonObject();

    State state = new State(rooms, Waypoints.parse(json), Items.parse(json, rooms));

    JsonArray setters =
        json.get("InitialProgression").getAsJsonObject().get("Setters").getAsJsonArray();
    for (JsonElement termValue : setters) {
      JsonObject obj = termValue.getAsJsonObject();

      Term term = Term.create(obj.get("Term").getAsString());
      int value = obj.get("Value").getAsInt();
      state.set(term, value);
    }

    return state;
  }
}
