package hollow.knight.logic;

import com.google.auto.value.AutoValue;

/**
 * A Term is a name with an integer value associated. It may be binary (such as acquiring an item,
 * defeating a boss, accessing a room) or it may be a multi-valued name such as boss essence,
 * dreamers, grimmkin flames, etc.
 */
@AutoValue
public abstract class Term {
  public abstract String name();
  
  public static Term create(String name) {
    return new AutoValue_Term(name);
  }
  
  private static final Term TRUE = Term.create("TRUE");
  public static Term true_() {
    return TRUE;
  }
  
  private static final Term GEO = Term.create("GEO");
  public static Term geo() {
    return GEO;
  }
  
  private static final Term ESSENCE = Term.create("ESSENCE");
  public static Term essence() {
    return ESSENCE;
  }
  
  private static final Term NOTCHES = Term.create("NOTCHES");
  public static Term notches() {
    return NOTCHES;
  }
  
  private static final Term CAN_REPLENISH_GEO = Term.create("Can_Replenish_Geo");
  public static Term canReplenishGeo() {
    return CAN_REPLENISH_GEO;
  }
}