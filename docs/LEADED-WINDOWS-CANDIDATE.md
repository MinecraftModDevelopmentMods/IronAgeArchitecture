# Leaded windows — 0.1.6.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

Leaded windows now belong to the Late Medieval & Tudor palette. There are 48 window
choices: diamond or rectangular leading, all six woods, raw or limewashed surrounds,
and full or half depth. Two leaded-pane components bring the visible total to 510.

Matching vertical sections form one outward-opening casement. Side-by-side columns
open separately. Right-click the frame or its open pane, or power any section with
redstone. Manual changes last until the next power change. The whole column checks
outside cells and the swing before moving, so an obstruction cannot leave it half open.
Entity-blocked powered openings retry while waiting; successful manual overrides also
acknowledge the current input, so a pending retry cannot undo them.

The first section chooses its hinge from the clicked half. Stacked sections inherit
their column's alignment unless sneaking. Thin windows keep the occupied wall half
independent of the outward direction, including when placed from inside the building.
Holding a window in either hand leaves placement available instead of toggling it.
Close adjoining columns before filling a gap between open and closed sections.

Craft eight glass panes around one iron ingot to make eight diamond-leaded panes.
One pane and a matching principal-post panel make one window, retaining wood, finish
and full/thin depth. Shape the panel before glazing. Windows cannot use the wall-cutting
or rejoining recipes. Panes and windows can swap leading one-for-one, and raw windows
accept single and eight-piece limewashing. The building guide and item hints cover this.

Windows have their own light-transmitting block family. A hidden outside-cell helper
carries the part of an open pane beyond its owner cell, including collision, targeting
and rendering across chunk borders. It has no item or independent drops. Four metadata
bits hold facing, open and cached power; hidden identities hold right hinges and rear-half
thin placements. Existing saved IDs stay unchanged. There are no tile entities, scheduled
block ticks or Furniture dependency. Idle windows and ownership links are never polled.

## Verification

- 41 unit tests pass, including shared window geometry, cell clipping, surface area,
  player clearance and rear-half placement. All 967 generated resources are current.
- Build/Javadoc, Java 8 and package audits, Eclipse output synchronization and the
  ordinary Eclipse classpath checks pass. Probe classes stay outside player artifacts.
- The packaged server checks 2,304 window metadata states, 576 directional columns
  and 144 saved columns on reload. Coverage includes both hinges/depths/finishes/styles,
  placement and alignment, both thin half-cell positions, independent adjacent columns,
  rejected open/closed bridges, obstruction rollback, entities, hand/power transitions,
  manual overrides of pending power edges, owner/helper targeting, player passage,
  intact survival/creative drops, explosions, forced replacement and link repair.
- All 2,010 default recipes return their expected output. The crafting matrix covers
  11,444 shifted, mirrored or reordered grids. All 1,514 Architecture-only conversions
  preserve separate material quantities, including frame and glazing. All 510 visible
  variants are reachable. Extra negative checks reject glazed cutting/rejoining,
  cross-braced glazing inputs and mixed wood, finish, depth or leading in wash batches.
- Thirteen packaged server modes pass: core, reload, Furniture coexistence, all families
  disabled, windows disabled, three earlier preview imports and five era selections.
  Iron Age alone retains 36 recipes. Disabling windows restores 460 variants and 1,863
  recipes. Empty or invalid-only era selections have no Architecture recipes. The
  original 4,448-state compatibility matrix remains readable.
- The native client checks models/materials, both actual creative tabs and every visible
  item's translated hints. Eighteen captures include closed/open glazing, the interior,
  a glazed house beside the unchanged comparison, six woods and a chunk-border casement.
- The new preview is saved and reloaded with only the player mod. A second plain load
  needs no missing-probe confirmation. The five earlier ordinary saves remain separate;
  all 140 of their existing files retain their original hashes.

Runtime SHA-256:
`339fbad0d414a5253b0e202976adbb914187b56b39899ef55e8726a3a0371c2a`

Evidence: `D:\IronAgeArchitecture\manual-testing\windows-0.1.6-qualified-03`.
Earlier attempts remain in the separate early, final and qualified roots. They include
the initial incomplete border-arena failure and a preliminary run that could not bind
the shared local server port. Those attempts are not the final qualification evidence.
The first clean-preview log check also matched Forge's normal connection diagnostic.
Its failed validation record is retained; the corrected check uses the saved-world
missing-probe warning explicitly. Both actual plain server starts succeeded.

Candidate: `D:\IronAgeArchitecture\candidates\0.1.6.110021-leaded-windows`.
The bundle contains player/sources/Javadoc artifacts, checksums, the building guide,
changelog, source snapshot, preview ZIP, screenshots and hash-bound receipts/logs.
The installed save is **Leaded Windows Preview** in the ordinary project's `run/saves`.
Restart the Eclipse client to load the new code before reviewing it.

## Gameplay review

The screenshots have been inspected; automated checks do not replace the player's
review. Try opening from the street and inside, choosing each hinge, stacking sections,
walking through a clear two-block casement and powering a column. Check the window
surrounds alongside thin walls, jetties and the unchanged comparison house. Opening is
instantaneous and reserves an empty outside cell per section. Shutters, bay windows,
half-width sections and clay tiles remain later additions.

Earlier candidates were not repaired or overwritten. The previous audit records three
exceptions in 0.1.3: `evidence/probe-build.log` and `source-worktree.zip` are missing, and
Windows reports `evidence/wall-head-red.xml` as unreadable/corrupted. The retained audit
is included for context; it is separate from this candidate's own delivery checks.

This increment remains local and uncommitted on `feature/1.10-era-palettes-and-joins`.
No push, merge or publication has been performed. Hosted CI has not run.
