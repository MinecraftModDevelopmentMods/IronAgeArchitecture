# Jettied storeys and half-block wall placement

Design direction, 2026-11-02, following the user's review of the wattle-and-daub candidate.
Implemented as a separate local 0.1.2.110021 candidate, including optional attached brackets.
See JETTIED-STOREYS-CANDIDATE.md for qualification and review instructions.
The earlier candidates, artifacts and preview worlds remain preserved.

## Intended result

Players should be able to build a framed upper floor projecting 0.5 or 1 block
beyond the storey below, then repeat that projection on another storey. Two
successive 0.5-block steps put the top facade 1 block beyond the ground facade;
0.5 followed by 1 puts it 1.5 blocks beyond. Each floor remains an ordinary room
with a level, walkable floor and useful additional interior space.

The underside needs a coherent timber structure: a continuous edge beam below
the upper wall, joists running back into the building, and floorboards visible
between the joists. Corner overhangs need one joined structure on both elevations.
Optional brackets should sit below principal posts, rather than repeating below
every infill panel. No structural load or collapse simulation is needed.

The architectural term is a jetty. The Vernacular Architecture Group describes
the jetty bressumer as the sill beam of an upper floor carried on projecting joists
or brackets. DBRG's glossary describes the diagonal dragon beam used where two
adjacent walls are jettied. These inform the proposed appearance, rather than a
simulation of historic carpentry:

- https://www.vernacularbuildingglossary.org.uk/a-z/jetty-bressumer14/
- https://www.dbrg.org.uk/glossary

## Findings in the current implementation

- FrameBlock and WallGeometry occupy nearly the entire depth of a grid cell.
  The small plaster recess is cosmetic. Moving an existing full-depth model by
  0.5 would put rendering and collision into a neighbouring cell, without giving
  that neighbouring space a placeable owner. Use actual partial shapes instead.
- The current frame metadata already uses all 16 values: four facings multiplied
  by four pattern/finish combinations. Thickness cannot be another saved flag on
  those IDs. New shapes need additional IDs, with current IDs and metadata retained.
- Wall connections currently compare block neighbours and facing axes. Partial
  walls need to compare their actual face planes and timber endpoints: two
  neighbouring cells can contain wall pieces on different planes.
- Existing cob slabs are horizontal, unframed slabs. They neither move the wall
  plane horizontally nor provide the timber edge and underside required here.
- Shared geometry, the baked model and the bounded render invalidation mechanism
  are suitable foundations. Rendering, collision and targeting must continue to
  consume the same geometry. Partial walls need their own lighting and side-solid
  rules; do not copy the current full-cell lighting workaround indiscriminately.

## Recommended building kit

| Component | Dimensions / purpose |
| --- | --- |
| Vertical half wall | 1 block long, 1 high, 0.5 deep; both faces finished. All existing infill and framing patterns. |
| Short half wall | 0.5 long, 1 high, 0.5 deep; closes a side-wall return where the facade falls halfway through a cell. |
| Half-wall corner / end post | Appropriate partial footprint with a single shared corner post and finished wall ends. |
| Timber jetty floor | A floor layer within a 0.5-high envelope, including boards, joists and the outer edge beam. |
| Partial jetty floor | Half and quarter footprints to terminate the floor exactly at a half-grid facade or corner. |
| Jetty bracket | Optional later detail beneath principal supports; unnecessary for basic placement. |

Use all six existing woods and both wall finishes. Timber floor components use the
six woods, without daub finishes. Keep the half-wall timber face widths and grain
consistent with the current kit; reducing wall depth must not shrink the visible
posts, rails or stud spacing. Crop diagonal members at partial boundaries while
preserving their angle. Do not stretch a one-block diagonal into a steeper short one.

Keep ordinary full-depth walls available. The slim kit is an additional building
choice. A builder who wants a one-block-thick wall spanning, for example, Z=[0.5,1.5]
can place two parallel half walls in neighbouring cells. A thickness occupying
Z=[0,1] uses the existing full wall. Neither requires moving a block outside its cell.
Combining matching halves within one cell could be a convenience after the core
kit works, but arbitrary mixed half-block contents are outside this feature.

## Placement and half-grid rules

Horizontal coordinates of facade planes and returns use increments of 0.5.
Floor heights can remain on whole-block levels; the half-height floor layer sits
below the upper wall. This feature does not require half-height wall courses.

For a simple coordinate example, let positive Z point towards the street. A thin
ground-floor wall can occupy Z=[-0.5,0]. Its successors can occupy [0,0.5],
[0.5,1], then [1,1.5]. Each wall is 0.5 thick and its exterior face steps forward
by exactly 0.5; alternating which half of a host cell is occupied achieves this.
Whole-block steps use the same parts one cell farther out. Floors extend to the
new facade, so the effect changes the usable interior as well as the exterior.

Placement should follow vertical-slab conventions:

1. Place the jetty floor at the desired storey height, then the upper walls.
2. A wall-face click places the half wall against that face; on top or bottom
   clicks, the clicked horizontal half and the player's facing select the wall plane.
3. Continuing a run from a matching half wall inherits its plane. Sneaking allows
   deliberate placement against another face. Never relocate previously placed walls.
4. Show distinct full and half-wall items. Provide simple full-to-half cutting
   recipes with equal material yield. Short returns and corner posts should have
   identifiable item models and names, rather than a hidden cycling tool mode.
