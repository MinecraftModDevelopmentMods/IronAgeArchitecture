# Building and crafting guide

Permanent building components for the Iron Age series, starting on Minecraft 1.10.2 Forge.
Architecture runs independently of Iron Age Furniture.

The current local candidate, 0.1.2.110021, contains raw and limewashed cob blocks,
stairs and slabs; modular wattle-and-daub walls in all six vanilla woods; and smooth
thatch/slate roofs with automatic corners and separate ridge caps, plus jettied-storey walls,
timber floors and support brackets. There are 460 visible
variants including the wood and finish choices. Architecture remains standalone.

## Wattle and daub

Build filled daub walls with principal posts, corner/end posts, sill/head rails, narrow
studs or close studding, and deliberate left/right diagonals. Plain infill has no border.
Place posts at your chosen bay spacing and rails at floor/head heights. Matching timber
joins automatically at intersections and around corners. Studs extend into rails and
diagonal runs terminate at matching posts, corners or rails. Wood grain follows each member.

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

## Crafting

- Four dirt, four clay balls and one wheat, shapeless: eight cob.
- Cob plus bone meal: one limewashed cob.
- Three cob in a row: six slabs. The stair pattern: four stairs.
- One cob, shapeless: one wattle-and-daub infill block of the matching finish.
- Four matching planks around cob: four post-and-daub blocks of that wood and finish.
- A post block plus two sticks: a cross-braced block; add one stick for a single rising brace.
- Two post blocks in a row: two sill rails. Craft a sill/head rail alone to swap its pattern.
- Craft a post block alone for a narrow stud; add a stick to that stud for close studding.
- Three post blocks in a column: three corner posts. Craft corner/end posts alone to swap.
- Craft a rising/falling brace alone to swap its direction. Raw daub/modules accept bone meal.
- Two matching full walls vertically: four thin walls. Two thin walls vertically: four short walls.
- Two short walls horizontally: one thin wall. Craft paired thin/short patterns alone to swap.
- One short end post: quarter corner; two vertically: half corner; three in an L: L corner.
- Craft corner posts alone to switch their outside/inside pattern. Raw partial walls accept bone meal.
- Three matching planks above three sticks: six timber jetty floors. Two full floors vertically:
  four halves; two halves vertically: four quarters. Two cuts horizontally recombine into one larger piece.
- Three quarter floors in an L: one L-shaped floor. Craft an L-shaped floor alone to recover three quarters.
- Two matching planks with a stick beside the lower plank: two full-reach brackets. Craft one alone
  for two half-reach brackets; craft two halves horizontally to recombine.
- Three wheat above three sticks: six thatch roofs. Replace wheat with stone for slate roofs.
- Two matching roof slopes in a row: three ridge caps.

All content families are enabled by default. Framed panels require the cob family.
Thatch and timber-bearing walls burn; plain daub, cob and slate do not. There is no world generation or collapse simulation.

See [development instructions](DEVELOPMENT.md) for builds, Eclipse and disposable runtime checks.
This is an early alpha. Automated checks and manual visual/gameplay review are recorded separately.
The current test world is Jettied Street Preview; both earlier preview saves are retained separately.
See [the jetty candidate handoff](JETTIED-STOREYS-CANDIDATE.md) for the preview and verification record.
