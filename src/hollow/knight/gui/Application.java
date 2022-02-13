package hollow.knight.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import hollow.knight.logic.ParseException;
import hollow.knight.logic.RoomLabels;
import hollow.knight.logic.State;

public final class Application extends JFrame {
  private static final long serialVersionUID = 1L;

  private final SearchEngine searchEngine;
  private final JList<String> resultsList;
  private final SearchResultsListModel searchResultsListModel;
  private final JScrollPane resultsPane;
  private final JList<String> routeList;
  private final RouteListModel routeListModel;
  private final JScrollPane routePane;

  public Application(State state) throws ParseException {
    this.searchResultsListModel = new SearchResultsListModel();
    this.routeListModel = new RouteListModel(state);

    setTitle("HKSpoilerViewer");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    setJMenuBar(createMenu());

    JPanel left = new JPanel();
    BoxLayout layout = new BoxLayout(left, BoxLayout.PAGE_AXIS);
    left.setLayout(layout);
    List<SearchResult.Filter> resultFilters = addFilters(left);

    this.searchEngine = new SearchEngine(state.roomLabels(), resultFilters);
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

  private static final ImmutableList<String> KS_INFO =
      ImmutableList.<String>builder().add("UP/DOWN - move through results")
          .add("W/D - move selected item up/down (bookmarks+route)")
          .add("X - remove selected item (bookmarks+route)").add("-")
          .add("SPACE - acquire selected item").add("BACKSPACE - un-acquire last selected item")
          .add("-").add("B - bookmark selected item").add("-").add("H - hide selected item")
          .add("U - un-hide selected item").build();

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

  private JMenuBar createMenu() {
    JMenuBar bar = new JMenuBar();

    JMenu file = new JMenu("File");
    JMenuItem saveToTxt = new JMenuItem("Save Route as *.txt");
    file.add(saveToTxt);
    bar.add(file);

    JMenu about = new JMenu("About");
    JMenuItem pl = new JMenuItem("Purchase Logic ($)");
    about.add(pl);
    about.add(new JSeparator());
    JMenuItem ks = new JMenuItem("Keyboard Shortcuts");
    about.add(ks);
    about.add(new JSeparator());
    JMenuItem v = new JMenuItem("Version");
    about.add(v);
    bar.add(about);

    saveToTxt.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        try {
          Application.this.routeListModel.saveAsTxt(Application.this);
        } catch (IOException ignore) {
        }
      }
    });

    pl.addActionListener(infoListener("Purchase Logic ($)", PL_INFO));
    ks.addActionListener(infoListener("Keyboard Shortcuts", KS_INFO));
    v.addActionListener(
        infoListener("Version", ImmutableList.of("HKSpoilerViewer Version " + Main.VERSION, "-",
            "https://github.com/dplochcoder/hkspoilerviewer")));

    return bar;
  }

  private List<SearchResult.Filter> addFilters(JPanel parent) throws ParseException {
    List<SearchResult.Filter> resultFilters = new ArrayList<>();

    TextFilter textFilter = new TextFilter(currentState().roomLabels());
    resultFilters.add(textFilter);
    addTextFilter(textFilter, parent);
    parent.add(new JSeparator());

    ItemCategoryFilters icf = ItemCategoryFilters.load();
    resultFilters.add(icf);
    addItemCategoryFilters(icf, parent);
    parent.add(new JSeparator());

    resultFilters.add(addRoomFilters(parent));
    parent.add(new JSeparator());

    ExclusionFilters ef = new ExclusionFilters(currentState().roomLabels());
    addExclusionFilters(ef, parent);
    resultFilters.add(ef);

    return resultFilters;
  }

  private void addModeButton(TextFilter textFilter, TextFilter.Mode mode, String txt,
      ButtonGroup group, JPanel parent, boolean selected) {
    JRadioButton button = new JRadioButton(txt, selected);
    group.add(button);
    parent.add(button);

    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        textFilter.setMode(mode);
        repopulateSearchResults();
      }
    });
  }

  private void addTextFilter(TextFilter textFilter, JPanel parent) {
    JPanel search = new JPanel();
    search.add(new JLabel("Search:"));
    JTextField field = new JTextField(16);
    field.getDocument().addDocumentListener(new DocumentListener() {
      @Override
      public void changedUpdate(DocumentEvent e) {
        textFilter.setText(field.getText());
        repopulateSearchResults();
      }

      @Override
      public void insertUpdate(DocumentEvent arg0) {
        textFilter.setText(field.getText());
        repopulateSearchResults();
      }

      @Override
      public void removeUpdate(DocumentEvent arg0) {
        textFilter.setText(field.getText());
        repopulateSearchResults();
      }
    });
    search.add(field);

    parent.add(search);

    JPanel mode = new JPanel();
    ButtonGroup group = new ButtonGroup();
    addModeButton(textFilter, TextFilter.Mode.ITEM, "Item", group, mode, false);
    addModeButton(textFilter, TextFilter.Mode.LOCATION, "Location", group, mode, false);
    addModeButton(textFilter, TextFilter.Mode.BOTH, "Both", group, mode, true);

    parent.add(mode);
  }

  private static final int COLS = 2;

  private void addItemCategoryFilters(ItemCategoryFilters icf, JPanel parent) {
    JPanel filters = new JPanel();
    int numItems = icf.allFilters().size();
    int numRows = (numItems + COLS - 1) / COLS;
    ++numRows; // Two buttons.
    filters.setLayout(new GridLayout(numRows, COLS));

    JButton all = new JButton("ALL");
    filters.add(all);

    JButton none = new JButton("NONE");
    filters.add(none);

    List<JCheckBox> jcbs = new ArrayList<>();
    for (String filter : icf.allFilters()) {
      JCheckBox jcb = new JCheckBox(filter);
      jcb.setSelected(true);
      jcb.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          icf.enableFilter(filter, jcb.isSelected());
          repopulateSearchResults();
        }
      });

      filters.add(jcb);
      jcbs.add(jcb);
    }

    all.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        icf.allFilters().stream().forEach(f -> icf.enableFilter(f, true));
        jcbs.stream().forEach(b -> b.setSelected(true));
        repopulateSearchResults();
      }
    });
    none.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        icf.allFilters().stream().forEach(f -> icf.enableFilter(f, false));
        jcbs.stream().forEach(b -> b.setSelected(false));
        repopulateSearchResults();
      }
    });

    parent.add(filters);
  }

  private SearchResult.Filter addRoomFilters(JPanel parent) {
    JButton all = new JButton("All Areas");
    parent.add(all);

    JTabbedPane pane = new JTabbedPane();

    Map<RoomLabels.Type, JList<String>> lists = new HashMap<>();
    for (RoomLabels.Type type : RoomLabels.Type.values()) {
      DefaultListModel<String> model = new DefaultListModel<>();
      currentState().roomLabels().allLabels(type).stream().sorted().forEach(model::addElement);

      JList<String> list = new JList<>(model);
      lists.put(type, list);
      list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      list.setSelectedIndices(IntStream.range(0, model.getSize()).toArray());

      JScrollPane scroll = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
          JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      pane.addTab(type == RoomLabels.Type.MAP ? "Map Areas" : "Titled Areas", scroll);
    }

    RoomFilters filters = new RoomFilters(currentState().roomLabels(), lists);
    lists.values().forEach(l -> l.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        repopulateSearchResults();
      }
    }));

    pane.addChangeListener(new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        filters.setActiveType(RoomLabels.Type.values()[pane.getSelectedIndex()]);
        repopulateSearchResults();
      }
    });

    all.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        lists.values().forEach(
            l -> l.setSelectedIndices(IntStream.range(0, l.getModel().getSize()).toArray()));
        repopulateSearchResults();
      }
    });

    parent.add(pane);
    return filters;
  }

  private void addExclusionFilters(ExclusionFilters filters, JPanel parent) {
    parent.add(new JLabel("Exclusions"));
    ActionListener exFilterChanged = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        repopulateSearchResults();
      }
    };

    filters.gui().forEach(cb -> {
      parent.add(cb);
      cb.addActionListener(exFilterChanged);
    });
  }

  private JList<String> createSearchResults() {
    JList<String> resultsList = new JList<String>(searchResultsListModel);
    resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    Arrays.stream(resultsList.getKeyListeners()).forEach(resultsList::removeKeyListener);
    resultsList.addKeyListener(resultsListKeyListener());

    return resultsList;
  }

  private State currentState() {
    return routeListModel.currentState();
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
}
