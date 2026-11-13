# Development

The checkout is `D:\IronAgeArchitecture\IAA110\IronAgeArchitecture`; its parent is the Eclipse workspace.
`master-1.10` is the maintained MMD branch. The initial implementation branch is
`feature/1.10-initial-cottage-kit`. Historical `master` remains available in both repositories.

Use Java 17 to run Gradle, with `GRADLE_USER_HOME=D:\GradleCaches\minecraft-mods-shared`.
The wrapper is Gradle 9.6.1; ForgeGradle 7.0.34 compiles and runs the game with an Adoptium Java 8 toolchain.

```
node scripts/generate-resources.cjs
gradlew.bat clean check build javadoc auditArtifacts prepareEclipse verifyEclipseProductionClasspath prepareProbe
```

`build/libs` contains the obfuscated player JAR, sources, Javadoc and SHA256SUMS.txt.
`build/libs-dev` contains the development JAR. `build/probe/architecture-probe.jar` is an opt-in test mod;
never distribute it to players. Normal Eclipse launch classpaths exclude tests and probes.
prepareEclipse also synchronizes the compiled classes and processed resources to bin/main.

The explicit resource generator owns blockstates, models, English language files and metadata.
`node scripts/generate-resources.cjs --check` compares the current outputs and checks local parents/textures.
PNG source art and generation prompts are in art/. Runtime textures are 32x32 nearest-neighbour reductions.
Vanilla wood textures remain owned by Minecraft and are referenced by resource location.

## Publishing an alpha

Push the changes to the fork, then open a PR against MMD's `master-1.10`. After it
merges, wait for the Build and CodeQL checks on that commit. Check the player JAR
from the merged branch's Build run against the SHA-256 of the tested candidate.

Run **Publish Iron Age Architecture alpha** on MMD's `master-1.10`. Supply the
version from `gradle.properties`, the full merge SHA, the successful Build run ID
and the tested player JAR's SHA-256. The matching section of `CHANGELOG.txt` must
start with `<version> - Alpha`; it becomes the file's CurseForge changelog.

Leave **Upload the checked JAR** unticked to verify the inputs and package. Tick
it when the alpha is ready to go out. The workflow needs the `CURSEFORGE_TOKEN`
Actions secret and uploads only the player JAR to CurseForge project 1779832.
Check the file list and previous runs before publishing. If an upload was attempted,
check its file status in CurseForge before trying again; a workflow failure can
happen after CurseForge has accepted the file.

## Geometry and saved state

Each timber wood has five wall-family block IDs: post/cross, sill/head rail, single/close stud,
rising/falling brace, and corner/end post. Metadata remains `facing | panel << 2`, where panel
selects the family's two patterns in raw or limewashed finish. Inventory metadata is panel only.
The original timber_frame IDs retain their item/state metadata. Plain infill uses one additional
block ID and two inventory variants. Connections are unlisted extended-state properties, never metadata.
WallGeometry supplies the clipped beam/brace parts for both meshes and collision. A bounded horizontal
rail search joins structural intersections; vertical/diagonal neighbours supply stud and brace terminals.
The client listener invalidates the dependent render range after updates without polling/ticking.
Walls use full-cell lighting classification with opaque light blocking and non-opaque face culling, so
recessed plaster samples the lit adjacent cell rather than its own dark interior in Forge 1.10.
Roof blocks save facing. Clay verges also save which half overhangs the gable.
Inner/outer corners and ridge caps derive from same-material neighbours.
Slopes rise one block per block; ridge caps rise half a block and belong one row above the adjacent slopes.
The roof mesh is smooth; collision is a conservative approximation within 1/16 of the surface.
No architectural block needs a tile entity. Smart wall edits are coalesced into bounded end-of-tick work.

WindowBlock is a separate light-transmitting family. Metadata is horizontal facing in
bits 0-1, open in bit 2 and cached power in bit 3. Wood, finish, depth and leading live
in identity. Hidden right-hinged and rear-half thin identities have no inventory items;
all drops and picks resolve to the matching canonical window. Existing saved IDs stay intact.
Rear-half placement keeps a thin wall's occupied plane independent of the outward normal.

WindowGeometry supplies sash-local UVs, clipped owner/helper meshes and collision boxes.
Timber and lead use the solid layer; glazing uses the translucent layer. The open sash is
split at the cell boundary without adding faces at the cut. Each outside cell contains
one hidden WindowLeafBlock pointing back to its owner, so its portion remains rendered,
targetable and solid when the two cells belong to different render chunks.

