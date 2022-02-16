package hollow.knight.gui;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import hollow.knight.logic.StateContext;
import hollow.knight.util.JsonUtil;

public final class Main {
  public static String VERSION = "1.0";

  private static Config loadConfig(String[] args) {
    if (args.length > 0) {
      return Config.load(Paths.get(args[0]));
    } else {
      return Config.load();
    }
  }

  private static Path findHkSpoiler(Config cfg) throws Exception {
    if (!cfg.get("RAW_SPOILER").isEmpty()) {
      return Paths.get(cfg.get("RAW_SPOILER"));
    }

    // Make user open it.
    JFileChooser j = new JFileChooser("Find RawSpoiler.json");
    j.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().contentEquals("RawSpoiler.json");
      }

      @Override
      public String getDescription() {
        return "RawSpoiler.json";
      }
    });
    if (j.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
      throw new Exception("Must choose RawSpoiler.json");
    }

    Path p = Paths.get(j.getSelectedFile().getAbsolutePath());

    cfg.set("RAW_SPOILER", p.toString());
    cfg.save();
    return p;
  }

  public static void main(String[] args) throws Exception {
    Config cfg = loadConfig(args);

    Path rawSpoiler = findHkSpoiler(cfg);
    StateContext ctx = StateContext.parse(JsonUtil.loadPath(rawSpoiler).getAsJsonObject());

    new Application(ctx);
  }

  private Main() {}
}
