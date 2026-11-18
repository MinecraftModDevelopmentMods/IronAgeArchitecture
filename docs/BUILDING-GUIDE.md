# Building and crafting guide

Permanent building components for the Iron Age series, starting on Minecraft 1.10.2 Forge.
Architecture runs independently of Iron Age Furniture.

Version 0.1.16.110021 contains raw and limewashed cob blocks,
stairs and slabs; modular wattle-and-daub walls in all six vanilla woods; and smooth
thatch, slate and clay tile roofs with automatic corners and separate ridge caps, plus jettied-storey walls,
timber floors, support brackets, hanging clay tiles, openable leaded windows, and brick and
stone fireplaces, rounded smart cob walls and smart thatch. There are 167 visible variants in Automatic, or 577 in Hybrid,
including the wood and finish choices.

## Rounded smart cob

Put four matching ordinary cob blocks in a 2×2 square to make four smart cob.
Craft a smart block alone to recover ordinary cob. Raw and limewashed finishes can
be shaped and recovered this way; single and batch limewashing work as usual.
The existing single-cob recipe still makes plain wattle and daub.

Start with connected full blocks. A rectangular cottage gets softened corners;
a stepped outline makes a rounded bay or oval room. Keep the blocks connected along
their faces. Diagonal-only touches cannot make a continuous wall. Use thicker walls
where you want a broader curve. Cob stays inside the placed cells and reduces the
rounding where space is tight.

Fit the existing doors, glass panes or leaded windows into the openings. Matching
courses keep their outside shape as openings are added. The wall has no timber
frame; its soft earthen surface continues around the corners and into the reveals.

Smart cob is available in Automatic and Hybrid. It stays registered in Manual so
existing smart-cob buildings remain readable. `blocks.smartCob` enables the family
and requires `blocks.cob`; timber framing and windows can be disabled independently.
It belongs to both era palettes. With fresh Automatic settings, the kit has 354
recipes; selecting Iron Age alone leaves 60, including the shared workbench and full floors.

The Smart Thatch Preview puts joined, faceted roofs over the three cob cottages.
New window styles and half-depth cob pieces remain later work.

## Drawing a roof at the workbench

Lay a rectangular attic floor from full timber floor pieces and put the bench on
it. A complete ring of full-depth walls at the same height, immediately outside the floor, lets
the bench recognise an inset floor. The older floor-over-wall arrangement also
works. Use **Floor: Inset** or **Wall top** to choose explicitly. Complete the floor
perimeter or trim the rectangle; stairwells may stay inside.

Craft the bench with paper, feather and paper across the top, three planks beneath,
and a crafting table at the bottom centre. One matching plank above a stick makes
two roof timber. The bench also accepts those ingredients directly.

Changing the drawing does not construct a roof. Turn on the personal preview,
walk around it, then return to build when you are happy.

- Choose 1:2, 1:1 or 2:1 pitch. Rotate the ridge before placing features.
- **Base** raises the roof in half-block steps without moving the floor.
- **Reach** adjusts the selected eave or gable from zero to two blocks in half-block
  steps. Select each of the four sides separately.
- The **roof** page has eave drop, floor position, frame and edge controls.
  Eave drop lowers the outer edge without lowering the room inside.
- **Traditional** adds ties. **Attic room** raises collars to the chosen clearance
  above the floor; the default is 2.5 blocks. **Open vault** leaves them out.
  Raise the roof or choose a steeper pitch if the selected clearance will not fit.
- **Fascia** adds the timber eave board; **Boxed** also lines its underside.
  Existing gutter and outlet items fit the outside board face. Fit a separate
  soffit from below if you want to line an exposed eave later. Route pipes by hand.

### Take the clipboard with you

Paper above any plank makes one reusable clipboard. The bench can make it from
those same supplies with **Make clipboard**, placing it in the separate tool slot.
Take it out, or put an existing clipboard into the slot to link it to this roof.
Using it on a bench also links it. The drawing remains saved with the roof.

Use the clipboard in the air to choose a tool; sneak-use in the air always opens
its controls. Point at the roof or visible ghost and use it to place a feature.
Sneak-use on a feature selects it. The controls can change the selected feature,
remove it, or undo and redo the last 32 drawing changes. Edits are shared, while
the ghost is personal. If another player changes the drawing first, it refreshes
before your next edit. Keep the whole affected area loaded when building.

