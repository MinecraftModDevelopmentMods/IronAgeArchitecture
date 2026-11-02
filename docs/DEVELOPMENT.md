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
Roof blocks save facing only. Inner/outer corners and ridge caps derive from same-material neighbours.
Slopes rise one block per block; ridge caps rise half a block and belong one row above the adjacent slopes.
The roof mesh is smooth; collision is a conservative approximation within 1/16 of the surface.
No architectural block needs a tile entity or scheduled ticking.

PartialWallBlock adds fixed thin/short/corner footprints without changing existing saved IDs.
WallRuns compares world-space occupied strips, including offset planes and shortened endpoints.
Partial walls retain the original pattern/finish/facing layout; short end posts use two finish
variants. Hidden right-handed placements drop/pick their canonical left-handed inventory item.
Partial walls use slab-style neighbour brightness and are not classified as full cubes.

JettyFloorBlock uses facing bits 0-1, joist-axis bit 2 and bracket-reach bit 3. Shape and optional
local bracket direction are carried by block identity. Bracketed floor IDs have no ItemBlocks;
their drops contain the canonical floor and the matching bracket item. The floor's fixed
footprint never changes with neighbours. Only covered perimeter faces are derived at render time.
JettyFloorGeometry supplies boards, timbers, corner ties and brackets to the same collision/model
path as walls. Board edge UVs include vertical depth and stay inside their atlas sprite bounds.
FrameItem checks collision against the chosen placement state, including hidden handedness,
rather than the canonical inventory block's default shape.

## Native probes

Run scripts/invoke-runtime-probe.ps1 with core, reload, furniture, disabled, legacy and legacy-daub modes after prepareProbe.
Only Forge/server libraries and the explicitly named Furniture JAR are read from ServerTemplate;
no existing worlds or configs are copied or changed. Each mode has an isolated runtime except reload,
which verifies the saved core world. Servers bind to loopback. Keep failed logs when correcting failures.
legacy extracts a copy of the frozen 0.1.0 preview and verifies all 96 original timber state/variant records.
legacy-daub extracts the frozen 0.1.1 preview and verifies all 656 saved block/state records.
Use a fresh runtime root for each candidate; current default is manual-testing/jetties-0.1.2.

The optional client probe requires iaa.probe=true, iaa.clientProbe=true and iaa.keepRunning=true.
It creates its own Jettied Street Preview, checks all mesh/material variants, captures native screenshots, then shuts down.
After verification copy the preview save into the ordinary run/saves directory and launch without the probe.
Screenshots and automated walk/collision checks do not constitute the user's manual acceptance.

scripts/invoke-client-preview.ps1 launches the actual obfuscated client and Forge universal JAR with
the packaged mod and probe. It reuses verified Mojang libraries and existing assets/native binaries;
the ordinary client launch does not use this harness. Do not place server classes ahead of the vanilla
client in the native classpath, because Forge's client binary patches require the exact client bytes.
