# Jetty brackets — 0.1.4.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

The brackets beneath jettied floors now have outward-facing sides and end caps.
Their previous outline ran clockwise, which turned the extrusion inside out and
showed surfaces behind the support. It could also expose coincident faces at the foot.
The corrected outline has the same shape and removes its duplicate closing point.

The renderer also discards clipped fragments that have no area after conversion to
Minecraft's float vertex coordinates. These fragments were found at the attachment
while checking the actual packaged bracket meshes. Block IDs, metadata, recipes,
collision footprints and bracket drops are unchanged from the era-palette candidate.

## Review

Launch the ordinary Eclipse client and open **Jetty Brackets Preview**. Look underneath
the half-block and whole-block projections, especially from either side of a support.
The earlier four preview saves and the frozen 0.1.3 bundle remain separate.
Existing brackets receive the geometry fix automatically when the new version loads.

## Verification

- 36 unit tests pass. The added bracket-face test failed against the previous outline
  and passes with the corrected one across every valid footprint, reach and direction.
- Build, generated-resource checks, Java 8/package audits and the Eclipse production
  classpath check pass. Normal Eclipse output is synchronized without the test mod.
- Packaged core, saved-world reload and the 0.1.2 preview import pass all 4,448 saved
  states, 1,077 recipes, placement/drop checks, rotations, mirrors and walking checks.
  The core run also repeats the eight era policies and 16 content configuration cases.
- The native client inspects actual baked bracket vertices in every available wood,
  reach and saved rotation. Each emitted bracket face must have area and point out
  of the solid. The first packaged check caught collapsed attachment fragments;
  the renderer correction passes the same assertion.
- Twelve native captures include close views of a full bracket from both sides and
  the feet of half- and full-reach brackets. Both era tabs retain their expected contents.
- The new preview is saved and reloaded with Architecture alone, without the test mod.
  Automated checks and captured views remain separate from the user's gameplay review.

Runtime SHA-256:
`135db73299ec934d6040172dfd1901b143c3b4aaf71c7b4d177971162f1e386d`

Checked runtime evidence: `D:\IronAgeArchitecture\manual-testing\brackets-0.1.4-checked`.
The failed unit/runtime evidence remains under `brackets-0.1.4-early` and
`brackets-0.1.4-final`. The latter also holds an initial passing native run before the
closeup cameras were lowered beneath the floor. No failed result was relabelled.

The separate candidate is `D:\IronAgeArchitecture\candidates\0.1.4.110021-jetty-brackets`.
It includes the runtime, sources and Javadoc artifacts, checksums, screenshots,
receipts, clean preview ZIP, source snapshot and manifests.

The changes remain local and uncommitted on `feature/1.10-era-palettes-and-joins`.
Nothing has been pushed, merged or published for this increment. Hosted CI has not run.
