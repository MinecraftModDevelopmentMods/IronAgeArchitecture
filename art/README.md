# Material textures

Created with the built-in image-generation tool on 2026-11-01. Source PNGs are retained
in source/. The four runtime PNGs in src/main/resources/assets/ironagearchitecture/textures/blocks/
are 32x32 nearest-neighbour reductions. No texture depends on an artist's local path.

Common prompt:

> Use case: stylized-concept. Asset type: a single seamless square Minecraft block material texture for Iron Age Architecture. Create precisely a 32 by 32 pixel art tile magnified with nearest-neighbour to fill the entire square image. Orthographic flat texture, uniform ambient illumination, no perspective, no text, no borders, no objects, no highlights or cast shadows. Low contrast handcrafted vanilla Minecraft-compatible palette, visibly discrete square pixels. Tile must repeat seamlessly on all four edges.

Per-material prompt additions:

- cob: Raw cob earth wall: warm medium muted ochre-brown clay, finely mottled earth and tiny scattered straw flecks. The surface is solid packed earth, no bricks or cracks.
- limewashed_cob: Limewashed cob plaster: warm ivory cream, subtly uneven hand-trowelled plaster mottling, tiny pale sandy flecks. No bricks, beams, holes or cracks.
- thatch: Golden straw thatch roof: muted wheat gold and brown straw bundles, subtle overlapping horizontal courses of short vertical straw. Fine pixel strokes; warm natural thatch, no tile grid.
- slate: Dark blue grey slate roof: three horizontal courses of slightly irregular overlapping rectangular slate shingles, staggered joints, restrained subtle edge definition and natural stone variation.

The historical modeltemplates/ directory retains the 2019 drafts and their authorship.
The new runtime geometry uses local templates and the shared Java roof mesh.

## Timber beam revision

Six new beam-side textures were generated with the built-in image-generation tool for the
0.1.1 wall revision. Complete prompts are in beam-prompts.json; full-resolution originals
are source/beam_*.png, with 32x32 nearest-neighbour runtime files named textures/blocks/beam_*.png.
The straight grain is rotated with posts, rails and braces. Existing Minecraft log-top textures
provide end grain. These assets replace plank-board faces on the modular timber wall family.
