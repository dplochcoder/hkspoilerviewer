package hollow.knight.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Config {
  private final Path path;
  private final Map<String, String> props;
  
  private Config(Path path, Map<String, String> props) {
    this.path = path;
    this.props = new HashMap<>(props);
  }
  
  public String get(String prop) {
    return get(prop, "");
  }
  
  public String get(String prop, String defaultValue) {
    return props.getOrDefault(prop, defaultValue);
  }
  
  public boolean isEmpty() {
    return props.isEmpty();
  }
  
  public void set(String prop, String value) {
    if (value.isEmpty()) {
      props.remove(prop);
    } else {
      props.put(prop, value);
    }
  }
  
  public void save() throws IOException {
    StringBuilder content = new StringBuilder();
    props.keySet().stream().sorted().forEachOrdered(k -> {
      content.append(k);
      content.append('=');
      content.append(props.get(k));
      content.append('\n');
    });
    
    Files.write(path, content.toString().getBytes(StandardCharsets.UTF_8));
  }
  
  public static Config load() {
    Path abs = Paths.get(System.getProperty("user.dir"), "HKSpoilerViewer.cfg").toAbsolutePath();
    return load(abs);
  }
  
  public static Config load(Path path) {
    Map<String, String> props = new HashMap<>();
    try {
      for (String line : Files.readAllLines(path)) {
        String[] parts = line.split("=", 2);
        if (parts.length != 2) continue;
        
        props.put(parts[0], parts[1]);
      }
    } catch (IOException ex) {
      props.clear();
    }
    
    return new Config(path, props);
  }
}