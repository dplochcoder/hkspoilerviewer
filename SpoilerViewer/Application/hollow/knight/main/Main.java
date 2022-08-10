package hollow.knight.main;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import com.google.common.collect.ImmutableList;
import hollow.knight.gui.Application;
import hollow.knight.gui.Config;
import hollow.knight.gui.GuiUtil;
import hollow.knight.io.FileOpener;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;

public final class Main {
  private static final Version VERSION;
  static {
    try (InputStream is = Main.class.getResourceAsStream("VERSION");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr)) {
      VERSION = Version.parse(br.readLine());
    } catch (Exception ex) {
      throw new AssertionError(ex);
    }
  }

  public static final Version version() {
    return VERSION;
  }

  private static Config loadConfig(String[] args) {
    if (args.length > 0 && !args[0].toLowerCase().endsWith(".json")) {
      return Config.load(Paths.get(args[0]));
    } else {
      return Config.load();
    }
  }

  private static boolean hasJson(String[] args) {
    return args.length > 0 && args[0].toLowerCase().endsWith(".json");
  }

  private static Path findHkSpoiler(Config cfg, String[] args) throws Exception {
    if (hasJson(args)) {
      return Paths.get(args[0]);
    }

    if (!cfg.get("RAW_SPOILER").isEmpty()) {
      return Paths.get(cfg.get("RAW_SPOILER"));
    }

    // Make user open it.
    JFileChooser j = new JFileChooser("Find RawSpoiler.json");
    j.setFileFilter(new FileFilter() {
      @Override
      public boolean accept(File pathname) {
        return pathname.isDirectory() || pathname.getName().equals("RawSpoiler.json")
            || pathname.getName().equals("ctx.json");
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
        Path rawSpoiler = findHkSpoiler(cfg, args);
        if (rawSpoiler == null) {
          return;
        }

        FileOpener opener = new FileOpener(ImmutableList.of());
        ctx = opener.openFile(rawSpoiler);
        break;
      } catch (Exception ex) {
        GuiUtil.showStackTrace(null, "Error opening RawSpoiler.json: ", ex);

        if (hasJson(args)) {
          args = new String[0];
        } else {
          cfg.set("RAW_SPOILER", "");
          cfg.save();
        }
      }
    }

    new Application(ctx, cfg);
  }

  private Main() {}
}
