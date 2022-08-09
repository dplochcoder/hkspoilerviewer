package hollow.knight.logic;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class Version {
  public abstract int major();

  public abstract int minor();

  public abstract int build();

  public abstract int patch();

  public static Version parse(String txt) throws ParseException {
    String[] args = txt.split("\\.");
    if (args.length < 2 || args.length > 4) {
      throw new ParseException("Invalid version: " + txt);
    }

    int major = Integer.parseInt(args[0]);
    int minor = Integer.parseInt(args[1]);
    int build = 0;
    int patch = 0;
    if (args.length >= 3) {
      build = Integer.parseInt(args[2]);
      if (args.length >= 4) {
        patch = Integer.parseInt(args[3]);
      }
    }

    return new AutoValue_Version(major, minor, build, patch);
  }

  @Override
  public String toString() {
    return major() + "." + minor() + (build() + patch() != 0 ? "." + build() : "")
        + (patch() != 0 ? "." + patch() : "");
  }
}
