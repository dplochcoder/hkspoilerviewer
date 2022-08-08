package hollow.knight.logic;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public final class SynchronizedEntityManager<T> {
  private final Object mutex = new Object();
  private final Set<T> entities = new HashSet<>();

  public void add(T entity) {
    synchronized (mutex) {
      entities.add(entity);
    }
  }

  public void remove(T entity) {
    synchronized (mutex) {
      entities.remove(entity);
    }
  }

  public void forEach(Consumer<T> action) {
    Set<T> copy;
    synchronized (mutex) {
      copy = new HashSet<>(entities);
    }
    copy.forEach(action);
  }
}
