package hollow.knight.logic;

import com.google.auto.value.AutoValue;

/**
 * An opaque identifier which describes a specific item placement location.
 * 
 * <p>
 * Multi-locations, like shops, can have multiple UniqueLocations within them.
 */
@AutoValue
public abstract class CheckId {

  public abstract long id();

  public static CheckId of(long id) {
    return new AutoValue_CheckId(id);
  }

}
