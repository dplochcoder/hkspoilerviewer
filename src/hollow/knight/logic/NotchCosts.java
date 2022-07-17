package hollow.knight.logic;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class NotchCosts implements StateContext.Mutable {
  private final List<Integer> notchCosts;

  public NotchCosts() {
    this.notchCosts = new ArrayList<>();
  }

  public ImmutableList<Integer> costs() {
    return ImmutableList.copyOf(notchCosts);
  }

  public void setCosts(List<Integer> notchCosts) {
    this.notchCosts.clear();
    this.notchCosts.addAll(notchCosts);
  }

  public int notchCost(int charmId) {
    return notchCosts.get(charmId - 1);
  }

  public JsonArray toRawSpoilerJsonArray() {
    JsonArray arr = new JsonArray();
    notchCosts.forEach(arr::add);
    return arr;
  }

  @Override
  public String saveName() {
    return "ICDLNotchCosts";
  }

  @Override
  public JsonObject save() {
    JsonObject obj = new JsonObject();
    obj.add("notchCosts", toRawSpoilerJsonArray());
    return obj;
  }

  @Override
  public void load(JsonObject json) {
    this.notchCosts.clear();
    for (JsonElement elem : json.get("notchCosts").getAsJsonArray()) {
      notchCosts.add(elem.getAsInt());
    }
  }
}
