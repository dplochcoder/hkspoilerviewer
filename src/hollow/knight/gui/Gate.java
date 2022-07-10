package hollow.knight.gui;

import com.google.auto.value.AutoValue;
import com.google.common.base.Verify;

@AutoValue
public abstract class Gate {
  public abstract String sceneName();

  public abstract String gateName();

  public static Gate parse(String gate) {
    int l = gate.indexOf('[');
    int r = gate.indexOf(']');
    Verify.verify(l > 0 && l < r && r == gate.length() - 1, gate);

    return new AutoValue_Gate(gate.substring(0, l), gate.substring(l + 1, r));
  }
}
