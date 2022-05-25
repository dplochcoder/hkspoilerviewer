package hollow.knight.logic;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Version {
  public abstract int major();

  public abstract int minor();

  public abstract int patch();

  public static Version parse(String txt) throws ParseException {
    String[] args = txt.split("\\.");
    int major = Integer.parseInt(args[0]);
    int minor = Integer.parseInt(args[1]);
    int patch = 0;
    if (args.length == 3) {
      patch = Integer.parseInt(args[2]);
    } else if (args.length != 2) {
      throw new ParseException("Invalid version: " + txt);
    }

    return new AutoValue_Version(major, minor, patch);
  }

  @Override
  public String toString() {
    return major() + "." + minor() + (patch() != 0 ? "." + patch() : "");
  }
}
