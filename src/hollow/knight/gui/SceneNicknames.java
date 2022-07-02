package hollow.knight.gui;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.ParseException;

public final class SceneNicknames {

  private ImmutableMap<String, String> nicknames;

  private SceneNicknames(Map<String, String> nicknames) {
    this.nicknames = ImmutableMap.copyOf(nicknames);
  }

  public void refresh() {
    try {
      nicknames = load().nicknames;
    } catch (ParseException ex) {
      throw new AssertionError("Impossible");
    }
  }

  public String nickname(String transitionName) {
    String exact = nicknames.get(transitionName);
    if (exact != null) {
      return exact;
    }

    int bIndex = transitionName.indexOf('[');
    if (bIndex != -1) {
      String prefix = transitionName.substring(0, bIndex);
      String replace = nicknames.get(prefix);
      if (replace != null) {
        return replace + transitionName.substring(bIndex);
      }
    }

    return transitionName;
  }

  public static SceneNicknames load() throws ParseException {
    JsonObject obj =
        JsonUtil.loadResource(SceneNicknames.class, "scene_nicknames.json").getAsJsonObject();

    Map<String, String> nicks = new HashMap<>();
    for (String key : obj.keySet()) {
      nicks.put(key, obj.get(key).getAsString());
    }
    return new SceneNicknames(nicks);
  }

}
