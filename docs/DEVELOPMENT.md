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
Roof collision is a conservative approximation within 1/16 of the surface.
Only the architect's inventory workbench needs a tile entity; it does not tick. Smart wall edits are coalesced into bounded end-of-tick work.

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

Before the workbench addition, fresh Automatic totals were 321 registered IDs, 159 visible variants and 346 recipes.
Iron Age survival in Automatic has 40 recipes. Hybrid has 569 visible variants;
Manual retains the smart IDs for saved buildings without exposing smart acquisition.

## Smart thatch

SmartThatchBlock stores owner facing in metadata bits 0-1. The hidden part uses
bit 3 (LINKED) for a local underside; metadata zero remains the retired rounded
helper. PROFILE is derived from neighbours. No shape adds a block ID.

ThatchShape reads at most 25 positions in a one-cell neighbourhood. Missing chunks
are never forced. Its finite 56-profile kit has four orientations. ThatchGeometry
precomputes a half-block-deep skin, splits it at the cell floor, merges coplanar
faces and rotates the shared results. A straight slope uses 11 faces across both
parts; a flat section uses six. The underside follows the top at a constant vertical
depth. Collision uses fixed 16/32-column spans within 1/16 block; rays use the exact
closed mesh. No world-specific geometry is generated, transmitted or saved.

The lower fragment uses only the cell immediately below its owner. Both therefore
share a vanilla chunk, even when the roof crosses a horizontal chunk border.
Helpers have no item or independent drops. They forward picking, harvesting and
explosions to the owner above. Breaking the owner clears its part. Fire and forced
air replacement consume the linked owner; other block placement trims the lower
fragment and preserves the roof. No unrelated block is removed.

Edits repair only the immediate neighbourhood. New part placement checks entity
clearance. An entity-blocked fit schedules a normal ten-tick retry on that owner;
idle roofs do not tick. SmartChunkWork budgets one-off loaded-chunk conversion and
coalesced border repairs. The Level NBT marker ironagearchitecture_thatch_skin
records conversion. Ordinary block packets synchronize both parts. Legacy part
metadata zero and orphaned parts are removed during repair.

Models are baked once. Duplicate listed LINKED states share the same baked model;
all inert legacy variants share one empty model. Render calls read immutable quads.

Earlier rounded roof owners remain in the save and adopt the finite shapes. Old
SmartThatchData, template store, geometry and network classes have been removed.
Their files are left untouched and ignored; they are not migration inputs. Cob and
timber profiles remain independent. Retired thatch dirty records are ignored.

blocks.thatch controls smart and manual content independently of cob/timber.
Existing configs default retainLegacyThatchIds to true; fresh Automatic configs
omit the two old IDs. Hybrid/Manual always register them. Recipes respect mode
and era independently of registration. Never load original golden roofs with
their compatibility IDs disabled. Both smart identities stay registered in all modes.

## Smart-system performance

World access for timber/cob stays on the server thread. SmartWallPlanner and
SmartCobPlanner capture loaded descriptors; pure facade and contour work runs on
SmartShapeWorker with backpressure and cancellation on unload. Notifications are
coalesced, identical cob courses reuse contours, and finished profiles remain
available during edits. No worker reads or mutates a World.

SmartShapeDirtyData records unfinished cob and timber work. Completed saved
buildings retain their profiles on load; unfinished profiles and deferred borders
resume bounded surveys (2,048 blocks, 48x48 horizontally, 32 vertically). Thatch
has no roof survey or generated-shape queue. Local underside repairs use ordinary
block updates instead of a geometry synchronization channel. Its update cost
is bounded by the local block neighbourhood, independent of the size of the roof.

SmartLighting caches neighbouring light samples within a render rebuild's
ChunkCache only. Live World samples remain uncached; new contexts resample light.

prepare-city-fixture.cjs makes disposable 300/500-house loading fixtures by translating and
optionally rotating completed houses, including normals, collision and ownership. It does not
measure construction throughput or claim hundreds of unique architectural designs. Cold-city
client probes launch a fresh process with -Xmx2G, four reported processors, CPU affinity 0x55,
six-chunk view distance and a 60 FPS cap. They time entry and nearby shape synchronization,
check received cells against the server, travel between districts and verify light-cache refresh.
Qualification currently requires nearby synchronization within 20 seconds and retained heap
below 1,200 MiB after the route. Keep raw frame stalls and failed runs in the evidence; the host
GPU remains high-end and fresh processes do not imply a flushed operating-system disk cache.

scripts/invoke-smart-performance.ps1 measures real continuous-placement world ticks,
queue drain, idle/edit work and saves. It binds receipts to the player JAR. Its city is
kept within the spawn area because a headless server has no real player to keep the fixture
loaded. BlocksPerTick=4 exercises 80 placements per second. Native city runs additionally
measure frame intervals and verify a warm pass reuses the loaded city's baked meshes.
scripts/audit-simple-thatch.cjs checks the physical owner count and legacy cleanup.
The older audit-smart-city.cjs is retained for historical rounded-roof evidence only;
it does not qualify this finite-state implementation.
Keep intermediate failures and profiling trials alongside the final receipts; functional
probes, stress measurements and the player's gameplay acceptance are separate evidence.

