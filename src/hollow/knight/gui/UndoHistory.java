package hollow.knight.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import com.google.auto.value.AutoValue;
import com.google.auto.value.extension.memoized.Memoized;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Sets;
import hollow.knight.logic.CheckId;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ListenerManager;
import hollow.knight.logic.StateContext;

public final class UndoHistory {

  public interface Listener {
    void newSnapshot(int index);

    void truncated(int lastIndex);
  }

  private interface CheckpointInterface {
    Set<ItemCheck> checks();

    List<Integer> notchCosts();

    default int size() {
      return checks().size() + notchCosts().size();
    }
  }

  private static class Checkpoint implements CheckpointInterface {
    private final Set<ItemCheck> checks;
    private final List<Integer> notchCosts;

    public Checkpoint() {
      this.checks = new HashSet<>();
      this.notchCosts = new ArrayList<>();
    }

    private Checkpoint(Checkpoint copy) {
      this.checks = new HashSet<>(copy.checks);
      this.notchCosts = new ArrayList<>(copy.notchCosts);
    }

    public void reset(CheckpointInterface other) {
      checks.clear();
      notchCosts.clear();

      checks.addAll(other.checks());
      notchCosts.addAll(other.notchCosts());
    }

    @Override
    public Set<ItemCheck> checks() {
      return checks;
    }

    @Override
    public List<Integer> notchCosts() {
      return notchCosts;
    }

    public Checkpoint deepCopy() {
      return new Checkpoint(this);
    }

    public void applyDelta(Delta delta) {
      checks.removeIf(c -> delta.removedCheckIds().contains(c.id()));
      checks.addAll(delta.newChecks());
      delta.updatedNotchCosts().forEach(notchCosts::set);
    }
  }

  private static final class StateContextView implements CheckpointInterface {
    private final StateContext ctx;

    StateContextView(StateContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public Set<ItemCheck> checks() {
      return ctx.checks().allChecksSet();
    }

    @Override
    public List<Integer> notchCosts() {
      return ctx.notchCosts().costs();
    }
  }

  @AutoValue
  abstract static class Delta {
    public abstract ImmutableSet<ItemCheck> newChecks();

    public abstract ImmutableSet<CheckId> removedCheckIds();

    public abstract ImmutableMap<Integer, Integer> updatedNotchCosts();

    @Memoized
    public int size() {
      return newChecks().size() + removedCheckIds().size() + updatedNotchCosts().size();
    }

    private static ImmutableMap<Integer, Integer> notchDiff(List<Integer> before,
        List<Integer> after) {
      ImmutableMap.Builder<Integer, Integer> diff = ImmutableMap.builder();
      for (int i = 0; i < after.size(); i++) {
        if (before.get(i).intValue() != after.get(i).intValue()) {
          diff.put(i, after.get(i).intValue());
        }
      }
      return diff.build();
    }

    public static Delta compute(CheckpointInterface before, CheckpointInterface after) {
      return new AutoValue_UndoHistory_Delta(
          ImmutableSet.copyOf(Sets.difference(after.checks(), before.checks())),
          Sets.difference(before.checks(), after.checks()).stream().map(ItemCheck::id).collect(
              ImmutableSet.toImmutableSet()),
          notchDiff(before.notchCosts(), after.notchCosts()));
    }
  }

  private final ListenerManager<Listener> listeners = new ListenerManager<>();

  private final List<String> labels = new ArrayList<>();
  private final SortedMap<Integer, Checkpoint> checkpoints = new TreeMap<>();
  private final ListMultimap<Integer, Delta> deltas = ArrayListMultimap.create();
  private int checkpointIndex = -1;
  private final Checkpoint current = new Checkpoint();

  private int nextIndex = 0;
  private int deltaSum = 0;

  public UndoHistory(String label, StateContext ctx) {
    reset(label, ctx);
  }

  private Checkpoint get(int index) {
    Preconditions.checkElementIndex(index, size());

    int key = checkpoints.headMap(index + 1).lastKey();
    Checkpoint c = checkpoints.get(key).deepCopy();
    deltas.get(key).subList(0, index - key).forEach(c::applyDelta);
    return c;
  }

  public int size() {
    return nextIndex;
  }

  public String getLabel(int index) {
    return labels.get(index);
  }

  // Updates `ctx` to be as it was at the given record index.
  public void rewindTo(StateContext ctx, int index) {
    Delta delta = Delta.compute(new StateContextView(ctx), get(index));
    applyDelta(delta, ctx);
  }

  // Removes all records > index.
  public void truncate(int index) {
    if (nextIndex == index + 1) {
      return;
    }

    current.reset(get(index));
    nextIndex = index + 1;

    checkpoints.tailMap(index + 1).clear();
    checkpointIndex = checkpoints.lastKey();

    List<Delta> keyDeltas = deltas.get(checkpointIndex);
    keyDeltas.subList(index - checkpointIndex, keyDeltas.size()).clear();
    deltaSum = keyDeltas.stream().mapToInt(Delta::size).sum();

    listeners.forEach(l -> l.truncated(nextIndex - 1));
  }

  public void reset(String label, StateContext ctx) {
    labels.clear();
    checkpoints.clear();
    deltas.clear();

    labels.add(label);
    current.reset(new StateContextView(ctx));
    checkpoints.put(0, current.deepCopy());
    checkpointIndex = 0;
    nextIndex = 1;
    deltaSum = 0;

    listeners.forEach(l -> l.truncated(0));
  }

  // Records the current state, with the given label. Returns its index.
  public void record(String label, StateContext ctx) {
    CheckpointInterface next = new StateContextView(ctx);
    Delta delta = Delta.compute(current, next);
    current.applyDelta(delta);

    if (deltaSum + delta.size() > 2 * current.size()) {
      // Make a new checkpoint.
      checkpoints.put(nextIndex, current.deepCopy());
      checkpointIndex = nextIndex;
      deltaSum = 0;
    } else {
      deltas.put(checkpointIndex, delta);
      deltaSum += delta.size();
    }

    int copy = nextIndex++;
    listeners.forEach(l -> l.newSnapshot(copy));
  }

  private void applyDelta(Delta delta, StateContext ctx) {
    ctx.checks().removeMultiple(delta.removedCheckIds());
    ctx.checks().addMultiple(delta.newChecks());

    if (!delta.updatedNotchCosts().isEmpty()) {
      List<Integer> updatedNotchCosts = new ArrayList<>(ctx.notchCosts().costs());
      delta.updatedNotchCosts().forEach(updatedNotchCosts::set);
      ctx.notchCosts().setCosts(updatedNotchCosts);
    }
  }

  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