WindowController preflights a complete matching vertical column, snapshots changed cells
and commits under a callback guard. Opening needs an empty loaded outside cell per section
and an entity-free swept prism. It never consumes an obstruction or loads a neighbouring
chunk. Cached power changes operate the column; manual overrides leave that cache alone.
A failed powered opening retains the old cache and retries on later neighbour updates.
Helpers forward activation/mining, have no independent drops and remove only owned links.
One-off chunk-load tasks reconcile loaded borders, including deferred power queries;
there are no tile entities or scheduled block ticks. A short-lived queue retries only
entity-blocked power edges every ten world ticks, because entity movement does not send
block updates. Idle windows and ownership links are never polled.

PartialWallBlock adds fixed thin/short/corner footprints without changing existing saved IDs.
WallRuns compares world-space occupied strips, including offset planes and shortened endpoints.
Partial walls retain the original pattern/finish/facing layout; short end posts use two finish
variants. Hidden right-handed placements drop/pick their canonical left-handed inventory item.
Partial walls use slab-style neighbour brightness and are not classified as full cubes.

JettyFloorBlock uses facing bits 0-1, joist-axis bit 2 and bracket-reach bit 3. Shape and optional
local bracket direction are carried by block identity. Bracketed floor IDs have no ItemBlocks;
their drops contain the canonical floor and the matching bracket item. The floor's fixed
deck footprint never changes with neighbours. Covered perimeter faces and a lower wall-head beam
are derived from neighbouring coverage. Connection bits 16-19 describe the quarters occupied by
the supporting wall. The beam fills only those quarters below the upper-half deck; cantilever air
remains open. Removing or replacing the lower wall invalidates the floor mesh.
JettyFloorGeometry supplies boards, timbers, corner ties and brackets to the same collision/model
path as walls. Board edge UVs include vertical depth and stay inside their atlas sprite bounds.
FrameItem checks collision against the chosen placement state, including hidden handedness,
rather than the canonical inventory block's default shape.

SurfaceGeometry clips buried faces from the shared solid parts. At coplanar intersections,
boards, posts and rails have deterministic ownership; braces sit slightly behind the main frame.
Diagonal covers are subtracted as convex prisms, so their end caps replace the plaster
they occupy without cutting rectangular holes around the sloping timber.
The collision union continues to use the solid parts. Horizontal ties reverse their reflected
mesh winding so the exposed underside remains visible.
One-sided automatic rails are clipped against the timber outlines at the rail's height.
Partial walls do this after shifting short-piece posts and clipping their footprints.
Explicit rail panels and runs requested from both sides retain their full span. The
preview generator converts its world-space brace slope to the selected strip's local
axis; rear-half strips reverse local X. Native checks verify eight complete brace bays.
TimberCorners fills the two small missing quadrants of a diagonal block-corner joint
inside the adjacent wall cells. Two matching, aligned brace endpoints must agree on
wood; lone endpoints, mixed woods and offset planes do not produce a continuation.
A separate unlisted property carries four three-bit timber choices. Part.timber selects
the matching existing sprite on otherwise plain infill; the model cache includes this
property. Shared parts supply rendering, collision and targeting. No saved metadata,
block IDs, helper blocks or tile entities are needed for these junctions.
RailJoins completes source-to-anchor runs through structural panels. It starts from
the direct rail requests and searches the opposite side for aligned matching timber
at that height. It never recursively evaluates another block's connections. Partial
corners draw only their requested outgoing legs. Native verification measures positive
timber contact at 1,920 shared faces across 960 corner/post/source arrangements and
checks that removing the source clears the derived rails.
The back face of each corner rail starts at the inside post. Carrying it through the
full corner cell exposes its cut end on the perpendicular wall as a detached wood mark.
Bracket outlines must run counter-clockwise without a repeated closing vertex. Clockwise
outlines turn the entire extrusion inside out, exposing the back of the attachment surface
and making the support appear hollow. Unit tests and the native client check the actual
bracket face winding; the native check covers both reaches and every saved direction.

## Smart walls

SmartWallBlock keeps its wood, finish and full/half/corner footprint independent of its
appearance. The two visible profiles have canonical items. Joining two half panels
creates an L footprint which drops both original panels. Each wood uses two block IDs.
The full ID packs facing in bits 0-1, finish in bit 2 and the L footprint in bit 3,
using all 16 metadata values. The thin ID retains facing and finish in bits 0-2.
Its old low-eight values and the full wall's old low-eight values keep their meanings.
Separate smart-corner IDs only register with building.retainLegacyWallIds.

