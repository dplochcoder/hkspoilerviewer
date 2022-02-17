package hollow.knight.logic;

import com.google.common.base.Verify;
import com.google.gson.JsonObject;

public final class Cost {
  public static enum Type {
    GEO, TERM,
  }

  private final Type type;
  private final int value;
  private final Term term;

  private Cost(Type type, int value, Term term) {
    this.type = type;
    this.value = value;
    this.term = term;
  }

  public Type type() {
    return type;
  }

  public int value() {
    return value;
  }

  public Term term() {
    Verify.verify(type() == Type.TERM);
    return term;
  }

  public boolean hasCostTerm(Term term) {
    return type == Type.TERM && this.term.equals(term);
  }

  public boolean canBePaid(boolean canPayGeo, TermMap values) {
    if (type == Type.GEO) {
      return canPayGeo;
    } else {
      return values.get(term) >= value;
    }
  }

  public String debugString() {
    if (type == Type.GEO) {
      return value + " geo";
    } else {
      return value + " " + term.name().toLowerCase();
    }
  }

  public static Cost parse(JsonObject obj) {
    if (obj.get("term") != null) {
      Term term = Term.create(obj.get("term").getAsString());
      int threshold = obj.get("threshold").getAsInt();
      return new Cost(Type.TERM, threshold, term);
    }

    int geo = obj.get("GeoAmount").getAsInt();
    return new Cost(Type.GEO, geo, null);
  }
}
