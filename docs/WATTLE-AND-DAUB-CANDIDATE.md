# Wattle and daub — 0.1.1.110021 local handoff

The wall redesign is implemented for Minecraft 1.10.2 / Forge 12.18.3.2511. Wider Rye
building families remain deferred. This candidate remains local and uncommitted on
feature/1.10-initial-cottage-kit; there has been no feature push, PR, merge or release.

## Review the result

Launch runClient.launch or `gradlew.bat runClient`, then select **Wattle and Daub Preview**.
The new save contains a broad-bay building, a close-studded building, inside/outside corners,
all four corner rotations and the six-wood module catalog. Vanilla masonry/glass and the
existing slate roof are used as context; no additional Rye palette families were introduced.
The original Architecture Cottage Preview and the 0.1.0 candidate bundle remain separate.

The wall family now has plain infill, posts, optional X-bracing, sill/head rails, single/close
studs, left/right diagonals, and corner/end posts. Matching rails turn around a single corner
post. Studs extend into the rails; diagonal runs terminate in matching posts/corners/rails.
Plaster is slightly recessed and grain follows the timber. Each module is a filled wall block.

Post/rail placement controls the structural layout. Matching rail links pass through structural
intersections and stop at plain infill, wall ends, different woods or incompatible orientations.
The direction of placement chooses the wall plane/corner rotation. Nothing guesses storey heights.
Raw and limewashed finishes and all six vanilla woods are available: 132 total visible variants,
including the original cob and roof families, with 229 crafting/conversion recipes.

## Qualification

- 13 unit tests pass: eight wall-geometry tests and five retained roof tests.
- 194 generated resources pass the read-only generator check. Superseded frame meshes are removed;
  all six 32x32 beam textures are present. The build, Java 8 artifact audit and Eclipse checks pass.
- Native packaged core/reload tests pass 656 saved states and their drops, all 229 recipe outputs
  without shadowing, 488 module placement combinations, rotation/mirror round trips, and wall joins.
- Join checks cover post/sill connections, rail removal, wrapped corner rails, links through posts,
  infill termination, stud-to-rail extensions and diagonal terminals across every wood/facing.
- The retained slab-merge and four-facing roof-walking checks still pass. All 16 config combinations
  are exercised inside initialized Forge; the all-content-disabled packaged run also passes.
- Coexistence with the retained Furniture 0.3.0.110021 JAR passes without a Furniture dependency.
- A copied frozen 0.1.0 world loads and retains all 96 original timber block/state/variant records.
  The old registry names and four item subtypes remain readable; their appearance is revised.
- The actual obfuscated client loads the packaged mod, bakes every wall and roof state and every wall
  inventory subtype, runs the common assertions and captures native screenshots. No missing models,
  missing variants or missing asset-index errors occur in the final client run.
- The preview is re-saved and reloaded with Architecture alone. The verification mod is absent from
  its saved mod list. Eclipse production launches exclude test/probe code and bin/main is synchronized.

Runtime SHA-256:
`e071b29acceecd4837cadd9e2a9a529e2f4143ce035823070fb7920e5bfbf05c`

Candidate bundle: `D:\IronAgeArchitecture\candidates\0.1.1.110021-wattle-and-daub`.
Final native evidence: `D:\IronAgeArchitecture\manual-testing\wattle-daub-0.1.1-final`.
The bundle includes artifacts/checksums, screenshots, receipts, the clean preview, source snapshot
and manifests. Full early visual evidence is retained under wattle-daub-0.1.1 and its lighting/joins
iterations: the initial black-plaster rendering and incomplete member terminals are not passes.
Forge 1.10 still emits its legacy signature/patcher warnings; the logs retain them.

Automated and agent visual checks are complete. The user's manual visual/gameplay acceptance remains
pending. Hosted CI and cross-platform artifact equality have not been established.

## Art and implementation

[art/beam-prompts.json](../art/beam-prompts.json) records the complete built-in image-generation prompts.
Full-resolution sources are art/source/beam_*.png; the 32x32 runtime reductions are in the block textures.
Original daub textures are reused. WallGeometry supplies both the meshes and collision/ray bounds.
Connections use render-only extended state, with no wall tile entities or scheduled ticking.
