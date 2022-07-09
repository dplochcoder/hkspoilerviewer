package hollow.knight.gui;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;

public final class SceneNicknames {

  private final ImmutableMap<String, String> nicknames;

  private SceneNicknames(Map<String, String> nicknames) {
    this.nicknames = ImmutableMap.copyOf(nicknames);
  }

  public String nickname(String transitionName) {
    return nicknames.getOrDefault(transitionName, transitionName);
  }

  public static SceneNicknames load() throws ParseException {
    JsonObject obj =
        JsonUtil.loadResource(SceneNicknames.class, "scene_nicknames.json").getAsJsonObject();

    Map<String, String> nicks = new HashMap<>();
    for (String scene : obj.keySet()) {
      JsonObject kObj = obj.get(scene).getAsJsonObject();
      String sName = kObj.get("Name").getAsString();

      JsonObject gObj = kObj.get("Gates").getAsJsonObject();
      for (String gate : gObj.keySet()) {
        String gName = gObj.get(gate).getAsString();
        nicks.put(scene + "[" + gate + "]", sName + "[" + gName + "]");
      }
    }
    return new SceneNicknames(nicks);
  }

}
