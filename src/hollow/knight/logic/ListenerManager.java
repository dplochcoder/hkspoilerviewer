package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class ListenerManager<I> {
  private final Object mutex = new Object();
  private final Set<I> listeners = new HashSet<>();

  public void add(I listener) {
    synchronized (mutex) {
      listeners.add(listener);
    }
  }

  public void remove(I listener) {
    synchronized (mutex) {
      listeners.remove(listener);
    }
  }

  public void forEach(Consumer<I> action) {
    Set<I> copy;
    synchronized (mutex) {
      copy = new HashSet<>(listeners);
    }
    copy.forEach(action);
  }
}