- **Window** places a flush rooflight in one uninterrupted roof plane. It must
  clear ridges and mansard bends. Match its Roof option to the covering for the
  correct reveal depth. It opens outward from its top hinge by 45 degrees.
  Point at a dormer wall to add or replace a vertical casement instead.
- **Gabled dormer** makes a small projecting gable, one to five blocks wide.
- **Shed dormer** makes a wider front with a shallow roof. Leave one block at each
  end of the main roof. Both dormers join back before its ridge; lower or narrow
  one if it cannot fit. Choose one- or two-high windows and front lift separately.
- **Mansard side** changes the roof side you point at. The roof controls also let
  you select either side or both. Its steep lower third and shallow upper part
  keep the standard roof's ridge height.
- Choose all six timbers, raw or limewashed surrounds, either leading pattern,
  cob, daub or tile-hung cheeks, and thatch, slate or clay covering. Materials
  disabled in your configuration cannot be built.
- **Remove** on a dormer window fills it back in. Remove the selected dormer in
  the controls to remove the whole projection. Drawing changes take effect only
  after construction at the bench.

Right-click a completed window, including its open pane, to operate it. Matching
vertical sections open together. Redstone transitions open and close it; a manual
change persists until the next transition. Leave its full swing clear. Helpers
reserve the cells crossed by open panes, without independent items or drops.

### Pay for the changes

The **bill** page lists additions, reused materials and returns; scroll to see the
whole bill. Supply matching roof timber, or matching planks and sticks for timber,
along with the listed ordinary wall pieces, complete leaded windows and coverings.
Features include their framing, cheeks, glazing and covering. The main roof is
initially a bare frame.

**Build / repair** changes only the affected owned sections. Removed materials are
reused before extra supplies, and leftovers return to the bench. Unchanged roofs
keep their covering, fittings and window state. Cancel if necessary; completed
sections remain and unused materials return. A saved job resumes after reload.
Keep the bench loaded while it works. It never replaces ordinary walls or floors.

Fit the main covering by clicking a sloping rafter with smart thatch, slate slopes
or clay tile slopes. Sneak-use an empty hand to recover it. Different coverings
can be swapped without replacing timber. Mining a part recovers that paid part,
including through a helper, while other parts sharing its cell remain.

### Gable infill

Cycle the bench's View button to **gables**. Choose each end separately: **Match
walls**, cob, limewashed cob, daub, limewashed daub, hanging tiles or **Open**.
Match samples the wall course beside an inset floor, or below a floor over the
wall. Framed daub uses that wall's wood; explicit choices use the roof timber.
The clipboard's roof page has the same controls. If there is no supported wall
course, Match uses limewashed cob when that material is enabled.

The top follows the actual roof profile, including mansard bends. The bill lists
ordinary wall materials and any framing timber. Rework reuses or returns them as
usual. Existing wall blocks and windows remain independent; anything obstructing
the roof structure still needs clearing. Older drawings stay open until you select
infill. You can remove an infill panel and fit an ordinary window in its place;
rebuilding keeps that window.

Roof drawings are bounded to 32 by 32 blocks and 1,024 occupied cells. Large or
steep roofs may need separate drawings. Side walls beneath raised eaves remain
ordinary building work. Use the bounds map to trim the rectangle and
mark roof openings separately from floor stairwells. Earlier workbench roofs keep
the old solver until you explicitly change them through the new design controls.

The workbench, clipboard and basic full floors belong to both palettes. Automatic
and Hybrid offer their recipes; Manual preserves existing structures and items.
The family uses blocks.architectsWorkbench independently of cob and timber walls.

## Smart thatch

In Automatic, three wheat across a row above three sticks make six smart thatch.
Build a roof in steps: raise each row by one block for a pitched roof, bring slopes
together for a ridge, or step the outline inward on all sides for hips. A single
rising course at the perimeter gives an angled eave. Two level courses make a
wider, flatter apron, so leave those for a roof that needs one.

