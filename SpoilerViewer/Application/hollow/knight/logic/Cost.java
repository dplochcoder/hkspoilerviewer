package hollow.knight.logic;

import com.google.gson.JsonObject;

public interface Cost {
  Term term();

  int value();

  Condition asCondition();

  String debugString();

  JsonObject toRawSpoilerJson();

  JsonObject toICDLJson() throws ICDLException;

  default int geoCost() {
    return 0;
  }

  default int termCost(Term term) {
    return 0;
  }

  static Cost parse(JsonObject obj) {
    if (obj.get("term") != null) {
      Term term = Term.create(obj.get("term").getAsString());
      int threshold = obj.get("threshold").getAsInt();
      return TermCost.create(term, threshold);
    }

    int geo = obj.get("GeoAmount").getAsInt();
    return GeoCost.create(geo);
  }
}
