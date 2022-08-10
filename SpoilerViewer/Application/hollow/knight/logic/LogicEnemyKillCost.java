package hollow.knight.logic;

import java.util.Objects;
import com.google.gson.JsonObject;

public final class LogicEnemyKillCost implements Cost {

  private final Term canBenchWaypoint;
  private final Term defeatWaypoint;
  private final int amount;
  private final String enemyIcName;
  private final boolean respawns;

  private LogicEnemyKillCost(Term canBenchWaypoint, Term defeatWaypoint, int amount,
      String enemyIcName, boolean respawns) {
    this.canBenchWaypoint = canBenchWaypoint;
    this.defeatWaypoint = defeatWaypoint;
    this.amount = amount;
    this.enemyIcName = enemyIcName;
    this.respawns = respawns;
  }

  @Override
  public Term term() {
    return defeatWaypoint;
  }

  @Override
  public int value() {
    return amount;
  }

  @Override
  public Condition asCondition() {
    if (amount <= 0) {
      return Condition.alwaysTrue();
    } else if (amount == 1 || respawns) {
      return TermGreaterThanCondition.of(defeatWaypoint);
    } else {
      return Conjunction.of(TermGreaterThanCondition.of(canBenchWaypoint),
          TermGreaterThanCondition.of(defeatWaypoint));
    }
  }

  @Override
  public String debugString() {
    return "Kill " + amount + " " + enemyIcName;
  }

  @Override
  public JsonObject toRawSpoilerJson() {
    JsonObject obj = new JsonObject();
    obj.addProperty("$type", "TheRealJournalRando.Rando.LogicEnemyKillCost, TheRealJournalRando");
    obj.addProperty("CanBenchWaypoint", canBenchWaypoint.name());
    obj.addProperty("DefeatWaypoint", defeatWaypoint.name());
    obj.addProperty("Amount", amount);
    obj.addProperty("EnemyIcName", enemyIcName);
    obj.addProperty("Respawns", respawns);
    return obj;
  }

  @Override
  public JsonObject toICDLJson() throws ICDLException {
    throw new ICDLException("LogicEnemyKillCost plando not yet supported");
  }

  @Override
  public int hashCode() {
    return Objects.hash(LogicEnemyKillCost.class, canBenchWaypoint, defeatWaypoint, amount,
        enemyIcName, respawns);
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof LogicEnemyKillCost)) {
      return false;
    }

    LogicEnemyKillCost that = (LogicEnemyKillCost) o;
    return this.canBenchWaypoint.equals(that.canBenchWaypoint)
        && this.defeatWaypoint.equals(that.defeatWaypoint) && this.amount == that.amount
        && this.enemyIcName.contentEquals(that.enemyIcName) && this.respawns == that.respawns;
  }

  public static LogicEnemyKillCost parse(JsonObject obj) {
    Term canBenchWaypoint = Term.create(obj.get("CanBenchWaypoint").getAsString());
    Term defeatWaypoint = Term.create(obj.get("DefeatWaypoint").getAsString());
    int amount = obj.get("Amount").getAsInt();
    String enemyIcName = obj.get("EnemyIcName").getAsString();
    boolean respawns = obj.get("Respawns").getAsBoolean();

    return new LogicEnemyKillCost(canBenchWaypoint, defeatWaypoint, amount, enemyIcName, respawns);
  }
}
