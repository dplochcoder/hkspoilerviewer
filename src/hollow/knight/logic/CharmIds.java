package hollow.knight.logic;

import java.util.HashMap;
import java.util.Map;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import hollow.knight.io.JsonUtil;

public final class CharmIds {
  private final ImmutableMap<Term, Integer> charmIds;

  private CharmIds(Map<Term, Integer> charmIds) {
    this.charmIds = ImmutableMap.copyOf(charmIds);
  }

  public ImmutableSet<Term> charmTerms() {
    return charmIds.keySet();
  }

  public Integer charmId(Term charm) {
    return charmIds.get(charm);
  }

  public static CharmIds load() throws ParseException {
    JsonObject obj = JsonUtil.loadResource(CharmIds.class, "charm_ids.json").getAsJsonObject();

    Map<Term, Integer> charmIds = new HashMap<>();
    for (String key : obj.keySet()) {
      charmIds.put(Term.create(key), obj.get(key).getAsInt());
    }

    return new CharmIds(charmIds);
  }
}
