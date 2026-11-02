# Jettied storeys — 0.1.2.110021 local handoff

The jetty upgrade is implemented for Minecraft 1.10.2 / Forge 12.18.3.2511.
It remains local and uncommitted on feature/1.10-initial-cottage-kit. No feature
push, PR, merge or release has been performed. Manual gameplay/visual acceptance
remains separate from automated qualification.

## Review

Launch the ordinary runClient.launch, then select **Jettied Street Preview**.
The save is installed in the project's run/saves, without the verification mod.
The five examples show a half-block jetty, whole-block jetty, successive half +
half, half + whole, and whole + whole projections. The last two also project
around the side. The six-wood, two-finish sample row is behind the houses.

Use the README crafting and placement instructions. Thin walls are full height
and half depth; short returns are also half length. Quarter, half and L-shaped
corner posts have outside/inside patterns. Left/right cuts are saved placement
variants sharing one inventory entry. Existing full wall IDs and metadata remain
readable, and their standalone geometry is retained.

Timber floors have full/half/quarter/L footprints. Their boards provide a level
walkable top. Joists and perimeter beams join against actual neighbouring floor
coverage, with diagonal ties beneath exposed corners. Each selected floor can
accept one matching half/full-reach bracket from below. A bracket occupies the
lower half of the floor cell; breaking it returns the floor and bracket items.
There is no collapse or support simulation.

All six woods and both daub finishes are supported. There are 460 visible item
variants: 448 block item subtypes plus 12 bracket items. The mod remains independent
of Furniture. The new kit follows the existing timberFrames/cob family gates.

Partial pieces reserve their host cell: movement and ray targeting can pass through
their empty halves, but unrelated blocks cannot occupy them. There is no multipart
editor or automatic merging. Doors/windows and roof offsets retain their existing
grids. The preview's window openings are deliberately unglazed; new window families
and the wider Rye palette remain deferred.

## Qualification

- 22 unit tests pass: nine jetty geometry, eight original wall and five roof tests.
  These cover saved footprint bounds, unscaled clipped braces, board coverage,
  perimeter suppression, rotated brackets and ties meeting all four outside corners.
- Build, 771 generated-resource checks, Java 8/package audits and Eclipse production
  classpath checks pass. bin/main and ordinary launch output are synchronized.
- Packaged core and reload tests pass 4,448 saved-state cases, 1,077 unshadowed recipe
  outputs and drops, rotations/mirrors and the original full-wall/slab/roof assertions.
- The native probe exercises 3,504 partial-wall item placements across faces, cuts
  and subtypes, plus explicit continuation/sneaking, occupied-placement rejection,
  empty-half targeting, offset-plane rejection and rail-removal checks.
- Floor tests cover perimeter removal/reappearance, level top collision, actual
  entity movement across a floor seam, clear space underneath, bracket addition,
  item consumption, combined drops and rejecting a full bracket on a quarter floor.
- All 16 configuration combinations pass inside initialized Forge. The disabled
  packaged run contains zero Architecture variants, recipes or saved-state cases.
- Packaged coexistence with the retained Furniture 0.3.0.110021 JAR passes without
  adding a Furniture dependency.
- Disposable copies of frozen 0.1.0 and 0.1.1 worlds pass compatibility checks:
  respectively 96 original timber records and all 656 previous block/state records.
- The obfuscated client loads the packaged JAR, verifies every wall/floor/roof model
  and wall/floor inventory subtype, rejects missing material sprites and captures
  six views, including a close underside view and an occupied upper-floor interior.
- The preview is saved, stopped and reloaded with Architecture alone (four loader/game
  mods total), then copied into ordinary run/saves. The clean reload has no missing
  verification-mod warning. Initial removal/startup logs are retained separately.
- Both earlier candidate bundles were checked against every entry in their existing
  manifests: all 21 original-cottage and 25 wall-redesign delivery files remain unchanged.

Runtime SHA-256:
`0f0065fbf41b76a35c80fed1a2bc846a40a92197e40e4c597825ce945369317d`

Candidate bundle: `D:\IronAgeArchitecture\candidates\0.1.2.110021-jettied-storeys`.
Native evidence: `D:\IronAgeArchitecture\manual-testing\jetties-0.1.2-final`.
Selected screenshots/client log are from its native-client-closeup directory. All
server and client receipts bind to the runtime SHA above. The later closeup probe
changes only the camera; the packaged player JAR is identical.

The bundle contains runtime/sources/Javadoc JARs, checksums, screenshots, receipts,
build/test logs, the clean preview ZIP, source snapshot and source/delivery manifests.
Full early evidence remains under jetties-0.1.2-early and jetties-0.1.2-joined. It includes
the clipping-test failure and the missing dark-oak material/board-edge UV visual
iterations. These were corrected, not relabelled as final passes.

Forge's legacy signature/patcher/Jansi warnings remain in the logs. Fresh dedicated
server setup also logs absent operator/whitelist files; the ordinary clean reload
uses its newly created files. No warning suppression was added. Hosted CI and
cross-platform artifact equality have not been established.
