package hollow.knight.io;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;
import hollow.knight.logic.ICDLException;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Version;
import hollow.knight.main.Main;

public final class FileOpener {
  private final ImmutableList<SaveInterface> saveInterfaces;

  public FileOpener(List<SaveInterface> saveInterfaces) {
    this.saveInterfaces = ImmutableList.copyOf(saveInterfaces);
  }

  public StateContext openFile(Path path) throws ParseException, ICDLException {
    boolean isHKS = path.toString().endsWith(".hks");
    String parent = path.getParent().toAbsolutePath().toString();

    JsonObject saveData = new JsonObject();
    JsonObject rawSpoiler = JsonUtil.loadPath(path).getAsJsonObject();
    JsonObject rawICDL = null;
    JsonObject logicEdits = null;

    Path darknessPath = Paths.get(parent, "DarknessSpoiler.json");
    JsonObject darknessJson = null;

    Version version = Main.version();
    if (isHKS) {
      saveData = rawSpoiler;
      rawSpoiler = saveData.get("RawSpoiler").getAsJsonObject();

      version = Version.parse(saveData.get("Version").getAsString());
      if (version.major() < Main.version().major()) {
        throw new ParseException("Unsupported version " + version + " < " + Main.version());
      }

      if (saveData.has("RawICDL")) {
        rawICDL = saveData.get("RawICDL").getAsJsonObject();
      }
      if (saveData.has("RawDarkness") && !saveData.get("RawDarkness").isJsonNull()) {
        darknessJson = saveData.get("RawDarkness").getAsJsonObject();
      }
    } else if (path.endsWith("ctx.json")) {
      rawICDL = JsonUtil.loadPath(Paths.get(parent, "ic.json")).getAsJsonObject();
    } else {
      if (new File(darknessPath.toString()).exists()) {
        darknessJson =
            JsonUtil.loadPath(Paths.get(parent, "DarknessSpoiler.json")).getAsJsonObject();
      }
    }

    StateContext newCtx = StateContext.parse(isHKS, rawSpoiler, rawICDL, darknessJson);
    if (rawICDL != null) {
      newCtx.loadMutables(saveData);
    }

    Version finalVersion = version;
    JsonObject finalSaveData = saveData;
    saveInterfaces.forEach(i -> i.open(finalVersion, newCtx, finalSaveData.get(i.saveName())));

    return newCtx;
  }
}
