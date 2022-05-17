package hollow.knight.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileFilter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.Query;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.util.GuiUtil;
import hollow.knight.util.JsonUtil;

public final class Application extends JFrame {
  private static final long serialVersionUID = 1L;

  private final SearchResult.FilterChangedListener filterChangedListener;
  private final SearchResultsListModel searchResultsListModel;
  private final RouteListModel routeListModel;
  private final ImmutableList<SaveInterface> saveInterfaces;

  private final Skips skips;
  private final SearchEngine searchEngine;

  private final JList<String> resultsList;
  private final JScrollPane resultsPane;
  private final JList<String> routeList;
  private final JScrollPane routePane;

  public Application(StateContext ctx) throws ParseException {
    this.filterChangedListener = () -> repopulateSearchResults();
    this.searchResultsListModel = new SearchResultsListModel();
    this.routeListModel = new RouteListModel(ctx);
    this.saveInterfaces = ImmutableList.of(searchResultsListModel, routeListModel);

    setTitle("HKSpoilerViewer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    setJMenuBar(createMenu());

    JPanel left = new JPanel();
    BoxLayout layout = new BoxLayout(left, BoxLayout.PAGE_AXIS);
    left.setLayout(layout);
    List<SearchResult.Filter> resultFilters = addFilters(left);

    this.skips = Skips.load();
    this.skips.setInitialState(ctx.newInitialState());
    this.skips.addListener(this::skipsUpdated);
    left.add(new JSeparator());
    this.skips.addToGui(left);

    this.searchEngine = new SearchEngine(ctx.roomLabels(), resultFilters);
    this.resultsList = createSearchResults();
    this.resultsPane = new JScrollPane(resultsList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    resultsPane.setMinimumSize(new Dimension(400, 600));

    this.routeList = createRouteList();
    this.routePane = new JScrollPane(routeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    routePane.setMinimumSize(new Dimension(250, 600));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(left, BorderLayout.LINE_START);
    getContentPane().add(resultsPane, BorderLayout.CENTER);
    getContentPane().add(routePane, BorderLayout.LINE_END);

    pack();
    repopulateSearchResults();
    setVisible(true);
  }

  private StateContext ctx() {
    return routeListModel.ctx();
  }

  private State currentState() {
    return routeListModel.currentState();
  }

  private static final ImmutableList<String> PL_INFO = ImmutableList.<String>builder().add(
      "A check is in Purchase Logic ($) if it has a cost which is not yet met by the current route, ")
      .add(
          "but which *could* be met if the player acquired all immediately accessible items affecting the cost, including tolerance.")
      .add("-")
      .add(
          "I.e., an item at Grubfather for N grubs is in Purchase Logic if the player has immediate access to N+TOLERANCE grubs with their current moveset and keys. ")
      .add(
          "Adding N or more grubs to the route explicitly will put the check in normal logic and remove the '$'.")
      .build();

  private static final ImmutableList<String> INSERT_INFO = ImmutableList.<String>builder().add(
      "Insertion allows you to rewind Logic to an earlier point in the route to insert earlier checks.")
      .add(
          "Your search results will reflect Logic at the point just prior to insertion, and any acquired item checks ")
      .add(
          "will be inserted into the middle of the route at that point. Grayed-out route items after the insertion ")
      .add("point will not appear in search results.").add("-")
      .add("Route items before and after the insertion point can still be removed or swapped.")
      .build();

  private static final ImmutableList<String> QUERIES_INFO = ImmutableList.<String>builder().add(
      "Queries enable partial spoiler formats by surfacing specific info about a seed without revealing all of it.")
      .add("Several pre-built queries are included, but custom ones can be used as well.").add("-")
      .add("See the queries.json source file for examples of how to author custom queries.")
      .build();

  private static final ImmutableList<String> KS_INFO =
      ImmutableList.<String>builder().add("UP/DOWN - move through results")
          .add("W/D - move selected item up/down (bookmarks+route)")
          .add("X - remove selected item (bookmarks+route)").add("-")
          .add("SPACE - acquire selected item").add("BACKSPACE - un-acquire last selected item")
          .add("-").add("I - Insert and search before selected route item")
          .add("K - Undo insertion point").add("-").add("B - bookmark selected item").add("-")
          .add("H - hide selected item").add("U - un-hide selected item").build();

  private ActionListener infoListener(String title, Iterable<String> content) {
    return new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        for (String info : content) {
          if (info.contentEquals("-")) {
            panel.add(new JSeparator());
          } else {
            panel.add(new JLabel(info));
          }
        }

        JDialog dialog = new JDialog(Application.this, title);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setVisible(true);
      }
    };
  }

  private void addBuiltinQueries(JMenu menu) throws ParseException {
    JsonArray queries = JsonUtil.loadResource(Application.class, "queries.json").getAsJsonArray();
    for (JsonElement json : queries) {
      JsonObject obj = json.getAsJsonObject();
      String name = obj.get("Name").getAsString();
      Query query = Query.parse(obj.get("Query").getAsJsonObject());

      JMenuItem qItem = new JMenuItem(name);
      qItem.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          executeQuery(query);
        }
      });
      menu.add(qItem);
    }
  }

  private JMenuBar createMenu() throws ParseException {
    JMenuBar bar = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem open = new JMenuItem("Open (*.hks)");
    file.add(open);
    JMenuItem save = new JMenuItem("Save (*.hks)");
    file.add(save);
    file.add(new JSeparator());
    JMenuItem saveToTxt = new JMenuItem("Save Route as *.txt");
    file.add(saveToTxt);
    bar.add(file);

    JMenu query = new JMenu("Query");
    addBuiltinQueries(query);
    query.add(new JSeparator());
    JMenuItem qFromFile = new JMenuItem("From file (*.hksq)");
    query.add(qFromFile);
    bar.add(query);

    JMenu about = new JMenu("About");
    JMenuItem pl = new JMenuItem("Purchase Logic ($)");
    about.add(pl);
    about.add(new JSeparator());
    JMenuItem insert = new JMenuItem("Insertions / Rewind");
    about.add(insert);
    about.add(new JSeparator());
    JMenuItem aboutQueries = new JMenuItem("Queries");
    about.add(aboutQueries);
    about.add(new JSeparator());
    JMenuItem ks = new JMenuItem("Keyboard Shortcuts");
    about.add(ks);
    about.add(new JSeparator());
    JMenuItem v = new JMenuItem("Version");
    about.add(v);
    bar.add(about);

    open.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          openFile();
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(Application.this, "Failed to open file: " + ex.getMessage(),
              "Error", JOptionPane.ERROR_MESSAGE);
        }

        repopulateSearchResults();
      }
    });

    save.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          saveFile();
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(Application.this, "Failed to save file: " + ex.getMessage(),
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    saveToTxt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Application.this.routeListModel.saveAsTxt(Application.this);
        } catch (IOException ignore) {
        }
      }
    });

    qFromFile.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        executeQueryFromFile();
      }
    });

    pl.addActionListener(infoListener("Purchase Logic ($)", PL_INFO));
    insert.addActionListener(infoListener("Insert / Rewind", INSERT_INFO));
    aboutQueries.addActionListener(infoListener("Queries", QUERIES_INFO));
    ks.addActionListener(infoListener("Keyboard Shortcuts", KS_INFO));
    v.addActionListener(
        infoListener("Version", ImmutableList.of("HKSpoilerViewer Version " + Main.VERSION, "-",
            "https://github.com/dplochcoder/hkspoilerviewer")));

    return bar;
  }

  private static final FileFilter HKS_FILTER = new FileFilter() {
    @Override
    public String getDescription() {
      return "Hollow Knight Spoiler (*.hks)";
    }

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".hks");
    }
  };

  private static final FileFilter QUERY_FILTER = new FileFilter() {
    @Override
    public String getDescription() {
      return "Hollow Knight Spoiler Query (*.hksq)";
    }

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".hksq");
    }
  };

  private void openFile() throws ParseException {
    JFileChooser c = new JFileChooser("Open");
    c.setFileFilter(HKS_FILTER);

    if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    JsonObject saveData = JsonUtil.loadPath(c.getSelectedFile().toPath()).getAsJsonObject();
    String version = saveData.get("Version").getAsString();
    StateContext newCtx = StateContext.parse(saveData.get("RawSpoiler").getAsJsonObject());
    saveInterfaces.forEach(i -> i.open(version, newCtx, saveData.get(i.saveName())));
    skips.setInitialState(newCtx.newInitialState());

    repopulateSearchResults();
  }

  private void saveFile() throws IOException {
    JFileChooser c = new JFileChooser("Save");
    c.setFileFilter(HKS_FILTER);

    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    JsonObject saveData = new JsonObject();
    saveData.add("Version", new JsonPrimitive(Main.VERSION));
    saveData.add("RawSpoiler", currentState().ctx().originalJson());
    saveInterfaces.forEach(i -> saveData.add(i.saveName(), i.save()));

    String path = c.getSelectedFile().getAbsolutePath();
    if (!path.endsWith(".hks")) {
      path = path + ".hks";
    }

    try (JsonWriter w = new JsonWriter(new FileWriter(path))) {
      Streams.write(saveData, w);
    }
  }

  private List<SearchResult.Filter> addFilters(JPanel parent) throws ParseException {
    ImmutableList<SearchResult.Filter> resultFilters =
        ImmutableList.of(new TextFilter(ctx().roomLabels()), ItemCategoryFilters.load(),
            new RoomFilters(ctx().roomLabels()), new ExclusionFilters(ctx().roomLabels()),
            routeListModel.futureCheckFilter());

    for (int i = 0; i < resultFilters.size(); i++) {
      if (i > 0) {
        parent.add(new JSeparator());
      }
      resultFilters.get(i).addListener(filterChangedListener);
      resultFilters.get(i).addGuiToPanel(parent);
    }

    return resultFilters;
  }

  private void skipsUpdated() {
    routeListModel.updateSkips(skips);
    repopulateSearchResults();
  }

  private JList<String> createSearchResults() {
    JList<String> resultsList = new JList<String>(searchResultsListModel);
    resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(resultsList.getKeyListeners()).forEach(resultsList::removeKeyListener);
    resultsList.addKeyListener(resultsListKeyListener());
    resultsList.setCellRenderer(resultsListCellRenderer());

    return resultsList;
  }

  private static final ImmutableMap<Integer, Integer> UP_DOWN_VALUES = ImmutableMap.of(
      KeyEvent.VK_UP, -1, KeyEvent.VK_DOWN, 1, KeyEvent.VK_PAGE_UP, -25, KeyEvent.VK_PAGE_DOWN, 25);

  private KeyListener resultsListKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        e.consume();
        if (e.getKeyCode() != KeyEvent.VK_B && e.getKeyCode() != KeyEvent.VK_W
            && e.getKeyCode() != KeyEvent.VK_S && e.getKeyCode() != KeyEvent.VK_X
            && e.getKeyCode() != KeyEvent.VK_DOWN && e.getKeyCode() != KeyEvent.VK_UP
            && e.getKeyCode() != KeyEvent.VK_H && e.getKeyCode() != KeyEvent.VK_U
            && e.getKeyCode() != KeyEvent.VK_PAGE_DOWN && e.getKeyCode() != KeyEvent.VK_PAGE_UP
            && e.getKeyCode() != KeyEvent.VK_SPACE && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_B) {
          searchResultsListModel.addBookmark(currentState(), resultsList.getSelectedIndex());
          resultsList.setSelectedIndex(searchResultsListModel.numBookmarks() - 1);
        } else if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S) {
          boolean up = e.getKeyCode() == KeyEvent.VK_W;
          searchResultsListModel.moveBookmark(currentState(), resultsList.getSelectedIndex(), up);
          resultsList.setSelectedIndex(resultsList.getSelectedIndex() + (up ? -1 : 1));
        } else if (e.getKeyCode() == KeyEvent.VK_X) {
          searchResultsListModel.deleteBookmark(currentState(), resultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_H) {
          searchResultsListModel.hideResult(currentState(), resultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_U) {
          searchResultsListModel.unhideResult(currentState(), resultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          SearchResult result =
              searchResultsListModel.getResult(currentState(), resultsList.getSelectedIndex());
          if (result == null) {
            return;
          }

          addToRoute(result);
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
          if (routeListModel.getSize() > 0) {
            routeListModel.removeCheck(routeListModel.getSize() - 1);
          }
        } else {
          // Navigate up or down.
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = resultsList.getSelectedIndex() + delta;
          if (newIndex < 0) {
            newIndex = 0;
          } else if (newIndex >= resultsList.getModel().getSize()) {
            newIndex = resultsList.getModel().getSize() - 1;
          }

          resultsList.setSelectedIndex(newIndex);
        }

        repopulateSearchResults();
      }

      @Override
      public void keyReleased(KeyEvent e) {}

      @Override
      public void keyTyped(KeyEvent e) {}
    };
  }

  private ListCellRenderer<? super String> resultsListCellRenderer() {
    return new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        Component c =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        searchResultsListModel.adjustForegroundColor(c, currentState(), searchEngine, index);
        return c;
      }
    };
  }

  private static boolean needsExpansion(JScrollPane pane) {
    return pane.getPreferredSize().width > pane.getSize().width;
  }

  private void repopulateSearchResults() {
    ImmutableList<SearchResult> results = searchEngine.getSearchResults(currentState());
    searchResultsListModel.updateResults(currentState(), results);

    if (needsExpansion(resultsPane) || needsExpansion(routePane)) {
      pack();
    }
    repaint();
  }

  private JList<String> createRouteList() {
    JList<String> routeList = new JList<>(routeListModel);
    Arrays.stream(routeList.getKeyListeners()).forEach(routeList::removeKeyListener);
    routeList.addKeyListener(routeListKeyListener());
    routeList.setCellRenderer(routeListCellRenderer());

    return routeList;
  }

  private void addToRoute(SearchResult result) {
    searchResultsListModel.removeBookmark(routeListModel.currentState(), result.itemCheck());
    searchResultsListModel.unhideResult(routeListModel.currentState(), result.itemCheck());
    routeListModel.addToRoute(result);

    repopulateSearchResults();
  }

  private KeyListener routeListKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        e.consume();
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
          routeListModel.removeCheck(routeListModel.getSize() - 1);
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S) {
          int oldIndex = routeList.getSelectedIndex();
          int newIndex = oldIndex + (e.getKeyCode() == KeyEvent.VK_W ? -1 : 1);
          if (newIndex < 0 || newIndex >= routeListModel.getSize()) {
            return;
          }

          routeListModel.swap(Math.min(oldIndex, newIndex), Math.max(oldIndex, newIndex));
          routeList.setSelectedIndex(newIndex);
        } else if (e.getKeyCode() == KeyEvent.VK_X) {
          routeListModel.removeCheck(routeList.getSelectedIndex());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_I) {
          routeListModel.setInsertionPoint(routeList.getSelectedIndex());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_K) {
          routeListModel.setInsertionPoint(routeListModel.getSize());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
          int newIndex = routeList.getSelectedIndex() + (e.getKeyCode() == KeyEvent.VK_UP ? -1 : 1);
          if (newIndex >= 0 && newIndex < routeListModel.getSize()) {
            routeList.setSelectedIndex(newIndex);
          }
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {}

      @Override
      public void keyTyped(KeyEvent e) {}
    };
  }

  private ListCellRenderer<? super String> routeListCellRenderer() {
    return new DefaultListCellRenderer() {
      private static final long serialVersionUID = 1L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
          boolean isSelected, boolean cellHasFocus) {
        Component c =
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        routeListModel.adjustForegroundColor(c, index);
        return c;
      }
    };
  }

  private Query getQueryFromFile() throws ParseException {
    JFileChooser c = new JFileChooser("Query");
    c.setFileFilter(QUERY_FILTER);

    if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return null;
    }

    JsonObject queryData = JsonUtil.loadPath(c.getSelectedFile().toPath()).getAsJsonObject();
    return Query.parse(queryData);
  }

  private void executeQueryFromFile() {
    Query query;
    try {
      query = getQueryFromFile();
    } catch (Exception ex) {
      GuiUtil.showStackTrace(this, "Failed to parse Query", ex);
      return;
    }

    if (query != null) {
      executeQuery(query);
    }
  }

  private void executeQuery(Query query) {
    // Copy to clipboard.
    String results = query.execute(currentState());
    StringSelection sel = new StringSelection(results);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);

    String msg = results + "\n\n(Copied to clipboard!)";
    JOptionPane.showMessageDialog(this, msg, "Query results", JOptionPane.INFORMATION_MESSAGE);
  }
}
