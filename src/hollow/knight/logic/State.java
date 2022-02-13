package hollow.knight.logic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.util.JsonUtil;

/** Mutable state of a run; can be deep-copied. */
public class State {
  private final MutableTermMap termValues = new MutableTermMap();
  private final MutableTermMap costValuesWithTolerances = new MutableTermMap();
  private final Set<ItemCheck> acquiredItemChecks = new HashSet<>();
  private final ImmutableTermMap tolerances;

  private final RoomLabels roomLabels;
  private final Waypoints waypoints;
  private final Items items;

  private static final ImmutableSet<Term> COST_TERMS = ImmutableSet.of(Term.create("GRUBS"),
      Term.create("ESSENCE"), Term.create("RANCIDEGGS"), Term.create("CHARMS"));

  private State(RoomLabels rooms, Waypoints waypoints, Items items, TermMap tolerances) {
    this.roomLabels = rooms;
    this.waypoints = waypoints;
    this.items = items;
    this.tolerances = ImmutableTermMap.copyOf(tolerances);

    // TRUE is always set.
    set(Term.true_(), 1);

    // Automatically acquire all items at Start
    items.allItemChecks().stream().filter(c -> c.location().scene().contentEquals("Start"))
        .forEach(this::acquireItemCheck);
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
    return termValues.get(term);
  }

  public void set(Term term, int value) {
    termValues.set(term, value);
  }

  public void dumpTerms() {
    for (Term t : termValues.terms().stream().sorted((t1, t2) -> t1.name().compareTo(t2.name()))
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

  private static boolean canAffectCosts(ItemCheck c) {
    return COST_TERMS.stream().anyMatch(t -> c.item().hasEffectTerm(t));
  }

  // Iteratively apply logic to grant access to items / areas.
  public void normalize() {
    Set<Term> potWaypoints = new HashSet<>(waypoints.allWaypoints());
    potWaypoints.removeAll(termValues.terms());

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

      influences.removeAll(termValues.terms());
      potWaypoints = influences;
    }

    // Acquire all in-logic items that yield an effect on a cost term, until no more such items can
    // be acquired.
    State stateCopy = this.deepCopy();
    Set<ItemCheck> canAcquire = stateCopy.unobtainedItemChecks().stream()
        .filter(State::canAffectCosts).filter(c -> c.location().canAccess(this))
        .collect(Collectors.toCollection(HashSet::new));
    while (true) {
      int acquired = 0;
      for (ItemCheck c : new HashSet<>(canAcquire)) {
        if (c.costs().canBePaid(this.get(Term.canReplenishGeo()) > 0, stateCopy.termValues)) {
          stateCopy.acquireItemCheck(c);
          canAcquire.remove(c);
          acquired++;
        }
      }

      if (acquired == 0) {
        break;
      }
    }

    // For each cost term, determine the total we have logical access to, without acquiring anything
    // except more cost effects.
    for (Term t : COST_TERMS) {
      costValuesWithTolerances.set(t, this.tolerances.get(t) + stateCopy.get(t));
    }
  }

  public TermMap termValues() {
    return this.termValues;
  }

  public TermMap accessibleTermValues() {
    return this.costValuesWithTolerances;
  }

  public State deepCopy() {
    State copy = new State(roomLabels, waypoints, items, tolerances);
    copy.termValues.clear();
    for (Term t : this.termValues.terms()) {
      copy.termValues.set(t, get(t));
    }
    copy.acquiredItemChecks.addAll(this.acquiredItemChecks);
    copy.costValuesWithTolerances.clear();
    copy.costValuesWithTolerances.add(this.costValuesWithTolerances);
    return copy;
  }

  public static State parse(Path rawSpoiler) throws IOException, ParseException {
    RoomLabels rooms = RoomLabels.load();
    JsonObject json = JsonUtil.loadPath(rawSpoiler).getAsJsonObject();

    MutableTermMap setters = new MutableTermMap();
    MutableTermMap tolerances = new MutableTermMap();

    JsonArray jsonSetters =
        json.get("InitialProgression").getAsJsonObject().get("Setters").getAsJsonArray();
    for (JsonElement termValue : jsonSetters) {
      JsonObject obj = termValue.getAsJsonObject();

      Term term = Term.create(obj.get("Term").getAsString());
      int value = obj.get("Value").getAsInt();
      if (COST_TERMS.contains(term)) {
        tolerances.set(term, value);
      } else {
        setters.set(term, value);
      }
    }

    State state = new State(rooms, Waypoints.parse(json), Items.parse(json, rooms), tolerances);
    for (Term t : setters.terms()) {
      state.set(t, setters.get(t));
    }
    return state;
  }
}
