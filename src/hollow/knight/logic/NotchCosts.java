package hollow.knight.logic;

import java.util.ArrayList;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class NotchCosts {
  private final List<Integer> notchCosts;

  private NotchCosts(List<Integer> notchCosts) {
    this.notchCosts = new ArrayList<>(notchCosts);
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

  public static NotchCosts parse(JsonObject json) {
    ImmutableList.Builder<Integer> notchCosts = ImmutableList.builder();
    for (JsonElement elem : json.get("notchCosts").getAsJsonArray()) {
      notchCosts.add(elem.getAsInt());
    }
    return new NotchCosts(notchCosts.build());
  }
}
