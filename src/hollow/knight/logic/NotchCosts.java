package hollow.knight.logic;

import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class NotchCosts {
  private final ImmutableList<Integer> notchCosts;

  private NotchCosts(List<Integer> notchCosts) {
    this.notchCosts = ImmutableList.copyOf(notchCosts);
  }

  public int notchCost(int charmId) {
    return notchCosts.get(charmId - 1);
  }

  public static NotchCosts parse(JsonObject json) {
    ImmutableList.Builder<Integer> notchCosts = ImmutableList.builder();
    for (JsonElement elem : json.get("notchCosts").getAsJsonArray()) {
      notchCosts.add(elem.getAsInt());
    }
    return new NotchCosts(notchCosts.build());
  }
}
