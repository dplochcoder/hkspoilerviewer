package hollow.knight.logic;

import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class ItemCountsQuery implements Query {

  private static interface LabelExtractor {
    String getLabel(ItemCheck placement, RoomLabels roomLabels);
  }

  private final ImmutableSet<Term> items;
  private final LabelExtractor labelExtractor;

  @Override
  public String execute(State state) {
    Multiset<String> labels = HashMultiset.create();
    state.ctx().checks().allChecks().forEach(p -> {
      if (items.contains(p.item().term())) {
        labels.add(labelExtractor.getLabel(p, state.ctx().roomLabels()));
      }
    });

    return labels.elementSet().stream().sorted().map(l -> l + ": " + labels.count(l))
        .collect(Collectors.joining("\n"));
  }

  private ItemCountsQuery(Set<Term> items, LabelExtractor labelExtractor) {
    this.items = ImmutableSet.copyOf(items);
    this.labelExtractor = labelExtractor;
  }

  private static LabelExtractor parseLabelExtractor(String type) throws ParseException {
    switch (type) {
      case "MAP_AREA":
        return (check, labels) -> labels.get(check.location().scene(), RoomLabels.Type.MAP);
      case "TITLE_AREA":
        return (check, labels) -> labels.get(check.location().scene(), RoomLabels.Type.TITLE);
      case "SCENE":
        return (check, labels) -> check.location().scene();
      case "LOCATION":
        return (check, labels) -> check.location().name();
      default:
        throw new ParseException("Unknown label type: " + type);
    }
  }

  public static ItemCountsQuery parse(JsonObject json) throws ParseException {
    ImmutableSet.Builder<Term> items = ImmutableSet.builder();
    for (JsonElement elem : json.get("Items").getAsJsonArray()) {
      items.add(Term.create(elem.getAsString()));
    }

    return new ItemCountsQuery(items.build(),
        parseLabelExtractor(json.get("LabelType").getAsString()));
  }

}
