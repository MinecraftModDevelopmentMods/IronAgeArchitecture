# Timber joins — 0.1.7.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

The gaps came from three different places. Some automatic rails extended all the way
across a panel even when they should have stopped at its stud or brace. Diagonal beams
in diagonally adjoining cells met only at a point, leaving a visible pinch where the
other two panel corners were missing. The preview generator also reversed some brace
patterns when it chose the other occupied half of a wall.

Automatic rails now finish on the timber they meet, using its actual outline at the
rail's height. Short-piece posts move before this calculation. Deliberate sill/head
panels and runs fed from both sides keep their full width.

Small triangular continuations carry diagonal beams through the adjoining wall corners.
They stay inside their own wall cells, use the matching wood and appear only when two
compatible brace endpoints meet. Removing either brace removes the junction. Different
woods, opposing slopes and offset wall planes cannot create a false join. Collision and
targeting use the same geometry as the rendered pieces.

The new preview's brace patterns account for the wall's facing, keeping complete bays
aligned across half-block steps. Existing placed patterns retain their selected design;
an incorrectly oriented brace in an older build still needs its pattern swapping.

No recipes, visible items, saved metadata or registry identities changed. There are
still 510 visible variants and 2,010 default recipes; Iron Age survival retains 36.
The building guide explains the distinction between automatic joins and deliberate rails.

## Verification

- Two focused rail-end tests fail against the previous geometry and pass after the fix.
  The original failures and corrected results are retained in the evidence bundle.
- All 47 unit tests pass, covering rail ends, both depths, short pieces, unchanged
  continuous runs, corner widths, cell bounds, wood selection and outward winding.
- Build/Javadoc, 967 generated resources, package/Java 8 audits and ordinary Eclipse
  classpath/output checks pass. Probe code remains outside player artifacts.
- Thirteen packaged server modes pass: core, reload, Furniture coexistence, family
  switches, three earlier preview imports and five era selections. The existing window
  checks, 4,448-state compatibility matrix and 11,444-grid crafting checks remain green.
- The native world check verifies eight complete preview brace bays and 96 corner
  arrangements across six woods, two depths, four facings and both diagonal directions.
  Negative controls cover missing endpoints, mixed woods, wrong slopes and offset planes.
- The native client checks the actual models, materials, palettes and item hints and
  captures eighteen views. The bundle includes the earlier street view for comparison.
- Timber Joins Preview is saved and reloaded with only the player mod before being
  installed alongside the six earlier saves. All 170 files in those saves retain their
  original hashes. The frozen 0.1.6 candidate and its screenshots remain unchanged.

Runtime SHA-256:
`736994ccdcac9134b7691c8060e69ae3b11f779b86e1413dabfe01791b9981dc`

Evidence: `D:\IronAgeArchitecture\manual-testing\timber-joins-0.1.7-qualified`.
The study root retains the original rail failures and first-pass artifacts. The first
render exposed the point-only diagonal joint; its screenshots and receipts remain in
the separate `timber-joins-0.1.7-final` root. A new corner test initially passed a 2D
outline to a 3D area helper; that test error is retained separately from the final pass.

Candidate: `D:\IronAgeArchitecture\candidates\0.1.7.110021-timber-joins`.
The bundle contains player/sources/Javadoc artifacts, source snapshot, checksums,
guide, changelog, preview ZIP, screenshots and hash-bound receipts/logs.

Restart the Eclipse client and choose **Timber Joins Preview** to review the result.
The screenshots have been inspected; gameplay review remains separate from automated
verification. Check the outside and inside joins around the windows, both levels of
the first house, half-step upper floors and the close-studded comparison house.

This work remains local and uncommitted on `feature/1.10-era-palettes-and-joins`.
No push, merge or publication has been performed. Earlier candidate bundles were not
modified; their previously recorded audit exceptions remain documented in the earlier
handoffs and retained audit report.
