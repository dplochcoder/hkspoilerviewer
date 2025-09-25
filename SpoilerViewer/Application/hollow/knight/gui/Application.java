package hollow.knight.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBoxMenuItem;
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
import hollow.knight.io.FileOpener;
import hollow.knight.io.JsonUtil;
import hollow.knight.logic.CheckId;
import hollow.knight.logic.ICDLException;
import hollow.knight.logic.Item;
import hollow.knight.logic.ItemCheck;
import hollow.knight.logic.ItemChecks;
import hollow.knight.logic.ItemChecks.Missing;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.Query;
import hollow.knight.logic.SaveInterface;
import hollow.knight.logic.State;
import hollow.knight.logic.StateContext;
import hollow.knight.logic.Term;
import hollow.knight.main.Main;

public final class Application extends JFrame {
  private static final long serialVersionUID = 1L;

  private final Config cfg;
  private final TransitionData transitionData;
  private final SearchResult.FilterChangedListener filterChangedListener;
  private final RouteListModel routeListModel;
  private final SearchResultsListModel searchResultsListModel;
  private final TransitionVisualizerPlacements transitionVisualizerPlacements;
  private final ImmutableList<SaveInterface> saveInterfaces;
  private final ImmutableList<ItemChecks.Listener> checksListeners;

  private final SingletonWindow<TransitionVisualizer> transitionVisualizer;

  private boolean isICDL = false;
  private final JMenu icdlMenu;
  private final SingletonWindow<CheckEditor> checkEditor;
  private final SingletonWindow<LogicEditor> logicEditor;
  private final JMenuItem saveICDLFolder;

  private final SearchEngine searchEngine;

  private final JCheckBoxMenuItem showRawTransitions;
  private final JList<String> searchResultsList;
  private final JScrollPane searchResultsPane;
  private final JList<String> routeList;
  private final JScrollPane routePane;
  private final JLabel startLocLabel;
  private final List<RouteCounter> routeCounters;

