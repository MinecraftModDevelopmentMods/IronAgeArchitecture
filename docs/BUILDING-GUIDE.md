# Building and crafting guide

Permanent building components for the Iron Age series, starting on Minecraft 1.10.2 Forge.
Architecture runs independently of Iron Age Furniture.

Version 0.1.10.110021 contains raw and limewashed cob blocks,
stairs and slabs; modular wattle-and-daub walls in all six vanilla woods; and smooth
thatch, slate and clay tile roofs with automatic corners and separate ridge caps, plus jettied-storey walls,
timber floors, support brackets, hanging clay tiles and openable leaded windows. There are 517 visible
variants including the wood and finish choices. Architecture remains standalone.

## Wattle and daub

Build filled daub walls with principal posts, corner/end posts, sill/head rails, narrow
studs or close studding, and deliberate left/right diagonals. Plain infill has no border.
Place posts at your chosen bay spacing and rails at floor/head heights. Matching timber
joins automatically at intersections and around corners. Studs extend into rails and
diagonal runs terminate at matching posts, corners or rails. Wood grain follows each member.

A rail from a window or sill/head panel joins through intervening posts to a matching
corner or other timber. Unsupported ends finish on their post, stud or brace. Corner
pieces draw rails on their connected sides. Use sill/head panels for a deliberate
continuous rail. Meeting braces also carry their width across the
adjoining panel corners, using the same wood. Plain infill does not add complete rails.

Check the direction of a diagonal when changing the occupied half of a wall. Facing a
panel the other way reverses its slope in the building. Craft a brace alone to swap its
rising/falling pattern when needed, then continue the line through the next section.

All modules are ordinary wall blocks with raw or limewashed infill. The connections are
derived from neighbouring blocks, so there are no wall tile entities or ticking components.
The original timber_frame block IDs and four item subtypes remain readable.

## Jettied storeys

Thin walls are a full block high and half a block deep. Short walls are also half a block
long; clicking their left/right half selects the matching cut. Use the quarter, half or
L-shaped corner post to close the returns. Corners have outside/inside patterns and both
daub finishes. Existing full-depth walls remain available.

Place timber jetty floors first, then the upper walls. Floors occupy the upper half of
their cell, leaving a level walking surface at the next whole-block height. Full, half,
quarter and L-shaped floors provide the matching footprints. Joists follow the direction
you face; matching neighbouring floors join their boards and perimeter beams automatically.
Click the empty corner when placing an L-shaped floor.

Where a floor sits directly above a wall, a matching timber wall-head beam closes the
space up to the floorboards. It follows the lower wall's footprint, including half-depth
walls and corners, so the cantilevered part remains open underneath. It updates when the
lower wall changes; no extra piece needs placing.

A half-block facade step uses the other occupied half of a cell, or the next half in the
neighbouring cell. Repeat this on successive floors for 0.5 + 0.5, 0.5 + 1, or larger
projections. The added floor area is usable inside. Continue a wall run to inherit its
plane; sneak while placing to choose a different one. Partial shapes reserve a whole cell
for placement, so unrelated blocks cannot share their empty halves.

Use a matching half/full-reach bracket underneath an exposed jetty floor edge to attach it.
Each floor accepts one bracket; the bracket occupies the lower half of that floor's cell.
Invalid or occupied placements consume nothing. Breaking the combined piece returns both
the floor and bracket. Brackets do not introduce collapse or support physics.

Roof slopes face the direction the player looks when placing them. Their high edge
faces that direction. Adjacent matching slopes form hips and valleys using stair-like rules.
Ridges run along the direction the player looks; place them one row above the high ends
of the last slopes. Their half-block apex completes the 45-degree roof profile.

For a timber fascia along a straight clay eave, craft a slope with a stick and
place it facing uphill. At a gable, craft a slope with a dark oak plank to make
a verge. Face uphill and click the half that will overhang the wall. Its thin
underside covers a half-block projection. Craft a clay ridge cap with a dark oak
plank for the matching ridge end and place it while looking inward along the ridge.

Use full hanging tiles to fill the stepped gable beneath a clay roof. Thin hanging
tiles share the half-block plane with straight thin timber panels and thin leaded windows.
Place against one of these pieces to inherit its occupied half. Sneak to choose the half manually.

## Leaded windows

Use full-depth surrounds in full walls and half-depth surrounds in thin walls. Both come
in all six woods, with raw or limewashed daub and diamond or rectangular leading.
Each section is one block wide and high. Matching sections stacked above one another
form one tall casement, with a continuous opening. Side-by-side columns share an upright
mullion but operate separately.

Click the left or right half when placing the first section to choose its hinge. The
window faces towards you and opens outwards. Thin windows follow the occupied half of
the adjoining thin wall; stacked windows inherit their column's facing and hinge.
Sneak while placing to choose the alignment yourself. Holding a window item lets you
add more sections without opening the one you clicked.

Right-click either the frame or its open pane to open or close the whole matching column.
Keep the outside cell of every section empty and leave room for the swing, including
entities. If anything blocks it, the column stays as it was. Close separate columns
before filling a gap between them. A clear two-block-tall open casement is wide enough
for a normal player to pass through; step onto the sill first.

Powering any section operates its column. A manual change stays in effect until the
power next changes, just like a wooden door. A powered window blocked from opening
tries again when the obstruction is removed. Windows open instantly through 90 degrees.
Breaking a section, including through its open pane, returns its intact window item.

## Crafting

Start with a few base materials, then shape them into the pieces you need. Match the wood,
finish and pattern within each recipe. Shaping and recombining keep the amount of material;
changing a timber pattern does not need extra sticks.

