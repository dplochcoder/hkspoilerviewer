package hollow.knight.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
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
import com.google.common.primitives.Ints;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import hollow.knight.logic.CheckId;
import hollow.knight.logic.ICDLException;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.Query;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;
import hollow.knight.logic.Version;
import hollow.knight.util.GuiUtil;
import hollow.knight.util.JsonUtil;

public final class Application extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Config cfg;
  private final SearchResult.FilterChangedListener filterChangedListener;
  private final SearchResultsListModel searchResultsListModel;
  private final RouteListModel routeListModel;
  private final ImmutableList<SaveInterface> saveInterfaces;
  private final ImmutableList<ItemChecks.Listener> checksListeners;

  private boolean isICDL = false;
  private final JMenu icdlMenu;
  private final JMenuItem openEditor;
  private CheckEditor checkEditor;
  private final JMenuItem saveICDLFolder;

  private final Skips skips;
  private final SearchEngine searchEngine;

  private final JList<String> searchResultsList;
  private final JScrollPane searchResultsPane;
  private final JList<String> routeList;
  private final JScrollPane routePane;

  public Application(StateContext ctx, Config cfg) throws ParseException {
    this.cfg = cfg;
    this.filterChangedListener = () -> repopulateSearchResults();
    this.searchResultsListModel = new SearchResultsListModel();
    this.routeListModel = new RouteListModel(ctx);
    this.saveInterfaces = ImmutableList.of(searchResultsListModel, routeListModel);
    this.checksListeners = ImmutableList.of(searchResultsListModel, routeListModel);

    this.checksListeners.forEach(ctx.checks()::addListener);

    setTitle("HKSpoilerViewer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    openEditor = new JMenuItem("Open Editor");
    saveICDLFolder = new JMenuItem("Save As ICDL Pack Folder");
    icdlMenu = createICDLMenu();
    setJMenuBar(createMenu());

    JPanel left = new JPanel();
    BoxLayout layout = new BoxLayout(left, BoxLayout.PAGE_AXIS);
    left.setLayout(layout);
    this.skips = createSkips();
    List<SearchResult.Filter> resultFilters = addFilters(left);

    this.searchEngine = new SearchEngine(ctx.roomLabels(), resultFilters);
    this.searchResultsList = createSearchResults();
    this.searchResultsPane = new JScrollPane(searchResultsList,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    searchResultsPane.setMinimumSize(new Dimension(400, 600));

    this.routeList = createRouteList();
    this.routePane = new JScrollPane(routeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    routePane.setMinimumSize(new Dimension(250, 600));

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(left, BorderLayout.LINE_START);
    getContentPane().add(searchResultsPane, BorderLayout.CENTER);
    getContentPane().add(routePane, BorderLayout.LINE_END);

    pack();
    repopulateSearchResults();
    setVisible(true);
  }

  public StateContext ctx() {
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

  private static final ImmutableList<String> KS_INFO = ImmutableList.<String>builder()
      .add("Q - clear current selection").add("-").add("UP/DOWN - move through results")
      .add("W/S - move selected item up/down (bookmarks+route)")
      .add("X - remove selected item (bookmarks+route)").add("-")
      .add("SPACE - acquire selected item").add("BACKSPACE - un-acquire last selected item")
      .add("-").add("I - Insert and search before selected route item")
      .add("K - Undo insertion point").add("-").add("B - bookmark selected item").add("-")
      .add("H - hide selected item").add("U - un-hide selected item").add("-")
      .add("E - (ICDL) edit selected check in the check editor") // FIXME
      .add("Z - (ICDL) delete selected check")
      .add("C - (ICDL) copy current item onto selected check")
      .add("D - (ICDL) duplicate the selected check (mostly for shops)").build();

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

  public void refreshLogic(boolean refreshSearchResults) {
    routeListModel.refreshLogic();
    if (refreshSearchResults) {
      repopulateSearchResults();
    }

    if (checkEditor != null) {
      checkEditor.repopulateItemResults();
    }
  }

  public ItemCheck getSelectedSearchResultCheck() {
    return searchResultsListModel.getCheck(searchResultsList.getSelectedIndex());
  }

  public ItemCheck getSelectedRouteCheck() {
    return routeListModel.getCheck(routeList.getSelectedIndex());
  }

  private static enum CheckEditorPresence {
    NONE, ALREADY_OPEN, OPEN_NOW;

    public boolean isOpen() {
      return this != CheckEditorPresence.NONE;
    }

    public boolean wasOpen() {
      return this == CheckEditorPresence.ALREADY_OPEN;
    }
  }

  private CheckEditorPresence ensureCheckEditor() {
    if (checkEditor != null) {
      checkEditor.requestFocus();
      return CheckEditorPresence.ALREADY_OPEN;
    } else if (!isICDL) {
      JOptionPane.showMessageDialog(this, "Must open an ICDL ctx.json file for this action",
          "Requires ICDL", JOptionPane.ERROR_MESSAGE);
      return CheckEditorPresence.NONE;
    } else {
      openEditor();
      return CheckEditorPresence.OPEN_NOW;
    }
  }

  private boolean ensureICDL() {
    if (isICDL) {
      return true;
    } else {
      JOptionPane.showMessageDialog(this, "Open an ICDL ctx.json file for this action");
      return false;
    }
  }

  public boolean ensureRandomized(ItemCheck check) {
    if (check.vanilla()) {
      JOptionPane.showMessageDialog(this, "Cannot edit vanilla checks.", "Not Allowed",
          JOptionPane.WARNING_MESSAGE);
      return false;
    }

    return true;
  }

  private boolean editCheck(ItemCheck check) {
    if (!ensureCheckEditor().isOpen()) {
      return false;
    }

    if (check == null || !ensureRandomized(check)) {
      return false;
    }

    checkEditor.editCheck(check);
    return true;
  }

  public void copyCheckEditorItem(boolean refreshSearchResults, ItemCheck check) {
    if (!ensureCheckEditor().wasOpen()) {
      return;
    }

    Item item = checkEditor.selectedItem();
    if (item == null || check == null || item.term().equals(check.item().term())
        || !ensureRandomized(check)) {
      return;
    }

    ItemCheck searchCheck = getSelectedSearchResultCheck();
    ItemCheck routeCheck = getSelectedRouteCheck();

    CheckId newId =
        ctx().checks().replace(check.id(), check.location(), item, check.costs(), false);
    refreshLogic(refreshSearchResults);

    if (searchCheck == check) {
      searchResultsList
          .setSelectedIndex(searchResultsListModel.indexOfSearchResult(ctx().checks().get(newId)));
    }
    if (routeCheck == check) {
      routeList.setSelectedIndex(routeListModel.indexOfRouteCheck(ctx().checks().get(newId)));
    }
  }

  private void deleteCheck(ItemCheck check) {
    if (!ensureICDL() || check == null || !ensureRandomized(check)) {
      return;
    }

    ItemCheck searchCheck = getSelectedSearchResultCheck();
    ItemCheck routeCheck = getSelectedRouteCheck();

    ctx().checks().reduceToNothing(c -> c == check);
    refreshLogic(false);

    if (searchCheck == check) {
      searchResultsList.clearSelection();
    }
    if (routeCheck == check) {
      routeList.clearSelection();
    }
  }

  public void duplicateCheck(ItemCheck check) {
    if (!ensureICDL() || check == null) {
      return;
    }

    ctx().checks().placeNew(check.location(), check.item(), check.costs(), false);
    refreshLogic(false);
  }

  private void editStartingGeo() {
    ItemCheck check = ctx().checks().startChecks().filter(c -> c.item().hasEffectTerm(Term.geo()))
        .findFirst().get();
    int geo = check.item().getEffectValue(Term.geo());

    String newGeo =
        JOptionPane.showInputDialog(this, "Enter new Starting Geo", String.valueOf(geo));
    if (newGeo == null || newGeo.trim().isEmpty()) {
      return;
    }

    Integer value = Ints.tryParse(newGeo.trim());
    if (value == null || value < 0) {
      JOptionPane.showMessageDialog(this, "Must be a non-negative integer");
      return;
    }

    ctx().checks().replace(check.id(), check.location(), Item.newGeoItem(value), check.costs(),
        false);
    refreshLogic(true);
  }

  private void editNotchCosts() {
    if (NotchCostsEditor.editNotchCosts(this, ctx())) {
      refreshLogic(true);
    }
  }

  private JMenuItem icdlReset(String name, Predicate<ItemCheck> filter) {
    JMenuItem item = new JMenuItem(name);
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctx().checks().reduceToNothing(filter);
        refreshLogic(true);
      }
    });

    return item;
  }

  private JMenu createICDLMenu() {
    JMenu menu = new JMenu("ICDL");
    menu.setEnabled(false);
    menu.setToolTipText("Open an ICDL ctx.json file to enable ICDL features");

    JMenu reset = new JMenu("Reset All");
    reset.add(icdlReset("All Randomized Checks", c -> true));
    reset.add(icdlReset("Matching Search Results", searchResultsListModel::isMatchingSearchResult));
    menu.add(reset);

    menu.add(new JSeparator());
    JMenuItem editStartingGeo = new JMenuItem("Edit Starting Geo");
    editStartingGeo.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editStartingGeo();
      }
    });
    menu.add(editStartingGeo);

    JMenuItem editNotches = new JMenuItem("Edit Charm Costs");
    editNotches.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        editNotchCosts();
      }
    });
    menu.add(editNotches);

    menu.add(new JSeparator());
    openEditor.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        openEditor();
      }
    });
    menu.add(openEditor);

    menu.add(new JSeparator());
    saveICDLFolder.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          saveICDLFolder();
        } catch (IOException | ICDLException ex) {
          GuiUtil.showStackTrace(Application.this, "Failed to save ICDL folder", ex);
        }
      }
    });
    menu.add(saveICDLFolder);

    return menu;
  }

  private void openEditor() {
    checkEditor = new CheckEditor(Application.this);
    openEditor.setEnabled(false);
    openEditor.setToolTipText("Editor is already open");
  }

  public void editorClosed() {
    checkEditor = null;
    openEditor.setEnabled(true);
    openEditor.setToolTipText("");
  }

  private JMenuBar createMenu() throws ParseException {
    JMenuBar bar = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem open = new JMenuItem("Open");
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

    bar.add(icdlMenu);

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
          GuiUtil.showStackTrace(Application.this, "Open file failed", ex);
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
          GuiUtil.showStackTrace(Application.this, "Save file failed", ex);
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
        infoListener("Version", ImmutableList.of("HKSpoilerViewer Version " + Main.version(), "-",
            "https://github.com/dplochcoder/hkspoilerviewer")));

    return bar;
  }

  private static final FileFilter HKS_OPEN_FILTER = new FileFilter() {
    @Override
    public String getDescription() {
      return "Hollow Knight Spoiler (*.hks, RawSpoiler.json, ctx.json)";
    }

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".hks")
          || f.getName().contentEquals("RawSpoiler.json") || f.getName().contentEquals("ctx.json");
    }
  };

  private static final FileFilter HKS_SAVE_FILTER = new FileFilter() {
    @Override
    public String getDescription() {
      return "Hollow Knight Spoiler (*.hks)";
    }

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().endsWith(".hks");
    }
  };

  private static final FileFilter ICDL_FOLDER_FILTER = new FileFilter() {
    @Override
    public String getDescription() {
      return "ICDL Pack Folder";
    }

    @Override
    public boolean accept(File f) {
      return f.isDirectory();
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

  private void setICDLEnabled(boolean enable) {
    this.isICDL = enable;
    icdlMenu.setEnabled(enable);
    icdlMenu.setToolTipText(enable ? "" : "Open an ICDL ctx.json file to enable ICDL features");
  }

  private void openFile() throws ParseException, IOException {
    StateContext prevCtx = routeListModel.ctx();
    JFileChooser c = new JFileChooser("Open");
    c.setFileFilter(HKS_OPEN_FILTER);

    if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    Path p = c.getSelectedFile().toPath();
    boolean isRawSpoiler = !p.toString().endsWith(".hks");

    JsonObject saveData = new JsonObject();
    JsonObject rawSpoiler = JsonUtil.loadPath(p).getAsJsonObject();
    JsonObject rawICDL = null;
    Version version = Main.version();
    if (!isRawSpoiler) {
      saveData = rawSpoiler;
      rawSpoiler = saveData.get("RawSpoiler").getAsJsonObject();

      version = Version.parse(saveData.get("Version").getAsString());
      if (version.major() < Main.version().major()) {
        throw new ParseException("Unsupported version " + version + " < " + Main.version());
      }

      if (saveData.has("RawICDL")) {
        rawICDL = saveData.get("RawICDL").getAsJsonObject();
      }
    } else if (p.endsWith("ctx.json")) {
      String parent = p.getParent().toString();
      rawICDL = JsonUtil.loadPath(Paths.get(parent, "ic.json")).getAsJsonObject();
    }

    setICDLEnabled(rawICDL != null);

    Version finalVersion = version;
    JsonObject finalSaveData = saveData;

    StateContext newCtx = StateContext.parse(rawSpoiler, rawICDL);
    if (isICDL) {
      newCtx.loadMutables(saveData);
    }
    saveInterfaces.forEach(i -> i.open(finalVersion, newCtx, finalSaveData.get(i.saveName())));
    skips.setInitialState(newCtx.newInitialState());

    checksListeners.forEach(prevCtx.checks()::removeListener);
    checksListeners.forEach(newCtx.checks()::addListener);

    repopulateSearchResults();

    if (isRawSpoiler && !isICDL) {
      int option = JOptionPane.showConfirmDialog(this, "Open this RawSpoiler.json on startup?");
      if (option == JOptionPane.OK_OPTION) {
        cfg.set("RAW_SPOILER", p.toAbsolutePath().toString());
        cfg.save();
      }
    }

    if (checkEditor != null) {
      checkEditor.dispose();
      checkEditor = null;
    }
  }

  private void saveFile() throws IOException {
    JFileChooser c = new JFileChooser("Save");
    c.setFileFilter(HKS_SAVE_FILTER);

    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    JsonObject saveData = new JsonObject();
    saveData.add("Version", new JsonPrimitive(Main.version().toString()));
    saveData.add("RawSpoiler", ctx().rawSpoilerJson());
    if (isICDL) {
      saveData.add("RawICDL", ctx().icdlJson());
      ctx().checks().compact();
      ctx().saveMutables(saveData);
    }
    saveInterfaces.forEach(i -> saveData.add(i.saveName(), i.save()));

    String path = c.getSelectedFile().getAbsolutePath();
    if (!path.endsWith(".hks")) {
      path = path + ".hks";
    }

    JsonUtil.writeJson(path, saveData);
  }

  private void saveICDLFolder() throws IOException, ICDLException {
    JFileChooser c = new JFileChooser("Save");
    c.setFileFilter(ICDL_FOLDER_FILTER);

    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    ctx().saveICDL(c.getSelectedFile().toPath());
  }

  private Skips createSkips() throws ParseException {
    Skips skips = Skips.load();
    skips.setInitialState(ctx().newInitialState());
    skips.addListener(this::skipsUpdated);
    return skips;
  }

  private List<SearchResult.Filter> addFilters(JPanel parent) throws ParseException {
    ImmutableList.Builder<SearchResult.Filter> searchFilters = ImmutableList.builder();

    TextFilter textFilter = new TextFilter(ctx().roomLabels());
    textFilter.addListener(filterChangedListener);
    textFilter.addGuiToPanel(parent);
    searchFilters.add(textFilter);

    parent.add(new JSeparator());
    ItemCategoryFilters itemFilter = ItemCategoryFilters.load();
    itemFilter.addListener(filterChangedListener);
    itemFilter.addGuiToPanel(parent);
    searchFilters.add(itemFilter);

    parent.add(new JSeparator());
    RoomFilters roomsFilter = new RoomFilters(ctx().roomLabels());
    roomsFilter.addListener(filterChangedListener);
    roomsFilter.addGuiToPanel(parent);
    searchFilters.add(roomsFilter);

    parent.add(new JSeparator());
    this.skips.addToGui(parent);

    parent.add(new JSeparator());
    ExclusionFilters excFilters = new ExclusionFilters(ctx().roomLabels());
    excFilters.addListener(filterChangedListener);
    excFilters.addGuiToPanel(parent);
    searchFilters.add(excFilters);

    // No GUI
    searchFilters.add(routeListModel.futureCheckFilter());

    return searchFilters.build();
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
    return new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        e.consume();

        // TODO: Make key codes configurable.
        if (e.getKeyCode() != KeyEvent.VK_Q && e.getKeyCode() != KeyEvent.VK_B
            && e.getKeyCode() != KeyEvent.VK_W && e.getKeyCode() != KeyEvent.VK_S
            && e.getKeyCode() != KeyEvent.VK_X && e.getKeyCode() != KeyEvent.VK_DOWN
            && e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_H
            && e.getKeyCode() != KeyEvent.VK_U && e.getKeyCode() != KeyEvent.VK_E
            && e.getKeyCode() != KeyEvent.VK_C && e.getKeyCode() != KeyEvent.VK_D
            && e.getKeyCode() != KeyEvent.VK_Z && e.getKeyCode() != KeyEvent.VK_PAGE_DOWN
            && e.getKeyCode() != KeyEvent.VK_PAGE_UP && e.getKeyCode() != KeyEvent.VK_SPACE
            && e.getKeyCode() != KeyEvent.VK_BACK_SPACE) {
          return;
        }

        if (e.getKeyCode() == KeyEvent.VK_Q) {
          searchResultsList.clearSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_B) {
          searchResultsListModel.addBookmark(searchResultsList.getSelectedIndex());
          searchResultsList.setSelectedIndex(searchResultsListModel.numBookmarks() - 1);
        } else if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S) {
          boolean up = e.getKeyCode() == KeyEvent.VK_W;
          searchResultsListModel.moveBookmark(searchResultsList.getSelectedIndex(), up);
          searchResultsList.setSelectedIndex(searchResultsList.getSelectedIndex() + (up ? -1 : 1));
        } else if (e.getKeyCode() == KeyEvent.VK_X) {
          searchResultsListModel.deleteBookmark(currentState(),
              searchResultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_H) {
          searchResultsListModel.hideResult(searchResultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_U) {
          searchResultsListModel.unhideResult(searchResultsList.getSelectedIndex());
        } else if (e.getKeyCode() == KeyEvent.VK_E) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (editCheck(check) && getSelectedRouteCheck() != check) {
            routeList.clearSelection();
          }
          return;
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
          copyCheckEditorItem(false, getSelectedSearchResultCheck());
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          duplicateCheck(getSelectedSearchResultCheck());
        } else if (e.getKeyCode() == KeyEvent.VK_Z) {
          deleteCheck(getSelectedSearchResultCheck());
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (check == null) {
            return;
          }

          addToRoute(check);
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
          if (routeListModel.getSize() > 0) {
            routeListModel.removeCheck(routeListModel.getSize() - 1);
          }
        } else {
          // Navigate up or down.
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = searchResultsList.getSelectedIndex() + delta;
          if (newIndex < 0) {
            newIndex = 0;
          } else if (newIndex >= searchResultsList.getModel().getSize()) {
            newIndex = searchResultsList.getModel().getSize() - 1;
          }

          searchResultsList.setSelectedIndex(newIndex);
        }

        repopulateSearchResults();
      }
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

    if (needsExpansion(searchResultsPane) || needsExpansion(routePane)) {
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

  private void addToRoute(ItemCheck check) {
    searchResultsListModel.removeBookmark(check);
    searchResultsListModel.unhideResult(check);
    routeListModel.addToRoute(check);

    repopulateSearchResults();
  }

  private KeyListener routeListKeyListener() {
    return new KeyListener() {
      @Override
      public void keyPressed(KeyEvent e) {
        e.consume();
        if (e.getKeyCode() == KeyEvent.VK_Q) {
          routeList.clearSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
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
        } else if (e.getKeyCode() == KeyEvent.VK_E) {
          ItemCheck check = getSelectedRouteCheck();
          if (editCheck(check) && getSelectedSearchResultCheck() != check) {
            searchResultsList.clearSelection();
          }
          refreshLogic(true);
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
          duplicateCheck(getSelectedRouteCheck());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_Z) {
          deleteCheck(getSelectedRouteCheck());
          refreshLogic(true);
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