The block chooses from a fixed set of slopes, hips, valleys and transitions using
its immediate neighbours. The covering has thick cut edges and an underside that
follows the pitch. The shapes use ordinary chunk rebuilding, without a roof-wide
smoothing queue.

Place an extra row beyond the walls for an overhang. For a gable, carry the roof
past both end walls and fill the triangle below with cob or another wall material.
The cottage in the preview shows this alongside two hipped roofs.

The covering is half a block deep. At low parts of a slope it uses the upper half
of the cell below, so leave clearance under exposed edges. Walls and supports
trim that lower part. Mining or picking either part addresses the same section;
holding smart thatch lets you extend from its cut edge. It costs and drops one item.
Stack pieces where you want a solid mass underneath. Leave holes for chimneys and
windows. Slate and clay remain separate materials.

Earlier smart roofs keep their placed material and adopt the new covering.
Earlier rounded helpers are replaced locally as chunks load. The old mesh files
are left in the save and are no longer read. Test a copy before updating a world.

| Mode | Thatch crafting |
| --- | --- |
| Automatic | Wheat and sticks make smart thatch; original pieces are hidden |
| Hybrid | Original recipes stay; craft a slope alone into smart thatch or back again |
| Manual | Original recipes stay; an existing smart item can recover one slope |

Both smart IDs stay registered in every mode. `blocks.thatch` controls the whole family,
independently of cob and timber. Thatch belongs to both era palettes.

`building.retainLegacyThatchIds` defaults to true when upgrading an existing config.
Keep it enabled for worlds with original golden roofs. Fresh Automatic configurations
default to false, saving the two old IDs; Hybrid and Manual always register them.
In Automatic with the old IDs retained, craft one old slope or two ridge caps into
one smart thatch. This conversion changes their appearance, so apply it deliberately.
Client and server need matching registration settings and a restart.

The supplied preview retains the old IDs for its unchanged golden-roof comparison.
Use its accompanying configuration when loading that save. The original golden
thatch and its textures are unchanged. For dormers, use a roof drawing at the workbench.

## Manual wattle and daub

These individual patterns are available in Hybrid and Manual modes.

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

## Smart wattle and daub

Automatic is the default. Four matching planks around full daub make four smart walls.
Build your walls with it, leaving openings for doors and windows. The framing adjusts
as the building takes shape. Floors divide the storeys, and stepped wall outlines form
gables. Posts and braces follow each facade, including the returns around corners.

All six woods and both finishes are available. Two full smart panels vertically make
four thin panels. Two thin panels side by side rejoin into a full panel. Limewashing
works singly and in batches of eight, just like the ordinary walls.

Thin panels inherit the occupied half of a neighbouring thin wall or window. Sneak
to choose another plane. Sneak-use another thin panel on the end of a thin smart wall
to turn a corner. Mining that joined corner returns both panels. Full walls keep their
full footprint as their framing changes.

In Hybrid, add a stick to a full post panel to make a smart wall. Use the individual wall pieces to guide the result. A principal post
anchors a bay, rails set a horizontal line, and a diagonal asks the smart wall to continue
that brace. Craft a smart full panel alone to recover a matching principal post.

A leaded pane and a matching smart full or thin wall also make a window. Place floors,
roofs, windows and the other building pieces in the usual way.

Set `building.mode` in `config/ironagearchitecture.cfg` and restart.

| Mode | Wall kit |
| --- | --- |
| `manual` | The individual wall patterns and their original recipes |
| `hybrid` | Both kits, with manual pieces guiding smart framing |
| `automatic` | Smart framing. Four matching planks around full daub make four smart walls |

Automatic leaves the manual wall patterns out of registration on new installations,
saving block IDs as well as keeping the inventory smaller. The rest of the building
kit remains available. Client and server need matching registration settings and a restart.

When upgrading an existing configuration, `building.retainLegacyWallIds` is enabled
to keep older manual walls and smart corners readable. Leave it enabled for your existing
villages. For a new world or modpack, Automatic with this setting `false` uses the smaller
registry. Changing the mode does not convert placed walls. A world that needs an omitted
wall ID will refuse to load and tell you to turn compatibility back on.

Smart walls reconsider nearby sections after a building edit. Saved framing is retained
while an adjoining chunk is unavailable. A section can span up to 48 blocks horizontally
and 32 vertically. Larger uninterrupted sections use the simple provisional framing;
corners and floor levels divide ordinary houses into smaller sections.

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