SmartFacade is a deterministic, coordinate-independent grammar over an occupied facade
with explicit openings and manual anchors. SmartWallPlanner surveys loaded coplanar
sections, including window ownership cells, independently on each elevation. Floors
split the survey into storeys. It caps a section at 2,048 cells, 48 columns and 32 rows.
An incomplete or oversized survey retains its saved framing. Bare sections use a simple
frame until their surroundings are known. Manual pieces contribute constraints in Hybrid.

SmartWallData saves four eight-bit face descriptors per wall in per-dimension WorldSavedData.
SmartWallEvents coalesces changes and repairs loaded chunk borders. Idle walls do no survey
work. Chunk-watch snapshots and changed descriptors are sent to the client; render workers
read only the cached descriptors. No tile entities, render-thread world scans or forced
chunk loads are used. Automatic is the fresh-install default and omits all 144 manual
framed-wall IDs. Hybrid and Manual register them. Plain daub, floors and brackets remain
independent of that choice. Existing configs default retainLegacyWallIds to true; a fresh
config defaults it to false. The missing-mapping handler fails affected world loads rather
than letting Forge discard omitted walls, including with automatic query confirmation.
Client recipe lists and creative browsing follow the server mode and
return to the local setting after disconnecting.

With all content families enabled, packaged registry checks measure 320 block IDs
for fresh Automatic, 464 for fresh Hybrid/Manual, or 470 with legacy wall retention.
The 150-ID saving consists of 144 manual wall IDs and six separate smart-corner IDs.
Count actual registry entries when checking the 1.10–1.12 budget; creative variants,
unlisted framing descriptors and baked meshes do not each consume a block ID.

The shared WallGeometry and SurfaceGeometry path clips the timber members. Smart occupancy
supplies collision, targeting, placement clearance and jetty support. Both depths can craft
leaded windows. Recipe accounting preserves framing quantity separately from glazing.

## Rounded smart cob

SmartCobBlock uses one ID and metadata bit 0 for finish. Curves are derived geometry,
not additional blocks or tile entities. Registration depends on cob/smartCob; acquisition
follows Automatic/Hybrid independently of timber framing. Plain cob IDs are unchanged.

SmartCobPlanner surveys connected sections up to 2,048 cells, 48×48 horizontally and
32 vertically. Short openings form virtual structural columns, then actual missing cells
are cut from a common course outline. Contact strips are shared by matching courses.
This prevents window rows from acquiring different silhouettes or texture phases.
SmartCobGeometry uses material-contained visibility shortcuts, quadratic contour fillets
and a shallow inward skin. Failed thickness checks reduce rounding; the last fallback is
the original footprint. No geometry is added to air cells. Java2D geometry is pure CPU
code and runs headless on the dedicated Java 8 server.

SmartCobProfile stores immutable, cell-clipped contours at 1/4096 precision, boundary
ownership, smooth normals and contour-distance texture phase. The versioned per-dimension
SmartCobData and separate bounded SmartCobNetwork snapshots leave timber data unchanged.
Chunk events repair loaded borders without forcing loads. Idle walls do no surveys;
only entity-blocked expansions have a short retry queue until the obstruction clears.

SmartCobMesh supplies rendering and exact triangle selection. Its 1/32 collision grid
merges occupied rectangles, keeping conservative collision within 1/16 block of the mesh.
Interior cut faces are hidden and neighbouring courses subtract shared caps. Chunk-baked
meshes use smooth vertex lighting; atlas wraps are split before baking to avoid stretched
horizontal texture stripes. Vanilla light blocking remains cell-based on this legacy engine.

The default Automatic totals are 321 registered IDs, 160 visible variants and 348 recipes.
Hybrid adds two visible variants and six recipes to its previous totals. Manual retains
the new ID for saved walls, but its visible variants and recipes do not change. Iron Age
survival in Automatic/Hybrid has 42 recipes, including the six smart-cob conversions/finishes.

## Era membership

EraCatalog supplies both creative membership and survival recipe eligibility. Basic cob,
plain daub and thatch are shared by Iron Age and Late Medieval & Tudor; timber framing,
jetties, brackets, slate and clay tiles belong to the latter. Add future palettes to Era and assign membership
in the catalog. Shared items expose every relevant creative tab through their Item implementations.

ArchitectureConfig parses recipes.survivalEras at startup. All recipe registration passes through
one output-membership check; block/item registration is independent of this selection. The policy
uses a union of the selected eras, defaults to all and logs unknown IDs without enabling them.
Changing it requires a restart. Client and server recipe selections should match; this legacy
Forge version does not synchronize the config or recipe catalog automatically.

## Crafting conventions

ArchitectureRecipes keeps vanilla-material starters separate from shaping. Full infill and
four matching planks start timber walls; post panels then supply every family at the same
size. Three posts in a row, column, diagonal or L produce three rails, studs, braces or
terminal panels. Paired designs flip one-for-one. Rows of three other wall panels recover
base posts. Raw/washed finish and wood metadata stay explicit in every step.