### Cob and plain wattle and daub

- Four dirt, four clay balls and one wheat, shapeless: eight cob.
- One cob alone: one plain wattle-and-daub block. Craft the infill alone to turn it back into cob.
- Three cob in a row: six slabs. Two slabs side by side: one cob.
- Six cob in the usual stair pattern: eight stairs. Four stairs in a square: three cob.

### Timber wall patterns

Put plain wattle and daub in the centre, with one matching plank above, below, left and
right, to make four post panels. Choose the infill's finish before crafting, or limewash later.

Shape three matching post panels into the family you want:

| Arrangement | Result |
| --- | --- |
| Three in a horizontal row | Three sill rails |
| Three in a vertical column | Three narrow studs |
| Three along a diagonal across the crafting table | Three rising braces |
| Three in an L | Three corner/end wall panels |

These patterns work for full walls, thin walls and short pieces. Start with the single-post
pattern; a cross-braced post panel can be swapped back by crafting it alone.

Craft one paired-pattern piece alone to swap its design: post/cross brace, sill/head rail,
single/close studs, rising/falling brace, or corner/end post. Shaped corner pieces swap their
outside/inside pattern in the same way. A row of three matching rails, studs, braces or
corner/end wall panels returns three post panels of the same size, wood and finish.

### Cutting walls and making returns

| Input | Arrangement | Result |
| --- | --- | --- |
| Two matching full walls | Vertical | Four thin walls |
| Two matching thin walls | Vertical | Four short walls |
| Two matching short walls | Horizontal | One thin wall |
| Two matching thin walls | Horizontal | One full wall |

Cut corner/end panels become straight end posts. Rejoining them returns a full end post;
craft it alone if you want the full corner pattern instead.

For shaped returns, use short end posts: one makes a quarter corner, two vertically make
a half corner, and three in an L make an L-shaped corner. Two matching shaped corner pieces
side by side recover their short end posts: two from quarter corners, four from half corners,
or six from L-shaped corners. Either inside/outside pattern can be recovered this way.

### Leaded glazing

- Surround one iron ingot with eight glass panes to make eight diamond-leaded panes.
- Craft one leaded pane alone to swap between diamond and rectangular leading.
- Combine one pane with one matching principal-post panel to make one window. The
  panel supplies its wood, daub finish and full or half depth. Use the single-post pattern.
- Shape panels **before glazing**. Windows cannot be cut into thin or short walls.
- Craft a window alone to swap its leading while keeping the same surround and depth.
- Limewash a raw window with bone meal, singly or in a batch of eight identical windows.

### Floors, brackets and roofs

- Three matching planks above three sticks: six timber floors.
- Cut floors with a vertical pair: two full floors make four halves; two halves make four
  quarters. Join cuts with a horizontal pair: two quarters make one half; two halves make one full floor.
- Three quarter floors in an L: one L-shaped floor. Craft an L-shaped floor alone to recover three quarters.
- One full timber floor plus one stick, shapeless: two full-reach brackets in that wood.
  One full-reach bracket alone makes two half-reach brackets; two halves side by side make one full bracket.
- Three wheat above three sticks: six thatch roof slopes. Use stone for slate or bricks for clay tile.
- Two matching slopes vertically: four ridge caps. Two ridge caps side by side: one slope.
- Craft a clay tile slope alone to turn it into a full hanging panel, or craft the full panel
  alone to get the slope back. Two full panels vertically make four thin panels.
  Two thin panels side by side rejoin into one full panel.

### Limewashing

Every raw cob, daub or timber-and-daub piece can be limewashed after shaping. Use one piece
with one bone meal for a single conversion, or surround one bone meal with eight identical
pieces to wash the whole batch. Wood, pattern and shape stay the same. Slabs, stairs and
shaped corners follow this rule too.

## Era palettes

The creative tabs follow building eras. **Iron Age** currently holds the shared cob,
plain daub and thatch pieces. **Late Medieval & Tudor** holds the full timber-framed
village kit, including leaded windows, jetties, brackets, slate and clay tiles, along with those shared materials.
The Iron Age palette is a starting selection; a dedicated roundhouse kit is still to come.

Choose which eras can be crafted in survival in `config/ironagearchitecture.cfg`:

```text
recipes {
    S:survivalEras <
        late_medieval_tudor
    >
}
```

Use `iron_age` for the earlier palette, or put both IDs on separate lines to enable
both. The default entry `all` includes every era. An empty list disables all Architecture
recipes. Unknown names are logged and ignored, so a misspelling cannot enable other eras.
Restart the game or server after changing this setting.

A shared recipe stays available if any enabled era uses it. The setting filters this
mod's crafting recipes; creative tabs and existing placed blocks stay available. Server
owners should set the same recipe selection on their clients. Existing content-family
switches still apply separately.

All content families are enabled by default. Framed panels require the cob family.
The `blocks.leadedWindows` switch controls both glazing and windows and requires timber
frames and cob. Era recipe filtering is separate from this content switch.
The `blocks.clayTiles` switch controls the clay slopes, ridge caps, trim and both hanging panels.
Thatch and timber-bearing walls burn; plain daub, cob and slate do not. There is no world generation or collapse simulation.

See [development instructions](DEVELOPMENT.md) for builds, Eclipse and disposable runtime checks.
This is an early alpha. Automated checks and manual visual/gameplay review are recorded separately.
The Rye Roof Trim Preview shows how the fascia and gable trim fit on three houses stepping down a cobbled street.
