# Crafting — 0.1.5.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

The wall kit now follows one chain: cob, plain wattle-and-daub infill, timber post panels,
then the chosen framing pattern. Full, thin and short walls use the same arrangements.
The [crafting guide](BUILDING-GUIDE.md#crafting) is the player-facing reference.

Three matching post panels in a row, column, diagonal or L make three rails, studs,
braces or corner/end panels. Single pieces swap paired designs without additional sticks.
Rows of other wall panels recover base posts, while vertical pairs cut and horizontal
pairs rejoin the wall sizes. Shaped corner pieces can recover their short end posts.
The conversions retain wood and daub finish.

All raw cob and daub shapes support single or eight-piece limewashing, including slabs
and stairs. Stairs now yield eight from six cob and recover three cob from four stairs.
Slabs, roof ridges and the cut wall/floor sizes can be rejoined without wasting material.
Brackets derive from matching full floors. Translated item hints explain the useful steps.

No block IDs, saved metadata, geometry or placed-world contents changed in this pass.
The five existing ordinary preview saves remain available; **Jetty Brackets Preview**
can be used to try the new recipes after restarting the Eclipse client.

## Verification

- 36 unit tests, build/Javadoc, 771 generated resources, package/Java 8 audits and Eclipse
  production classpath checks pass. Normal Eclipse output is synchronized without probes.
- All 1,863 default recipes craft their expected output. The exhaustive placement pass
  checks 11,150 shifted, mirrored or reordered grids in the real Forge crafting manager.
- All 1,416 recipes containing only Architecture materials preserve the equivalent
  material quantities, including wood, finish, wall size, corner cuts and roof/floor cuts.
- All 460 visible variants are reachable from basic ingredients. The solver respects
  wildcard recipe ingredients and repeats under all eight era policy cases. Known wall
  starters reject mixed wood. Tests use the actual registered recipes and item metadata.
- Twelve packaged server modes pass: core, reload, five era selections, Furniture
  coexistence, all families disabled and the three previous-generation preview imports.
  The default/later/mixed era selections contain 1,863 recipes; Iron Age alone has 36;
  empty or invalid-only selections have zero. The existing saved-state matrix is unchanged.
- The native client loads the packaged mod, checks every existing model and material,
  verifies both era tabs and calls every visible item's tooltip. Hints must be present
  and translated. The separate client runtime creates its own disposable preview.

Runtime SHA-256:
`7d7de0ed475d4d9a60b1f2cd25de9b351f1166fa04efabdca34b600d586bbdc5`

Evidence: `D:\IronAgeArchitecture\manual-testing\recipes-0.1.5-final`.
The early root retains the failing plain-infill starter contract against 0.1.4 and an
initial reachability-test failure caused by the probe treating wildcard ingredients as
exact metadata. The wildcard correction changes only the verification code.

Candidate: `D:\IronAgeArchitecture\candidates\0.1.5.110021-crafting`.
The bundle contains runtime/sources/Javadoc artifacts, checksums, the current crafting
guide, source snapshot, screenshots and receipts/logs. It reuses the byte-identical clean
Jetty Brackets Preview ZIP from 0.1.4; this pass changes recipes and hints rather than builds.
Earlier candidates remain separate. Automated verification does not replace gameplay review.

The earlier-candidate audit verified 166 delivery files and recorded three exceptions in
the 0.1.3 bundle: `evidence/probe-build.log` and `source-worktree.zip` are missing, and
Windows reports `evidence/wall-head-red.xml` as corrupted/unreadable. No earlier files were
modified or restored during this pass. `evidence/earlier-candidate-audit.json` retains the
full results. This does not affect the new candidate's own artifact and receipt checks.

The work remains local and uncommitted on `feature/1.10-era-palettes-and-joins`.
No push, merge or publication has been performed for this increment. Hosted CI has not run.
