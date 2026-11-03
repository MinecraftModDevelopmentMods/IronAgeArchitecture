# Horizontal rails — 0.1.8.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

The remaining horizontal gaps were real. A corner could draw a rail towards a window
while the intervening principal post drew only the half nearest that window. That left
a gap between the corner's rail and the centre of the post. Some corner pieces also
grew a return on their unconnected side.

Rails now complete a run through intervening structural panels to matching timber on
the other side. The search starts from an existing window or rail request and checks
the occupied wall strip and timber at that height. It stays bounded and does not ask
neighbouring blocks to calculate their connections recursively. Plain infill still
stops a run; unsupported ends retain the previous post/brace trimming.

Corner pieces draw only their connected legs. The back faces finish at the inside
post instead of continuing through the other wall. This removes the detached little
wood marks exposed by the first exterior close-up.

The changes use existing blocks, recipes and saved metadata. The content totals stay
at 510 visible variants and 2,010 default recipes, with 36 for Iron Age survival.

## Verification

- Two corner-direction tests fail against 0.1.7. The packaged 0.1.7 JAR also fails the
  new shared-face contact check with the reported corner/post gap. A third test
  reproduces the rear-rail end showing through the adjoining wall.
- All 50 unit tests pass after the fixes. Build/Javadoc, 967 generated resources,
  package/Java 8 audits and ordinary Eclipse output/classpath checks pass.
- The new native check covers 960 corner–post–rail/window runs: six woods, both
  finishes, four facings, sill/head heights, full corners and all partial corner shapes.
  It measures positive timber contact at 1,920 shared faces and removes each source
  to check that the derived rails disappear. The windows-disabled mode covers 480 runs.
- Thirteen packaged server modes pass, including reload, Furniture coexistence,
  family switches, era restrictions and the three previous-generation world imports.
  The window, diagonal-junction, saved-state and crafting matrices remain green.
- The native client checks models, materials, palettes and translated item hints.
  Twenty captures include dedicated exterior and interior window-to-corner views.
  The bundle also retains the 0.1.7 street screenshot for comparison.
- The installed Horizontal Rails Preview layout is checked through a disposable copy
  with the final player JAR, including a plain reload without the probe. The final
  rear-face trim changes derived geometry, so the placed preview layout is reused.
  All 200 files in the seven earlier preview saves retain their original hashes.

Runtime SHA-256:
`e04d2d4f8d07efbca18da112e318a27e34ac2cbd94d9fd04afeb0ea0013b27a9`

Evidence: `D:\IronAgeArchitecture\manual-testing\horizontal-rails-0.1.8-qualified-02`.
The study root retains the red unit and native results. Its initial runtime used the
old probe because the failing unit task stopped probe preparation; that run is not
the new contact check. `red-runtime-02` contains the reproduced gap after preparing
the new probe. The earlier 0.1.8 render and receipts remain in the separate first-pass
roots; they exposed the rear-face cut ends and are not the final candidate proof.

Candidate: `D:\IronAgeArchitecture\candidates\0.1.8.110021-horizontal-rails`.
It contains player/sources/Javadoc artifacts, checksums, source snapshot, building guide,
changelog, preview ZIP, screenshots and hash-bound receipts/logs.

Restart the Eclipse client and open **Horizontal Rails Preview**. Check the sill and
head rails from the window jamb, through the next post and into the corner, from both
the street and the room. The screenshots have been inspected; player gameplay review
remains separate from the automated results.

Earlier saves and frozen candidates remain separate. This increment is local and
uncommitted on `feature/1.10-era-palettes-and-joins`. Nothing was pushed, merged or published.
