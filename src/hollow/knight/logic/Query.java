package hollow.knight.logic;

import com.google.gson.JsonObject;

// Generic interface for querying active state.
public interface Query {
  String execute(State state);

  static Query parse(JsonObject json) throws ParseException {
    String type = json.get("Type").getAsString();
    switch (type) {
      case "ItemCounts":
        return ItemCountsQuery.parse(json);
      default:
        throw new ParseException("Unknown Query type: " + type);
    }
  }
}
