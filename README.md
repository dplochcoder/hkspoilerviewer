HKSpoilerViewer is a desktop application for searching and routing checks in [Randomizer 4](https://github.com/homothetyhk/RandomizerMod).

# Installation

Use [Scarab](https://github.com/fifty-six/Scarab) to install SpoilerViewerMod. This will add a "Spoiler Viewer" section to your mod options, from which you can open the most recent `RawSpoiler.json` (from Randomizer) or `ctx.json` (from ICDL, if installed). You can also run the packaged `HKSpoilerViewer.jar` file as a standalone application without launching Hollow Knight. You must have [Java](https://www.java.com/en/) installed to run it either way.

If you run the JAR as a standalone app, the first it will ask you to locate the `RawSpoiler.json` file on your computer with a file-open dialog. After this, it will always open that file by default. If you want to select a different file, find the `HKSpoilerViewer.cfg` file in your AppData/Local/dplochcoder/HKSpoilerViewer directory (or OS-dependent equivalent) and delete it before re-opening the program. If you run the JAR from the Hollow Knight mod menu, it will find the relevant file automatically.

# Search Features

Search using arbitrary text, preset categories, area restrictions and/or logic restrictions to find interesting items and add them to a route one by one. All filters are conjunctive, allowing you to search for specific things like 'Spells in Greenpath'.

## Text Search

Search items by their name, location, or both. Scene names like `Ruins_05` are searchable as well (by location), though not displayed in the UI.

All matches are case-insensitive and match on *all* terms, so a search for "Des dark" will match items named 'Descending_Dark' but not 'Desolate_Dive'.

## Preset Filters

Several filter presets are provided, such as 'Movement', 'Spells', 'Charms', etc. All are enabled by default; to limit your search to a specific preset, click the 'NONE' button to disable all presets, then select only the one(s) you want.

## Area Filters

Constrain your search to specific Map or Titled areas in the likewise named tabs. All areas are enabled by default; click a specific item to constrain the search to that area. Use Ctrl+Click to select multiple areas at once.

## Exclusions

Several types of checks are excluded from search results by default; you can un-exclude them by checking them in the Exclusions section near the bottom. Likewise, you can exclude the standard categories, like 'In-Logic', in order to exclusively search Out-of-logic checks.

### Vanilla (#)

All item checks which were not randomized fall into the 'Vanilla' category. This includes geo rocks, so it tends to bloat search results when disabled. It also includes things like Grubs and Boss Essence which can be required for purchases at Grubfather and Seer, so you may need to disable this filter temporarily for such routes.

For clarity, all Vanilla search results are prefixed with a pound sign (#).

### Out of Logic (*)

Logic is evaluated according to the skips enabled for your seed, and by default, checks which are out of logic are hidden from search results. Disable this filter if you want to go out of logic for your route.

For clarity, all out-of-logic search results are prefixed with an asterisk (*).

### Purchase Logic ($)

There are four shops which constrain purchases based on collectibles the player has gathered:

  - Grubfather (Grubs)
  - Seer (Essence)
  - Salubra (Charms)
  - Jiji (Rancid Eggs)

If the player is in a situation where they have direct access (in logic) to `X` collectibles, all purchases at shops which require `N` collectibles, where `N + TOLERANCE <= X`, will show as in 'Purchase Logic' with a dollar-sign ($) prefix. This indicates that the player may be expected by logic to purchase these items, even though those collectibles haven't been added to the route yet.

Adding `N` or more such collectibles to your route will put these shop checks fully in logic, removing the '$' prefix.

## Skips

By default, logic assumes you only want to do the skips that were configured when creating the seed. In the Skips section at the bottom, you can turn on individual skips, putting their results "in logic". This allows you to constrain the out-of-logic search results to more manageable sizes.

# Search Results

Search results appear in the center pane and are updated instantly as filters are changed. Results can be bookmarked, hidden, or added to a route - see "About > Keyboard Shortcuts" for controls.

Search results are always ordered first by Map area (e.g. Greenpath, Dirtmouth, etc.) alphabetically, and second by item name. Ties are determinate but unspecified.

## Bookmarks

Bookmarks are results which the player finds interesting and wants to remember, but has not explicitly routed yet. They are always sorted at the top of search results, even if they don't match the current filters, and can be custom re-ordered. These are useful for planning a route when considering multiple checks towards a destination, but it is not yet certain which should be routed.

## Hidden Results

The player can mark checks as 'uninteresting' and force them to the bottom of search results with the hide feature. Hidden results will not appear when they do not match the search filters, and when they do they will always be forced to the bottom of the list.

## Routed Results

Search results can be added directly to the end of the current route. Doing so acquires the item and applies its effects to Logic, which can dramatically expand search results when the Out of Logic filter is active. Routed checks can also be deleted or re-ordered from the route panel on the right.

# Insertion and Rewind

Search results normally reflect the state of Logic at the end of the current route, but this is not so useful when you want to route in additional checks at an earlier point in the route. Use the `I` key to set an insertion point
in the current route, with an appropriate item selected.

This will 'rewind' Logic to prior to the insertion point, allowing to search for things in Logic *without* movement
that is routed in later on. Acquiring new checks with an insertion point set will insert the checks into the route at that point, instead of at the end.

Use the `K` key to unset the insertion point, and go back to appending to the end of the list.

# Saving and Loading

Your current route can be saved in two formats:

  - A `*.txt` file, for easy sharing/pasting, or
  - A `*.hks` file, which can be re-opened by HKSpoilerViewer later
  
`*.hks` files will save your current route, bookmarks, and hidden results, allowing you to share a route with other users of the HKSpoilerViewer, or simply checkpoint it for yourself in case you want to modify or continue the route later. `*.hks` files are versioned and should be compatible with newer versions of HKSpoilerViewer, but this is not an absolute guarantee.  (Most likely, a 2.0 release will break compatibility with 1.0 save files.) 

# Transition Visualizer

See `View > Transition Visualizer` for a richly featured interactive canvas, for viewing the geometry of your seed. The transition visualizer serves several distinct purposes:

  - Visualize the layout of a transition-rando seed, by moving scenes around manually
  - Search items by room via visual search, instead of text
  - Construct transition-rando plandos using ICDL features
  
Once the visualizer is open, see `About` for a detailed description of specific features and keyboard shortcuts. The canvas enables multi-select, similar ICDL edit shortcuts as described below, and other features designed to enable fluent search and editing.

Cosmetic options exist in the `View` menu, including a built-in layout of vanilla Hallownest which can be used as a starting point for creating your own visualization. Saving an *.hks file will preserve the layout you have in Transition Visualizer for ease of use. The specific visual placement of scenes has no material effect on the functionality of the seed; they are merely design aides.

# ICDL Edit Mode

You can also use the HKSpoilerViewer to create Plandos, using the [ICDL](https://github.com/homothetyhk/ItemChangerDataLoader) format. To create a plando, or simply make small modifications to an existing seed, first:

  1. Make sure the ICDL mod is installed via Scarab. 
  1. Create a new randomizer run inside Hollow Knight, and launch into the game. This will automatically create an ICDL save of the seed in your AppData/Team Cherry/Hollow Knight/ICDL/Temp folder. Make sure you force your desired start location: HKSpoilerViewer does not currently support changing the start location.
  1. In HKSpoilerViewer, go to `File > Open`, navigate to the ICDL save and open the `ctx.json` file.
  1. Click `ICDL > Open Editor` to open the check editor, a separate editing window. 

Now, you can edit the seed to your heart's content! The following sections describe precisely how to do this.

## Clearing the seed

You can bulk clear checks, filling them with the 'Nothing?' item if you want to start a Plando from a clean slate. Open the `ICDL > Reset all` menu and select the set of checks you'd like to clear. Shops which are cleared will all be be reduced down to a single item for sale, costing the minimum currency required by the shop.

Be careful: HKSpoilerViewer does not (yet) have Undo+Redo functionality, so be sure to save your intermediate work frequently when working on a large Plando.

## Placing Items

The ICDL check editor has two separate sections: an Item selector up top, and a Costs editor down below.
To place a specific Item at a specific location, first select the location/check to edit in Search Results or the Route, and tap 'E'. Then, select the Item you want to place in the check editor, and tap C.

As a shortcut, you can also:

  - Tap 'C' directly in Search or Route results. This will select the check for edit and also change its item in one step
  - Tap 'E' on items in the check editor to edit them without knowing their location. This is only allowed if the item is placed in exactly one unique location + costs.
  
The item search results shows the total number of items placed within the seed in parenthesis on the left, so you can target specific total counts easily. To see how your counts differ from the initial randomization settings, select `ICDL > Item Diff Report`.

Note that editing a check may cause it to immediately disappear from search results, because it no longer matches the active search filters. However, it will remain selected in the check editor.

## Adding Checks

HKSpoilerViewer does not support adding brand new checks to the world, a la [Transcendence](https://github.com/dpinela/Transcendence), but it does support creating new checks at existing locations. This is mostly intended for shops to create new sell-slots, but you could also use this functionality to create area-blitz-like multi-items throughout the world.

To create a new check at an existing location, select that check in Search Results and press 'D'. To remove a check, press 'Z'. Note that removing the last check at a specific location will instead replace that check with a 'Nothing?' check.

Because of the way items are added to the game, if you add multiple items at a single location that is not an established shop, they must all have the same cost (which is paid once by the player to acquire all the items). The editor will not prevent you from breaking this rule while editing, but you will not be able to export ICDL pack folders while this rule is broken. In a future version of HKSpoilerViewer, this may be improved to natively support multi-item checks.

## Custom Items

The CheckEditor supports creating custom Geo and Essence items, which give the player an arbitrary, specified amount of geo or essence respectively. To add custom geo or essence to your save, simply click the 'Add Custom Geo' or other button in the check editor, then assign that newly created item (e.g. `123_Geo`) to the location you want it placed it. This is the canonical mechanism for editing starting geo.

## Setting Shop Costs

To set the price of an item at a shop, select the check in Search Results and tap 'E' to edit it in the Costs section of the ICDL Check Editor. This will update the UI with fields for existing costs, allowing you to modify them, delete them, or create new ones. After making those modifications, click 'Apply Costs' to apply the changes, or 'Reset Costs' to undo the edits. Editing a new check without clicking 'Apply Costs' will lose those unsaved changes.

You can also tap 'E' if you have the item selected in the ICDL Check Editor, however this only works if the item is placed exactly once (and thus the check/location is unambiguous). If it is placed 0 times, or more than once, nothing will happen.

HKSpoilerViewer does not restrict what costs you set and where, but these may not work correctly outside of their standard locations. (In particular, there will be no UI to clue in the blind player as to what's required, outside of standard shop UIs.)

## Editing Starting Items

You can view starting items by enabling the 'Routed' filter in search, as well as the 'Vanilla' filter for built-ins like nail swings and Focus. While you cannot edit or delete vanilla items, you can add additional ones, such as custom geo, essence, even charms and movement. Simply duplicate (D) a Start check, then assign whatever item you want to the duplicate.

## Editing Context

You can edit charm notch costs, and logical tolerances from the ICDL menu. Each of these opens a text editor, which you modify and then close. The only restrictions are that geo and notch costs be non-negative integers.

## Importing

The ICDL editor does not allow you to modify vanilla checks, and there is no easy way to fix this once the save is already created. As a work-around, you can create a new randomizer save with the correct randomizer settings, then import your old save (.hks) on top of the new one. This will replace all the item checks in the new save that share randomized locations with the old save, with the checks in the old save.

For example, if you start a plando, then realize after making 100 edits that you forgot to randomize grimmkin flames, you can create a new seed that does randomize grimmkin flames, then import your old (incomplete) save onto the new one so you don't lose work.

## Saving and Exporting

At any point, you can save your progress as an hks file. The hks file is self-contained and you do not need to keep the original spoiler or ICDL files around to use it. It also preserves all of your bookmarks and routing info that you were using.

When you're ready to try out your save in Hollow Knight, select 'Export as ICDL Pack Folder' from the ICDL menu. If there are no errors, you'll be asked for a name and description of the pack folder, which will appear in the UI when the user browses 'Past Randos'. You'll ideally want to save your pack folders in the Past Randos directory, but you can save them anywhere.

# Contributions

Contributions are welcome! I'll accept any bug fixes or straight-forward feature enhancements without resistance; if you want to make bigger changes, please discuss them with me first. Because this mod is written in both Java and C#, you will need to do a little extra setup to test local changes.

## Java

First, you'll want to use a Java IDE for working with the application itself. I use [Eclipse](https://www.eclipse.org/downloads/), but I can recommend [IntelliJ](https://www.jetbrains.com/idea/) as well. Dependencies are managed via [Apache Maven](https://maven.apache.org/) in the `pom.xml`, so you'll need to install an appropriate integration for it in your IDE. The code base makes heavy use of [AutoValue](https://github.com/google/auto/blob/master/value/userguide/index.md) so you'll need to take some extra steps to ensure that Annotation Processing is enabled, or you won't be able to build.

Some pointers if you're learning the code base:

  - hollow/knight/main/Main.java is the application entry point. hollow/knight/gui/Application.java contains most of the GUI
  - hollow/knight/logic/State.java is similar to the rando ProgressionManager class, and is where most of 'Logic' happens
  - hollow/knight/logic/StateContext.java contains the ~immutable parts of progression, like logic expressions and charm notch costs
  - File saving and loading is managed through custom JSON representations, implemented with [https://github.com/google/gson](Gson). If you're creating new editable state, be mindful of serialization. Users can easily put in tens of hours into a designing a single plando, so save state needs to be robust against minor updates over time.
  
## C#

The C# aspect of the mod can be managed through Visual Studio, like most other HK Mods. Use the provided .csproj file in the SpoilerViewer/Mod folder, and manage your local resource paths in a `LocalBuildProperties.props` file.

If making a release, make sure you specify `<JarFile>` in the props file, and that it's built with dependencies included from the Java tree. I use Eclipse's 'Export Project' feature to make the JAR, and Visual Studio's default build to make the zip after.

# Future features

  - Undo/Redo functionality, particularly for ICDL work
  - Proper integration with Transcendence (editing notches for Transcendence probably doesn't work right now)
  - Native support of multi-item check locations (i.e. area blitz)