## Dedicated-server multiplayer qualification

The player JAR installs LegacyHandshakeGuard through the early channel-registration
event. It serializes only incoming FML|HS packets, including forwarding handshake
completion through Forge's shared embedded channel. Ordinary gameplay packets keep
their normal network threads. The guard does not set any JVM properties or replace
Forge classes. Final qualification uses the default Netty thread count and includes
three reconnect rounds on each ten-client run. Receipts count installed guards and
record the actual network-thread override, if any.

SmartChunkWork gives cob, timber, thatch and window border validation one shared
two-millisecond world-tick budget. Unloaded/replaced chunks discard their jobs;
saved profiles stay readable while checks await their turn. SmartCobNetwork also
paces chunk snapshots, retains coalesced cell deltas during building, and cancels
unwatched chunks. A single bounded packet/check may overshoot the soft time target.

SmartShapePages saves cob overlays for 2x2-chunk areas using the dimension's
MapStorage handler. Original cob snapshots stay untouched; empty pages preserve
deletions. Ordinary autosaves write dirty pages, and corrupt pages fail visibly.
Keep cob page files with the save. Thatch now needs only ordinary chunk block data.
Native persistence checks cover dimensions, deletions and reload.

Use -GcLog for GC/safepoint timing and -Profile for Java Flight Recorder during
diagnosis. Flight recording substantially affected pauses in the observed Java 8
runs, so keep those recordings separate from final gameplay timings. Slow-tick
receipts include server-thread CPU time as well as elapsed time. -Reconnects 3
exercises four login bursts per client without restarting the server.
The former rounded-roof save benchmark has been retired with that storage engine.

`scripts/invoke-multiplayer-probe.ps1` launches a headless Forge server and 1, 5 or
10 separate native Forge clients over loopback TCP. Run each count with both
`-Layout together` and `-Layout spread`, using a fresh `-Root` every time. The
source world is copied; existing saves are never used in place. Use `-Prepare`
once to pre-generate the travel area and construction pads, then use that saved
world as the common source for the six measured cases.

For click-level checks use invoke-client-preview.ps1 with -ThatchInteraction
and -ColdCity. Add -CitySource to test a disposable copy of the 500-house city.
The probe uses actual client ray hits and one controller action per edit, with
no resends or explicit shape drains. It fills four gaps above cob, then mines and
replaces a pitched roof edge. City runs also mine an existing saved roof. Receipts
time immediate client prediction, observed server acceptance, matching local shape
selection and the first render-frame callback after synchronization. That callback
is not GPU completion. Screenshots provide separate visual evidence.

The probe is a separate test mod. Clients join together, walk beside the saved
walls, build through vanilla placement/mining packets, travel to another district
and return. Each client performs 91 actions, leaving 46 wall blocks and 25 roof
blocks. Grouped clients build separate plots in the same district; this does not
test several people editing the same connected facade. At each checkpoint, each
client must match the server's nearby roof, cob and timber descriptors. A completed
connection alone is insufficient. `scripts/audit-multiplayer-world.cjs <case-root>`
then checks saved material quantities and preservation of the original buildings
and cob profiles. audit-simple-thatch.cjs separately counts material owners and
legacy helpers, verifies cleanup markers and canonical metadata, and requires
no retired helper or orphaned underside remains in a cleaned chunk. Ignored old roof mesh files stay untouched. Run this read-only audit after the timed workload has stopped.

Receipts bind both player and probe JAR hashes. They include phase tick timings,
CPU time, loaded chunks, post-login TCP payload, shape synchronization, live heap
and sampled process memory. The steady target is at least 19 TPS with p95 tick
work below 50 ms during idle and simultaneous building. Always report maximum
ticks and join/travel delays too: the steady target does not mean hitch-free play.
Retain failed attempts and their logs, including login failures, alongside repeats.

Earlier burst-login tests reproduced a Forge 1.10.2 handshake failure in
HandshakeCompletionHandler, whose shared dispatcher attribute can be overwritten
by concurrent network threads. `-NetworkThreads 1` explicitly adds
`-Dio.netty.eventLoopThreads=1` to the test server. Keep this as a named launch
configuration for baseline comparisons, separate from the default. The player JAR
now handles this race through LegacyHandshakeGuard. This old control does not
reduce the four game/worker cores or change the player JAR. The probe's own
acknowledgement channel reads the handler from each raw FMLProxyPacket, avoiding
the shared NET_HANDLER attribute while retaining strict sender checks.

