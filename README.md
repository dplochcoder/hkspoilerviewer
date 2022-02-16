HKSpoilerViewer is a desktop application for searching and routing checks in [Randomizer 4](https://github.com/homothetyhk/RandomizerMod).

# Installation

Download the latest JAR file from [Releases](https://github.com/dplochcoder/hkspoilerviewer/releases), and store it anywhere on your device. You must have [Java](https://www.java.com/en/) installed to run it.

The first time you run it, it will ask you to locate the `RawSpoiler.json` file on your computer with a file-open dialog. After this, it will always open that file by default; if the program is silently crashing and failing to open the json file, you may have selected a bad input. Delete the automatically created `HKSpoilerViewer.cfg` file and try again if this happens.

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

Several types of checks are excluded from search results by default; you can un-exclude them by disabling them in the Exclusions section.

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

# Search Results

Search results appear in the center pane and are updated instantly as filters are changed. Results can be bookmarked, hidden, or added to a route - see "About > Keyboard Shortcuts" for controls.

Search results are always ordered first by Map area (e.g. Greenpath, Dirtmouth, etc.) alphabetically, and second by item name. Ties are determinate but unspecified.

## Bookmarks

Bookmarks are results which the player finds interesting and wants to remember, but has not explicitly routed yet. They are always sorted at the top of search results, even if they don't match the current filters, and can be custom re-ordered. These are useful for planning a route when considering multiple checks towards a destination, but it is not yet certain which should be routed.

## Hidden Results

The player can mark checks as 'uninteresting' and force them to the bottom of search results with the hide feature. Hidden results will not appear when they do not match the search filters, and when they do they will always be forced to the bottom of the list.

## Routed Results

Search results can be added directly to the end of the current route. Doing so acquires the item and applies its effects to Logic, which can dramatically expand search results when the Out of Logic filter is active. Routed checks can also be deleted or re-ordered from the route panel on the right.

# Saving and Loading

Your current route can be saved in two formats:

  - A `*.txt` file, for easy sharing/pasting, or
  - A `*.hks` file, which can be re-opened by HKSpoilerViewer later
  
`*.hks` files will save your current route, bookmarks, and hidden results, allowing you to share a route with other users of the HKSpoilerViewer, or simply checkpoint it for yourself in case you want to modify or continue the route later. `*.hks` files are versioned and should be compatible with newer versions of HKSpoilerViewer, but this is not an absolute guarantee.  (Most likely, a 2.0 release will break compatibility with 1.0 save files.) 
