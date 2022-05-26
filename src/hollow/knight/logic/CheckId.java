package hollow.knight.logic;

import com.google.auto.value.AutoValue;

/**
 * An opaque identifier which describes a specific item placement location.
 * 
 * <p>
 * Multi-locations, like shops, can have multiple UniqueLocations within them.
 */
@AutoValue
public abstract class CheckId implements Comparable<CheckId> {

  public abstract long id();

  @Override
  public int compareTo(CheckId other) {
    return Long.compare(id(), other.id());
  }

  public static CheckId of(long id) {
    return new AutoValue_CheckId(id);
  }

}