  public Application(StateContext ctx, Config cfg) throws ParseException {
    this.cfg = cfg;
    this.transitionData = TransitionData.load(ctx.roomLabels());
    this.showRawTransitions = new JCheckBoxMenuItem("Raw Transitions");
    this.filterChangedListener = () -> repopulateSearchResults();
    this.routeListModel = new RouteListModel(transitionData, ctx);
    this.searchResultsListModel = new SearchResultsListModel(transitionData,
        () -> showRawTransitions.getState(), () -> routeListModel.ctx().darkness(), this::isRouted);
    this.transitionVisualizerPlacements = new TransitionVisualizerPlacements();
    this.saveInterfaces =
        ImmutableList.of(searchResultsListModel, routeListModel, transitionVisualizerPlacements);
    this.checksListeners = ImmutableList.of(searchResultsListModel, routeListModel);

    this.checksListeners.forEach(ctx.checks()::addListener);

    setTitle("HKSpoilerViewer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    this.transitionVisualizer =
        new SingletonWindow<>(this, "Transition Visualizer", () -> new TransitionVisualizer(this));
    this.checkEditor = new SingletonWindow<>(this, "Check Editor", () -> new CheckEditor(this));
    this.logicEditor = new SingletonWindow<>(this, "Logic Editor", () -> new LogicEditor(this));
    this.saveICDLFolder = new JMenuItem("Export As ICDL Pack Folder");
    this.icdlMenu = createICDLMenu();
    setJMenuBar(createMenu());

    setICDLEnabled(ctx.icdlJson() != null);

    JPanel left = new JPanel();
    BoxLayout layout = new BoxLayout(left, BoxLayout.PAGE_AXIS);
    left.setLayout(layout);
    List<SearchResult.Filter> resultFilters = addFilters(left);

    this.searchEngine = new SearchEngine(transitionData, ctx.roomLabels(), resultFilters);
    this.searchResultsList = createSearchResults();
    this.searchResultsPane = new JScrollPane(searchResultsList,
        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    searchResultsPane.setMinimumSize(new Dimension(400, 600));

    JPanel rightPane = new JPanel();
    rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.PAGE_AXIS));

    this.routeList = createRouteList();
    this.routePane = new JScrollPane(routeList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    routePane.setMinimumSize(new Dimension(250, 600));
    rightPane.add(routePane);

    this.routeCounters = createRouteCounters();
    JPanel countersPane = new JPanel();
    countersPane.setLayout(new GridLayout(routeCounters.size() + 1, 1));

    startLocLabel = new JLabel();
    updateStartLoc(ctx);
    countersPane.add(startLocLabel);

    routeCounters.forEach(c -> countersPane.add(c.getLabel()));
    countersPane.setMaximumSize(new Dimension(1_000_000, 160));
    rightPane.add(countersPane);

    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(left, BorderLayout.LINE_START);
    getContentPane().add(searchResultsPane, BorderLayout.CENTER);
    getContentPane().add(rightPane, BorderLayout.LINE_END);

    pack();
    refreshLogic();
    setVisible(true);
  }

  public TransitionData transitionData() {
    return transitionData;
  }

  public TransitionVisualizerPlacements transitionVisualizerPlacements() {
    return transitionVisualizerPlacements;
  }

  public StateContext ctx() {
    return routeListModel.ctx();
  }

  public State currentState() {
    return routeListModel.currentState();
  }

  public boolean isRouted(ItemCheck c) {
    return routeListModel.finalState().isAcquired(c);
  }

  private static final ImmutableList<String> INSERT_INFO = ImmutableList.<String>builder().add(
      "Insertion allows you to rewind to an earlier point in the route to insert earlier checks.")
      .add("Any acquired item checks will be inserted into the middle of the route at that point. ")
      .add("Grayed-out route items after the insertion point will not appear in search results.")
      .add("-")
      .add("Route items before and after the insertion point can still be removed or swapped.")
      .build();

  private static final ImmutableList<String> ICDL_INFO = ImmutableList.<String>builder().add(
      "ICDL edit mode allows you to create plandos, by setting the items at every check location.")
      .add(
          "You can save your progress while working as an .hks file to preserve all information, including the item counts for the originally generated seed.")
      .add(
          "When done, select 'Export as ICDL pack folder' to save your work in a format that can be opened in HK.")
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

  private static final ImmutableList<String> VERSION_INFO =
      ImmutableList.of("HKSpoilerViewer Version " + Main.version(), "-",
          "https://github.com/dplochcoder/hkspoilerviewer");

  private void addBuiltinQueries(JMenu menu) throws ParseException {
    JsonArray queries = JsonUtil.loadResource(Application.class, "queries.json").getAsJsonArray();
    for (JsonElement json : queries) {
      JsonObject obj = json.getAsJsonObject();
      String name = obj.get("Name").getAsString();
      Query query = Query.parse(obj.get("Query").getAsJsonObject());

      JMenuItem qItem = new JMenuItem(name);
      qItem.addActionListener(GuiUtil.newActionListener(this, () -> executeQuery(query)));
      menu.add(qItem);
    }
  }

  public void refreshLogic() {
    routeListModel.refreshLogic();
    repopulateSearchResults();
    checkEditor.ifOpen(e -> e.repopulateItemResults());
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
    if (checkEditor.isOpen()) {
      checkEditor.getWithFocus();
      return CheckEditorPresence.ALREADY_OPEN;
    } else if (!isICDL) {
      JOptionPane.showMessageDialog(this, "Must open an ICDL ctx.json file for this action",
          "Requires ICDL", JOptionPane.ERROR_MESSAGE);
      return CheckEditorPresence.NONE;
    } else {
      checkEditor.getWithFocus();
      return CheckEditorPresence.OPEN_NOW;
    }
  }

  public boolean isICDL() {
    return isICDL;
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

  public boolean ensureRandomizedNonTransition(ItemCheck check) {
    if (!ensureRandomized(check)) {
      return false;
    }

    if (check.isTransition()) {
      JOptionPane.showMessageDialog(this, "Use the Transition Visualizer to edit Transitions",
          "Not Allowed", JOptionPane.WARNING_MESSAGE);
      return false;
    }

    return true;
  }

  public boolean editCheck(ItemCheck check) {
    if (!ensureCheckEditor().isOpen()) {
      return false;
    }

    if (check == null || !ensureRandomizedNonTransition(check)) {
      return false;
    }

    checkEditor.get().editCheck(check);
    return true;
  }

  public void copyItemToCheck(Item item, ItemCheck check) {
    if (item.term().equals(check.item().term())) {
      return;
    }

    ItemCheck searchCheck = getSelectedSearchResultCheck();
    ItemCheck routeCheck = getSelectedRouteCheck();

    CheckId newId =
        ctx().checks().replace(check.id(), check.location(), item, check.costs(), false);
    refreshLogic();

    if (searchCheck == check) {
      searchResultsList
          .setSelectedIndex(searchResultsListModel.indexOfSearchResult(ctx().checks().get(newId)));
    }
    if (routeCheck == check) {
      routeList.setSelectedIndex(routeListModel.indexOfRouteCheck(ctx().checks().get(newId)));
    }
  }

  public void copyCheckEditorItem(ItemCheck check) {
    if (!ensureCheckEditor().wasOpen()) {
      return;
    }

    Item item = checkEditor.get().selectedItem();
    if (item == null || check == null || !ensureRandomizedNonTransition(check)) {
      return;
    }

    copyItemToCheck(item, check);
  }

  public void deleteCheck(ItemCheck check) {
    if (!ensureICDL() || check == null || !ensureRandomized(check)) {
      return;
    }

    ItemCheck searchCheck = getSelectedSearchResultCheck();
    ItemCheck routeCheck = getSelectedRouteCheck();

    try {
      ctx().checks().reduceToNothing(c -> c == check);
    } catch (ICDLException ex) {
      GuiUtil.showStackTrace(this, "Failed to delete", ex);
      return;
    }
    refreshLogic();

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

    if (check.isTransition()) {
      JOptionPane.showMessageDialog(this, "Cannot duplicate transitions", "Not Allowed",
          JOptionPane.WARNING_MESSAGE);
      return;
    }

    ctx().checks().placeNew(check.location(), check.item(), check.costs(), false);
    refreshLogic();
  }

  private void showItemDiffReport() {
    ImmutableMap<String, Integer> diff = ctx().checks().getICDLItemDiff();
    if (diff.isEmpty()) {
      JOptionPane.showMessageDialog(this, "No diff!");
    } else {
      StringBuilder sb = new StringBuilder();
      sb.append("Have placed:\n");
      diff.forEach((k, v) -> {
        sb.append(v > 0 ? "+" : "");
        sb.append(v);
        sb.append(' ');
        sb.append(k);
        sb.append('\n');
      });
      JOptionPane.showMessageDialog(this, sb.toString(), "ICDL Item Diff Report",
          JOptionPane.INFORMATION_MESSAGE);
    }
  }

  private void editNotchCosts() {
    NotchCostsEditor editor = new NotchCostsEditor(ctx());
    if (editor.performEdit(this)) {
      refreshLogic();
    }
  }

  private void editTolerances() {
    TolerancesEditor editor = new TolerancesEditor(ctx());
    if (editor.performEdit(this)) {
      refreshLogic();
    }
  }

  private JMenuItem icdlReset(String name, Predicate<ItemCheck> filter) {
    JMenuItem item = new JMenuItem(name);
    item.addActionListener(GuiUtil.newActionListener(this, () -> {
      ctx().checks().reduceToNothing(filter);
      refreshLogic();
    }));

    return item;
  }

  private JMenu createICDLMenu() {
    JMenu menu = new JMenu("ICDL");

    JMenu reset = new JMenu("Reset All");
    reset.add(icdlReset("All Randomized Checks", c -> !c.isTransition()));
    reset.add(icdlReset("All Randomized Transitions", c -> c.isTransition()));
    reset.add(icdlReset("Matching Search Results", searchResultsListModel::isMatchingSearchResult));
    menu.add(reset);

    menu.add(new JSeparator());
    JMenuItem missingItems = new JMenuItem("Item Diff Report");
    missingItems.addActionListener(GuiUtil.newActionListener(this, this::showItemDiffReport));
    menu.add(missingItems);

    JMenuItem editLogic = new JMenuItem("Edit Logic");
    editLogic.addActionListener(GuiUtil.newActionListener(this, () -> logicEditor.getWithFocus()));
    menu.add(editLogic);

    JMenuItem editNotches = new JMenuItem("Edit Charm Costs");
    editNotches.addActionListener(GuiUtil.newActionListener(this, this::editNotchCosts));
    menu.add(editNotches);

    JMenuItem editTolerances = new JMenuItem("Edit Tolerances");
    editTolerances.addActionListener(GuiUtil.newActionListener(this, this::editTolerances));
    menu.add(editTolerances);

    menu.add(new JSeparator());
    menu.add(checkEditor.getMenuItem());

    menu.add(new JSeparator());
    JMenuItem importHKS = new JMenuItem("Import HKS");
    importHKS.addActionListener(GuiUtil.newActionListener(this, this::importHKS));
    menu.add(importHKS);

    menu.add(new JSeparator());
    saveICDLFolder.addActionListener(GuiUtil.newActionListener(this, this::saveICDLFolder));
    menu.add(saveICDLFolder);

    return menu;
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

    JMenu view = new JMenu("View");
    view.add(transitionVisualizer.getMenuItem());
    view.add(showRawTransitions);
    bar.add(view);

    JMenu query = new JMenu("Query");
    addBuiltinQueries(query);
    query.add(new JSeparator());
    JMenuItem qFromFile = new JMenuItem("From file (*.hksq)");
    query.add(qFromFile);
    bar.add(query);

    bar.add(icdlMenu);

    JMenu about = new JMenu("About");
    about.add(GuiUtil.newInfoMenuItem(this, "Insertions / Rewind", INSERT_INFO));
    about.add(new JSeparator());
    about.add(GuiUtil.newInfoMenuItem(this, "ICDL", ICDL_INFO));
    about.add(new JSeparator());
    about.add(GuiUtil.newInfoMenuItem(this, "Queries", QUERIES_INFO));
    about.add(new JSeparator());
    about.add(GuiUtil.newInfoMenuItem(this, "Keyboard Shortcuts", KS_INFO));
    about.add(new JSeparator());
    about.add(GuiUtil.newInfoMenuItem(this, "Version", VERSION_INFO));
    bar.add(about);

    open.addActionListener(GuiUtil.newActionListener(this, this::openFile));
    save.addActionListener(GuiUtil.newActionListener(this, this::saveFile));
    saveToTxt
        .addActionListener(GuiUtil.newActionListener(this, () -> routeListModel.saveAsTxt(this)));

    qFromFile.addActionListener(GuiUtil.newActionListener(this, this::executeQueryFromFile));

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

  private void openFile() throws ParseException, IOException, ICDLException {
    StateContext prevCtx = routeListModel.ctx();
    JFileChooser c = new JFileChooser("Open");
    c.setFileFilter(HKS_OPEN_FILTER);

    if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    Path path = c.getSelectedFile().toPath().toAbsolutePath();
    FileOpener opener = new FileOpener(saveInterfaces);
    StateContext newCtx = opener.openFile(path);

    updateStartLoc(newCtx);
    setICDLEnabled(newCtx.icdlJson() != null);
    checksListeners.forEach(prevCtx.checks()::removeListener);
    checksListeners.forEach(newCtx.checks()::addListener);

    if (!newCtx.isHKS() && !isICDL) {
      int option = JOptionPane.showConfirmDialog(this, "Open this RawSpoiler.json on startup?");
      if (option == JOptionPane.OK_OPTION) {
        cfg.set("RAW_SPOILER", path.toString());
        cfg.save();
      }
    }

    transitionVisualizer.close();
    checkEditor.close();
    refreshLogic();
  }

  private void saveFile() throws IOException, ICDLException {
    JFileChooser c = new JFileChooser("Save");
    c.setFileFilter(HKS_SAVE_FILTER);

    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    JsonObject saveData = new JsonObject();
    saveData.add("Version", new JsonPrimitive(Main.version().toString()));
    saveData.add("RawSpoiler", ctx().rawSpoilerJson());
    saveData.add("RawDarkness", ctx().darkness().toJson());
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

  private void importHKS() throws IOException, ParseException, ICDLException {
    JFileChooser c = new JFileChooser("Import");
    c.setFileFilter(HKS_SAVE_FILTER);

    if (c.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    TransitionVisualizerPlacements newPlacements = new TransitionVisualizerPlacements();
    FileOpener opener = new FileOpener(ImmutableList.of(newPlacements));
    StateContext newCtx = opener.openFile(c.getSelectedFile().toPath());

    Missing missing = ctx().checks().overlayImportChecks(newCtx.checks());
    if (!missing.empty()) {
      JOptionPane.showMessageDialog(this, "Failed to import items at " + missing.locations()
          + " unknown locations, and " + missing.items() + " items at known locations");
    }
    if (JOptionPane.showConfirmDialog(this, "Import notch costs?") == JOptionPane.OK_OPTION) {
      ctx().notchCosts().setCosts(newCtx.notchCosts().costs());
    }
    if (JOptionPane.showConfirmDialog(this,
        "Import transition visualizer placements?") == JOptionPane.OK_OPTION) {
      transitionVisualizerPlacements.reset(newPlacements);
    }

    if (checkEditor != null) {
      editCheck(null);
    }
    searchResultsList.clearSelection();
    routeList.clearSelection();
    refreshLogic();
  }

  private void saveICDLFolder() throws IOException, ICDLException {
    JFileChooser c = new JFileChooser("Save");
    c.setFileFilter(ICDL_FOLDER_FILTER);

    if (c.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    ctx().checks().compact();
    ctx().saveICDL(c.getSelectedFile().toPath());
  }

  private List<SearchResult.Filter> addFilters(JPanel parent) throws ParseException {
    ImmutableList.Builder<SearchResult.Filter> searchFilters = ImmutableList.builder();

    TextFilter textFilter = new TextFilter(transitionData, ctx().roomLabels());
    textFilter.addListener(filterChangedListener);
    textFilter.addGuiToPanel(parent);
    searchFilters.add(textFilter);

    parent.add(new JSeparator());
    ItemCategoryFilters itemFilter = new ItemCategoryFilters();
    itemFilter.addListener(filterChangedListener);
    itemFilter.addGuiToPanel(parent);
    searchFilters.add(itemFilter);

    parent.add(new JSeparator());
    RoomFilters roomsFilter = new RoomFilters(ctx().roomLabels());
    roomsFilter.addListener(filterChangedListener);
    roomsFilter.addGuiToPanel(parent);
    searchFilters.add(roomsFilter);

    parent.add(new JSeparator());
    ExclusionFilters excFilters = new ExclusionFilters(ctx().roomLabels(), routeListModel);
    excFilters.addListener(filterChangedListener);
    excFilters.addGuiToPanel(parent);
    searchFilters.add(excFilters);

    return searchFilters.build();
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

        if (e.getKeyCode() == KeyEvent.VK_Q) {
          searchResultsList.clearSelection();
        } else if (e.getKeyCode() == KeyEvent.VK_B) {
          searchResultsListModel.addBookmark(searchResultsList.getSelectedIndex());
          searchResultsList.setSelectedIndex(searchResultsListModel.numBookmarks() - 1);
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_W || e.getKeyCode() == KeyEvent.VK_S) {
          boolean up = e.getKeyCode() == KeyEvent.VK_W;
          searchResultsListModel.moveBookmark(searchResultsList.getSelectedIndex(), up);
          searchResultsList.setSelectedIndex(searchResultsList.getSelectedIndex() + (up ? -1 : 1));
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_X) {
          searchResultsListModel.deleteBookmark(currentState(),
              searchResultsList.getSelectedIndex());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_H) {
          searchResultsListModel.hideResult(searchResultsList.getSelectedIndex());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_U) {
          searchResultsListModel.unhideResult(searchResultsList.getSelectedIndex());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_E) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (editCheck(check) && getSelectedRouteCheck() != check) {
            routeList.clearSelection();
          }
        } else if (e.getKeyCode() == KeyEvent.VK_L) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (check != null) {
            logicEditor.getWithFocus().editLogic(check.location().name());
          }
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (check != null && editCheck(check)) {
            if (getSelectedRouteCheck() != check) {
              routeList.clearSelection();
            }
            copyCheckEditorItem(check);
            refreshLogic();
          }
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          duplicateCheck(getSelectedSearchResultCheck());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_Z) {
          deleteCheck(getSelectedSearchResultCheck());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          ItemCheck check = getSelectedSearchResultCheck();
          if (check == null) {
            return;
          }

          addToRoute(check);
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
          if (routeListModel.getSize() > 0) {
            routeListModel.removeCheck(routeListModel.getSize() - 1);
            repopulateSearchResults();
          }
        } else if (UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
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
        searchResultsListModel.adjustComponentStyle(c, currentState(), searchEngine, index);
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
    routeCounters.forEach(c -> c.update(currentState()));
    transitionVisualizer.ifOpen(t -> t.updateChecksList());

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

  public void addToRoute(ItemCheck check) {
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
          refreshLogic();
        } else if (e.getKeyCode() == KeyEvent.VK_C) {
          ItemCheck check = getSelectedRouteCheck();
          if (check != null && editCheck(check)) {
            if (getSelectedSearchResultCheck() != check) {
              searchResultsList.clearSelection();
            }
            copyCheckEditorItem(check);
            refreshLogic();
          }
        } else if (e.getKeyCode() == KeyEvent.VK_D) {
          duplicateCheck(getSelectedRouteCheck());
          repopulateSearchResults();
        } else if (e.getKeyCode() == KeyEvent.VK_Z) {
          deleteCheck(getSelectedRouteCheck());
          refreshLogic();
        } else if (e.getKeyCode() == KeyEvent.VK_K) {
          routeListModel.setInsertionPoint(routeListModel.getSize());
          repopulateSearchResults();
        } else if (UP_DOWN_VALUES.containsKey(e.getKeyCode())) {
          int delta = UP_DOWN_VALUES.get(e.getKeyCode());
          int newIndex = routeList.getSelectedIndex() + delta;
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
        routeListModel.adjustComponentStyle(c, index);
        return c;
      }
    };
  }

  private void updateStartLoc(StateContext ctx) {
    startLocLabel.setText("Start: " + ctx.startLoc());
  }

  private List<RouteCounter> createRouteCounters() {
    List<RouteCounter> list = new ArrayList<>();
    list.add(new RouteCounter("Grubs", RouteCounter.termFunction(Term.grubs())));
    list.add(new RouteCounter("Essence", RouteCounter.termFunction(Term.essence())));
    list.add(new RouteCounter("Charms", RouteCounter.termFunction(Term.charms())));
    list.add(new RouteCounter("Rancid Eggs", RouteCounter.termFunction(Term.rancidEggs())));
    list.add(new RouteCounter("Dream Nails", RouteCounter.termFunction(Term.dreamNail())));
    list.add(new RouteCounter("Dreamers", RouteCounter.termFunction(Term.dreamer())));
    list.add(new RouteCounter("White Fragments", RouteCounter.termFunction(Term.whiteFragment())));
    list.add(new RouteCounter("Geo", RouteCounter.termFunction(Term.geo())));
    list.add(new RouteCounter("Relic Geo", RouteCounter::relicGeoCounter));
    list.add(new RouteCounter("Spent Geo", RouteCounter::spentGeoCounter));
    return list;
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
    GuiUtil.copyToClipboard(results);

    String msg = results + "\n\n(Copied to clipboard!)";
    JOptionPane.showMessageDialog(this, msg, "Query results", JOptionPane.INFORMATION_MESSAGE);
  }
}
