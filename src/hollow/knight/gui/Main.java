package hollow.knight.gui;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import hollow.knight.logic.StateContext;
import hollow.knight.util.JsonUtil;

public final class Main {
  public static String VERSION = "1.2.1";

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

        ctx = StateContext.parse(JsonUtil.loadPath(rawSpoiler).getAsJsonObject());
        break;
      } catch (Exception ex) {
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
          ex.printStackTrace(pw);
          JOptionPane.showMessageDialog(null,
              "Error opening RawSpoiler.json: " + ex.getMessage() + ";\n" + sw.toString());
        }

        cfg.set("RAW_SPOILER", "");
        cfg.save();
      }
    }

    new Application(ctx);
  }

  private Main() {}
}