5. Choose corner footprint from the intended occupied quadrant and connection
   directions at placement. Save that footprint explicitly. Adjacent pieces can
   derive timber joins, but removing a neighbour must not move or fill the wall.

Ordinary Minecraft blocks still reserve their host cell. Empty space inside that
cell is genuinely clear for movement and collision, but cannot also contain an
unrelated torch, slab or different wall block. Avoid promising multipart editing.
Placement must test the whole intended footprint and fail without consuming an
item if it would require an occupied cell. No geometry should spill outside its
own host cell.

## Corners must be part of the first implementation

A half-depth straight wall is not sufficient for a complete half-grid building.
Consider an outside corner with the room extending towards positive X and Z.
Let a cell cover X,Z=[0,1]. Depending on the exterior facade planes, its wall
occupancy is:

| Outer planes in that cell | Filled wall footprint |
| --- | --- |
| X=0, Z=0 | L: [0,1]x[0,0.5] union [0,0.5]x[0,1]. |
| X=0, Z=0.5 | Half: [0,1]x[0.5,1], with the outside post at (0,0.5). |
| X=0.5, Z=0 | Half: [0.5,1]x[0,1], with the outside post at (0.5,0). |
| X=0.5, Z=0.5 | Quarter: [0.5,1]x[0.5,1], with the outside post at (0.5,0.5). |

These are four configurations, not four unrelated decorative styles. Rotate and
mirror their geometry for other corners. Concave corners and full-to-thin returns
also need explicit coverage rules. Keep one continuous post at the actual outside
corner, and turn the rails around it without doubled beams or exposed daub seams.
Do not render a filled quarter automatically just because two distant planes exist.

The floor's footprint is the room area up to its outer boundary, rather than the
wall's L shape: the corresponding convex floor corners above are full, half, half
and quarter cells. At a two-sided jetty, turn the perimeter beam around the corner
and provide a diagonal underside timber where the joist runs meet. Hide internal
end faces only when the neighbour actually covers that face at the same height.

## Floor edge and underside

Use one jetty-floor system for both projection sizes. Extend it a half cell or a
whole cell as required; do not make a special wall block that only fakes an overhang.
Interior floor pieces allow a whole-block projection without an extra visible
perimeter beam halfway beneath it.

- The beam follows the upper wall's outside plane. The wall sill and the floor
  edge should read as connected members, with intentional depth and no z-fighting.
- Boards form a continuous walkable top surface. Underneath, board soffits are
  visible between joists; there must be no exposed plaster bottom or missing faces.
- Joists run perpendicular to the projecting facade, at a stable spacing. Their
  grain follows their length and cut ends use end grain. Clipping at half cells
  must retain the same spacing, including across host-cell boundaries.
- Derive perimeter joins from occupied floor faces, wood, height and joist axis.
  Support straight runs, convex and concave corners, and visible end caps. Do not
  infer a new footprint from whatever walls happen to stand above or below it.
- A bracket, if included, is a separately placed optional component within its
  own cell. It must meet the lower post and upper timber, rather than float.
- Keep the floor top on the ordinary block-height boundary. Match collision to
  the boards and timbers, including overhead clearance beneath the projection.

Roof slopes can retain their current integer grid: a half-grid wall can have a
half-block eave using an integer-grid roof edge. Check the resulting roof/wall
join in the examples; special roof offsets and new roof materials are separate work.
Vanilla doors/windows also retain their ordinary placement grid. Do not claim
that arbitrary half-grid window/door openings are solved by the slim wall kit.

## Implementation sequence

1. Introduce shared half-grid footprints and timber endpoints. Prove plain daub,
   posts, rails, side returns and all corner configurations in a small development
   example before multiplying wood/pattern variants.
2. Add the timber floor geometry and partial floor footprints. Prove both the
   street-side underside and room-side floor meet the wall exactly.
3. Carry across all wall patterns and finishes, including cropped brace endpoints,
   recipes, item models and correct drops. Reuse the current wood textures.
4. Add preview buildings and qualify a separate candidate. Retain 0.1.1 unchanged.

Keep geometry and connections in common code, with the client consuming that
geometry. Model wall planes and member endpoints in world-space half-grid units,
including diagonal phase at short returns. Use bounded neighbour queries and
extend render invalidation to partial walls/floors. No tile entities or scheduled
ticks are necessary for these fixed shapes. Extra shape IDs are preferable to
reinterpreting old metadata. Budget orientation, handedness, corner occupancy,
pattern and finish before registration; a single four-bit flag set cannot encode
every combination.

## Acceptance examples and checks

Make one fresh disposable street scene with:

- A single 0.5-block jetty and a single 1-block jetty.
- Three-storey houses stepping 0.5+0.5 and 0.5+1; also a 1+1 fantasy example.
- A projection around two sides, including each half-grid corner configuration.
- Broad bays and close studding, raw and limewashed, in the six woods.
- Interior views showing the real extra room and level floors; views up from
  the street showing joists, edge beams, returns and optional brackets.

Verify all facings and mirrors, diagonal phase at short pieces, face lighting,
transparent empty halves, side solidity, torch/support placement, collision,
head clearance, ray targeting, drops, recipe yields, fire/config behaviour and
save/reload. Connection removal must not leave stale or floating members.
Repeat the current full-wall and legacy-world checks to establish that existing
builds retain their shapes and metadata. Automated checks and screenshots remain
separate from the user's visual and gameplay acceptance.

The scope is the jetty and its necessary partial wall/floor pieces. Tile hanging,
new masonry, leaded windows and the rest of the Rye palette remain deferred.
