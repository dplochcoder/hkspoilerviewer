package hollow.knight.logic;

import com.google.gson.JsonElement;

public interface SaveInterface {
  String saveName();

  JsonElement save();

  void open(String version, State initialState, JsonElement json);
}
