package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import hollow.knight.util.JsonUtil;

public final class Pools {

  private final ImmutableSet<String> poolNames;
  private final ImmutableMap<Term, String> poolsByTerm;

  private Pools(Map<Term, String> poolsByTerm) {
    this.poolsByTerm = ImmutableMap.copyOf(poolsByTerm);
    this.poolNames = this.poolsByTerm.values().stream().distinct().sorted()
        .collect(ImmutableSet.toImmutableSet());
  }

  public ImmutableSet<String> pools() {
    return poolNames;
  }

  public String getPool(Term term) {
    return poolsByTerm.getOrDefault(term, "");
  }

  public static Pools load() throws ParseException {
    JsonArray arr = JsonUtil.loadResource(Pools.class, "pools.json").getAsJsonArray();

    Map<Term, String> poolsByTerm = new HashMap<>();
    for (JsonElement pool : arr) {
      JsonObject obj = pool.getAsJsonObject();

      String poolName = obj.get("Name").getAsString();
      if (poolName.equals("SalubraNotch")) {
        // pools.json makes two groups for these but we only care about 'Notch'
        poolName = "Notch";
      }

      for (JsonElement item : obj.get("IncludeItems").getAsJsonArray()) {
        poolsByTerm.put(Term.create(item.getAsString()), poolName);
      }
    }

    return new Pools(poolsByTerm);
  }

}