Two dark oak slabs stacked vertically make four soffits. Right-click the underside of
a clay fascia with a soffit to line that section of the eaves. Sneak and right-click
underneath with empty hands to take the lining back off. You can fit a soffit before
or after the gutter. The fascia still takes the same gutter or outlet either way.

## Guttering

Use a gutter or outlet on the outside face of a bare clay fascia. Level sections join
and cap their exposed ends. Sneak and use an empty hand on the metal to recover the fitting.

Start a pipe against the underside of the outlet and build down from the pipe end. The
eave offset brings the pipe towards the wall. For a half-block step, use the half-block
offset beneath an outside pipe in a clear bay beside the thin wall. For a full-block jetty,
run the pipe close to the upper wall, place the outer elbow below it and fit the inner elbow
on the wall side of that elbow. Continue down from the inner elbow. To select the position
of a straight pipe by hand, sneak, face the wall and click where you want it to sit.

Three iron ingots across a row make six gutters. Add an ingot to a gutter for an outlet.
Three iron ingots in a column make six pipes. Craft a pipe with a stick for an eave offset.
Craft an offset alone to cycle through the eave, half-block, outer and inner shapes.

Craft a straight downpipe alone to make a shoe. Fit it under the last pipe, in the
block above the paving. The short bend turns out from the wall, with enough room
under its mouth for rain to drip onto the cobbles.

An exposed outlet drips during rain. A connected pipe takes those drips to its open foot.

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

## Fireplaces and chimneys

Start with a firebox at floor level. Set matching hearth pieces beside it to widen the
hearth, with every piece facing the room. Jambs and lintels are separate, so you can build
a compact fireplace or frame a wide inglenook with your own masonry around it.

Right-click the firebox with flint and steel to light it. Sneak and right-click with
both hands empty to put it out. The fire gives off light and sometimes crackles. It
burns without fuel and stays contained in the firebox.

Build a straight run of flue blocks directly above each firebox. Offset the upstairs
firebox by one block so its flue can run alongside the lower one. Matching brick or stone
flues join visually, while each column carries smoke from its own firebox. Leave an open
flue at the top or finish it with a pot or slab cap. Pots need air above them and slab
caps need a clear side vent. Keep the run continuous and the outlet within 32 blocks of
the firebox.

On a straight clay slope, use a roof collar in place of the slope where the flue crosses
the roof. Place it facing up the slope just like a roof tile. The collar follows the same
pitch and continues the flue.

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
- Three wheat above three sticks: six smart thatch in Automatic, or six original slopes in Hybrid/Manual. Use stone for slate or bricks for clay tile.
- Two matching slopes vertically: four ridge caps. Two ridge caps side by side: one slope.
- Craft a clay tile slope alone to turn it into a full hanging panel, or craft the full panel
  alone to get the slope back. Two full panels vertically make four thin panels.
  Two thin panels side by side rejoin into one full panel.

### Fireplaces

Use brick blocks or plain stone bricks throughout each recipe.

| Piece | Recipe |
| --- | --- |
| Four hearths | Two masonry blocks on a diagonal |
| Firebox | One hearth and one iron bars block |
| Four jambs | Three masonry blocks vertically |
| Four lintels | Two masonry blocks side by side |
| Four flues | Two columns of three masonry blocks with the middle column empty |
| Pot cap | One flue and a flower pot |
| Slab cap | One flue and a stone slab |
| Roof collar | One flue and a clay roof slope |

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
The `blocks.clayTiles` switch controls the clay slopes, ridge caps, trim, hanging panels and iron guttering.
The `blocks.fireplaces` switch controls the fireplace kit in Late Medieval & Tudor.
Clay tiles also need to be enabled to use roof collars.
Thatch and timber-bearing walls burn; plain daub, cob and slate do not. There is no world generation or collapse simulation.

See [development instructions](DEVELOPMENT.md) for builds, Eclipse and disposable runtime checks.
This is an early alpha. Automated checks and manual visual/gameplay review are recorded separately.
The Rye Eaves Preview shows lined and exposed eaves on neighbouring cottages.
