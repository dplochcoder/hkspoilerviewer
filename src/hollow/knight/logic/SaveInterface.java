package hollow.knight.logic;

import com.google.gson.JsonElement;

public interface SaveInterface {
  String saveName();

  JsonElement save();

  void open(Version version, StateContext ctx, JsonElement json);
}
