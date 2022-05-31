package hollow.knight.main;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import com.google.common.collect.ImmutableList;
import hollow.knight.gui.Application;
import hollow.knight.gui.Config;
import hollow.knight.gui.GuiUtil;
import hollow.knight.io.FileOpener;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;

public final class Main {
  private static final String VERSION = "2.0";

  private static final Version TYPED_VERSION;
  static {
    try {
      TYPED_VERSION = Version.parse(VERSION);
    } catch (ParseException ex) {
      throw new AssertionError(ex);
    }
  }

  public static final Version version() {
    return TYPED_VERSION;
  }

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
      return null;
    }

    Path p = Paths.get(j.getSelectedFile().getAbsolutePath());

    cfg.set("RAW_SPOILER", p.toString());
    cfg.save();
    return p;
  }

  public static void main(String[] args) throws Exception {
    Config cfg = loadConfig(args);

    StateContext ctx;
    while (true) {
      try {
        Path rawSpoiler = findHkSpoiler(cfg);
        if (rawSpoiler == null) {
          return;
        }

        FileOpener opener = new FileOpener(ImmutableList.of());
        ctx = opener.openFile(rawSpoiler);
        break;
      } catch (Exception ex) {
        GuiUtil.showStackTrace(null, "Error opening RawSpoiler.json: ", ex);

        cfg.set("RAW_SPOILER", "");
        cfg.save();
      }
    }

    new Application(ctx, cfg);
  }

  private Main() {}
}
