package hollow.knight.logic;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;

/**
 * A Term is a name with an integer value associated. It may be binary (such as acquiring an item,
 * defeating a boss, accessing a room) or it may be a multi-valued name such as boss essence,
 * dreamers, grimmkin flames, etc.
 */
@AutoValue
public abstract class Term implements Comparable<Term> {
  public abstract String name();

  @Override
  public final int compareTo(Term term) {
    return name().compareTo(term.name());
  }

  public static Term create(String name) {
    return new AutoValue_Term(name);
  }

  // TODO: Order these.
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

  private static final Term GRUBS = Term.create("GRUBS");

  public static Term grubs() {
    return GRUBS;
  }

  private static final Term RANCID_EGGS = Term.create("RANCIDEGGS");

  public static Term rancidEggs() {
    return RANCID_EGGS;
  }

  private static final Term CHARMS = Term.create("CHARMS");

  public static Term charms() {
    return CHARMS;
  }

  private static final Term SCREAM = Term.create("SCREAM");

  public static Term scream() {
    return SCREAM;
  }

  private static final ImmutableSet<Term> COST_TERMS =
      ImmutableSet.of(GRUBS, ESSENCE, RANCID_EGGS, CHARMS);

  public static ImmutableSet<Term> costTerms() {
    return COST_TERMS;
  }

  private static final Term NOTHING = Term.create("Lumafly_Escape");

  public static Term nothing() {
    return NOTHING;
  }
}
