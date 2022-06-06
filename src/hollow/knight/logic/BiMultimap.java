package hollow.knight.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

// A bi-directional multimap, supporting many-to-many associations.
public final class BiMultimap<K, V> {

  private final Multimap<K, V> keysToValues = HashMultimap.create();
  private final Multimap<V, K> valuesToKeys = HashMultimap.create();

  public BiMultimap() {}

  public BiMultimap(BiMultimap<K, V> copy) {
    keysToValues.putAll(copy.keysToValues);
    valuesToKeys.putAll(copy.valuesToKeys);
  }

  public void put(K key, V value) {
    keysToValues.put(key, value);
    valuesToKeys.put(value, key);
  }

  public void putAll(BiMultimap<K, V> map) {
    map.keysToValues.forEach(this::put);
  }

  public void clear() {
    keysToValues.clear();
    valuesToKeys.clear();
  }

  public boolean remove(K key, V value) {
    return keysToValues.remove(key, value) && valuesToKeys.remove(value, key);
  }

  // Returns the set of values that no longer exist in the map.
  public Set<V> removeKey(K key) {
    Set<V> cleared = new HashSet<>();
    for (V value : keysToValues.removeAll(key)) {
      valuesToKeys.remove(value, key);
      if (!valuesToKeys.containsKey(value)) {
        cleared.add(value);
      }
    }
    return cleared;
  }

  // Returns the set of keys that no longer exist in the map.
  public Set<K> removeValue(V value) {
    Set<K> cleared = new HashSet<>();
    for (K key : valuesToKeys.removeAll(value)) {
      keysToValues.remove(key, value);
      if (!keysToValues.containsKey(key)) {
        cleared.add(key);
      }
    }
    return cleared;
  }

  public boolean containsKey(K key) {
    return keysToValues.containsKey(key);
  }

  public Collection<V> getValue(K key) {
    return keysToValues.get(key);
  }

  public boolean containsValue(V value) {
    return valuesToKeys.containsKey(value);
  }

  public Collection<K> getKey(V value) {
    return valuesToKeys.get(value);
  }
}
