package hollow.knight.logic;

import com.google.gson.JsonObject;

public interface Cost {
  Term term();

  int value();

  String debugString();

  JsonObject toRawSpoilerJson();

  JsonObject toICDLJson() throws ICDLException;

  default int geoCost() {
    return 0;
  }

  default int termCost(Term term) {
    return 0;
  }

  static Cost parse(JsonObject obj) throws ParseException {
    if (obj.get("term") != null) {
      Term term = Term.create(obj.get("term").getAsString());
      int threshold = obj.get("threshold").getAsInt();
      return TermCost.create(term, threshold);
    } else if (obj.get("GeoAmount") != null) {
      int geo = obj.get("GeoAmount").getAsInt();
      return GeoCost.create(geo);
    } else if (obj.get("$type").getAsString().contains("TheRealJournalRando")) {
      return LogicEnemyKillCost.parse(obj);
    } else {
      throw new ParseException("Unrecognized Cost type");
    }
  }
}