Two vertical larger pieces cut to four smaller pieces; two horizontal cuts rejoin into one
larger piece. Corner recovery returns the equivalent number of short end posts. Cob stairs
yield eight from six cob and recover three cob from four stairs. Roof slopes/ridges follow
the same 2:4 cutting and 2:1 joining convention. Brackets start from matching full floors.
Single limewashing and eight-piece batches share one helper and keep shape/pattern metadata.
Leaded panes start with eight glass panes around iron. One pane and one matching full/thin
principal-post panel produce one window. Glazed windows have no cutting/rejoining recipes.
Conservation checks account for frame and glazing independently, preventing glass duplication.
The building guide is the player-facing reference; translated crafting hints live in the
resource generator and item tooltips.

## Native probes

Run scripts/invoke-runtime-probe.ps1 after prepareProbe. Modes cover core, reload, furniture,
disabled, windows-disabled, legacy, legacy-daub, legacy-jetties, iron-age, late-medieval,
mixed-eras, no-eras, invalid-era, clay-disabled, clay-only, manual, hybrid, automatic,
cob-disabled, cob-only, legacy-smart, legacy-fascia and legacy-fireplaces.
Only Forge/server libraries and the explicitly named Furniture JAR are read from retained runtimes;
no existing worlds or configs are copied or changed. Each mode has an isolated runtime except reload,
which verifies the saved core world. Servers bind to loopback. Keep failed logs when correcting failures.
legacy extracts a copy of the frozen 0.1.0 preview and verifies all 96 original timber state/variant records.
legacy-daub extracts the frozen 0.1.1 preview and verifies all 656 saved block/state records.
legacy-jetties extracts the frozen 0.1.2 preview and verifies all 4,448 saved block/state records.
The core run also loads eight era config cases, rebuilds only Architecture recipes, checks allowed
and excluded crafting grids and restores the original global recipe list. Separate startup modes
prove the selection survives pre-init and does not change registered content. Wall-head checks cover
all woods, partial-wall footprints and rotations, plus support removal and vanilla solid walls.
The core run also enables recipeMatrix: it checks every shifted/mirrored shaped grid and two
shapeless arrangements, then audits all material-only conversions and visible-item reachability.
The reachability solver respects wildcard ingredients and repeats under each era selection.
Known starter recipes also reject mixed wood. Receipts bind both matrices to the packaged JAR.
Window checks cover 2,304 metadata states, 576 directional columns, 144 saved columns,
placement/alignment, independent columns, blocked bridges, hand/power transitions, swept
entity clearance, owner/helper rays, player passage, harvesting, explosions and link repair.
Clay tile checks cover cap joining, four wall orientations, half-depth collision and
placement alongside windows. Roof trim checks cover both verge hands in every facing,
clearance behind the fascia and the ridge end join. The clay-only mode checks that crafting works with the
other building families switched off. Guttering checks cover fitting and recovery, joined
ends, both offsets and the rain path. The legacy-fascia mode opens a copy of the roof trim
preview and verifies the original bare fascias. Use a fresh runtime root for each candidate.

Fireplace checks cover saved state and canonical drops, firebox light and interaction, independent
flues across a chunk border, blocked and disconnected runs, the three outlets and roof collar pitch.
The core reload checks that the fires still draw from the correct floor.

The optional client probe requires iaa.probe=true, iaa.clientProbe=true and iaa.keepRunning=true.
With smart cob enabled it creates Rounded Cob Preview: a simple cottage, a stepped
oval and an L-shaped house with a rounded bay, plus an unchanged ordinary-cob corner.
It checks mesh/material variants, client contour synchronization and both creative tabs,
then captures street, corner, opening, interior and roof-off overhead views. Existing
roof pieces are restored before saving. Without smart cob it uses the timber preview
with different house sizes, windows, both jetty depths and a manual anchor.
Every visible item must also provide a translated crafting hint without tooltip errors.
The client script's PreviewName argument selects a separate save without replacing an earlier preview.
After verification copy the preview save into the ordinary run/saves directory and launch without the probe.
Screenshots and automated walk/collision checks do not constitute the user's manual acceptance.

scripts/invoke-client-preview.ps1 launches the actual obfuscated client and Forge universal JAR with
the packaged mod and probe. It reuses verified Mojang libraries and existing assets/native binaries;
the ordinary client launch does not use this harness. Do not place server classes ahead of the vanilla
client in the native classpath, because Forge's client binary patches require the exact client bytes.
