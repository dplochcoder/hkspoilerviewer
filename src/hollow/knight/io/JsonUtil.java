package hollow.knight.io;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import hollow.knight.logic.ParseException;

public final class JsonUtil {
  public static JsonElement loadResource(Class<?> clazz, String fname) throws ParseException {
    try (InputStream is = clazz.getResourceAsStream(fname);
        InputStreamReader isr = new InputStreamReader(is)) {
      return JsonParser.parseReader(isr);
    } catch (IOException ex) {
      throw new ParseException("Failed to load " + fname + ": " + ex.getMessage());
    }
  }

  public static JsonElement loadPath(Path path) throws ParseException {
    try {
      List<String> lines = Files.readAllLines(path);
      String txt = lines.stream().collect(Collectors.joining("\n"));
      return JsonParser.parseString(txt);
    } catch (IOException ex) {
      throw new ParseException("Failed to load " + path + ": " + ex.getMessage());
    }
  }

  public static void writeJson(String path, JsonElement json) throws IOException {
    try (JsonWriter w = new JsonWriter(new FileWriter(path.toString()))) {
      w.setIndent("  ");
      Streams.write(json, w);
    }
  }

  private JsonUtil() {}
}