The server has a 2 GiB heap, four reported processors and affinity to four physical
cores. Clients use separate cores, 1.5 GiB heaps, six-chunk view distance and small
windows; background clients are capped at 10 FPS. This exercises real chunk baking
and network handling, but shared-host rendering is not a ten-PC FPS benchmark.
Loopback excludes internet latency, packet loss and constrained upload bandwidth.
A modern processor restricted to four cores is not proof for every average server.
Keep these limits, generated-city repetition and short phase durations with the
results. Gameplay acceptance and longer server soak tests remain separate.

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
cob-disabled, cob-only, thatch-disabled, thatch-only, legacy-cob, legacy-smart,
legacy-fascia and legacy-fireplaces.
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
With smart cob and thatch enabled it creates Smart Thatch Preview: a simple cottage,
a stepped oval and an L-shaped house with a rounded bay, plus an unchanged golden-roof comparison.
The preview configuration retains the original thatch IDs for that comparison. With
thatch disabled the client uses the rounded cob view instead.
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

## Architect's workbench and roof design preview

8721173 preserves the simple smart-thatch kit; 6024135 preserves the first workbench.
Version-1 plans and prism profiles retain the old solver. New designs use version 2,
with independent floor seating, four overhangs, eave drop, collar clearance and typed
parts for covering, timber, cheeks, fascia, lining, fittings and windows. Dormer
cheeks have independent top and bottom planes; geometry stays clipped to its cell.
Dormer valleys use clipped planar triangle prisms with the same slice/targeting
path; ordinary roof sheets retain their rectangle prisms. Roof windows share the existing sash kit with preserved local UVs. Pitched panes
rotate 45 degrees at the top hinge; dormer casements use the vertical column rules.

The two existing frame IDs retain their metadata. Two hidden window IDs add an
owner with facing/open/powered metadata and a metadata-0 helper. The clipboard is
one item. Fresh Automatic has 326 IDs, 167 visible variants and 354 recipes;
Iron Age has 60 recipes. Hybrid has 577 visible variants and 2,182 recipes.
The non-ticking bench is the only tile entity; clipboard-presence state changes
preserve its inventory and jobs. Chunk update tags omit that private inventory.

Typed immutable fragments are stored with their chunks. Paid identities and roots
are excluded from bounded mesh keys. Render meshes, exact ray triangles and
conservative collision come from the same prisms. Rebuilds run only on explicit
design edits. Loaded chunk borders queue one-off window link repair; blocked
powered windows retry on edits or a short-lived entity queue. Persisted removal records prevent an unloaded helper from restoring a broken
window; explicit reconstruction clears its record. No idle roof scans.

Shared drawings use revisions, a 32-entry undo/redo history and roof UUID plus
dimension clipboard bindings. Requests validate held tools or containers, range,
loaded chunks, permissions and bounded settings. The server determines geometry
and prices. Inbound roof requests take their sender from the raw FMLProxyPacket
handler, avoiding Forge 1.10's shared NET_HANDLER attribute race under simultaneous
connections. Replies and snapshots remain on the client-only simple channel.
The bill compares actual owned parts with the proposed drawing,
including open window poses and unchanged covering, gutter and lining attachments.

Construction uses real persisted escrow: changed old fragments are removed first,
and a unit becomes available only after its final fragment is removed. New units
are debited once, with additions reserved before work begins. The operation index,
escrow and spent identities survive save/reload. Cancel returns unspent material;
completed sections remain. All benches share 16 cells and a two-millisecond budget
between writes. A single block/light update can exceed that time slice.

Chunk snapshots use the shared 256 KiB-per-tick estimate and two-millisecond between-packet budget, at most 64 cells and a
16 KiB estimate per packet. Compressed encoded/decoded payload and decoded-cell
counters support multiplayer qualification. Player JARs exclude every probe.
RoofDesignProbe exercises survival escrow, cancellation, resumption, local rework,
window power, obstruction and helper cleanup. RoofDesignClientProbe exercises
native bench/clipboard traffic and captures the separate five-house scene.
RoofCityProbe seeds a disposable 500-house typed-profile fixture; bulk preparation
is not a player-construction throughput result. Multiplayer design cases edit via
the real clipboard and start construction through the ordinary bench request.

### Workbench gable infill

RoofPlan saves optional per-end infill settings and resolved material/wood choices.
Missing infill settings mean open ends, preserving earlier profiles. New drawings
default to matching the wall course. RoofGables samples at most two 34-block rows
when settings change; there is no idle or render-time wall survey.

RoofDesignPlanner emits closed, tapered WALL parts using the same piecewise roof
height function, with recessed plaster and separate timber for daub. Panels extend
one thirty-second into the covering so the interface cannot show sky; exposed
wood sits proud of the plaster. Geometry, collision, targeting, bills, escrow and
canonical drops use the existing typed-part path. No new registry identities or
recipes are needed. Ordinary wall/window cells can suppress only generated gable
parts; other roof geometry still fails obstruction checks.
