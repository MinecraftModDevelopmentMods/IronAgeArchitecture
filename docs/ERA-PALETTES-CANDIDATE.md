# Walls and era palettes — 0.1.3.110021

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

The changes are local on `feature/1.10-era-palettes-and-joins`, based on the merged
0.1.2 code. This increment has not been committed, pushed, merged or published.

## Try the changes

Launch the ordinary Eclipse client and open **Era Palettes and Joins Preview**.
The previous three saves are retained separately. The new preview has been saved and
reloaded with Architecture alone, without the verification mod.

Look along the timber intersections and beneath the overhanging floors. Buried and
coplanar timber faces now have a single visible owner; braces sit slightly behind posts
and rails. A timber wall-head beam closes the half-block opening between lower walls
and upper-half floors. It follows the wall below, including corner footprints, and
leaves the projecting part open underneath. Existing block IDs and saved metadata stay
the same, so older builds receive the revised geometry automatically.

The creative tabs are **Iron Age** and **Late Medieval & Tudor**. Cob, plain daub and
thatch appear in both; framing, jetties, brackets and slate are in the latter. The earlier
palette currently contains shared basics rather than a complete Iron Age building kit.
The [building guide](BUILDING-GUIDE.md#era-palettes) explains the survival recipe setting.

Mermaid Street contains buildings from several periods. The framed palette's name is
based on Historic England's late-15th-century date for the
[Mermaid Hotel](https://historicengland.org.uk/listing/the-list/list-entry/1251961)
and its record of a 15th-century front range and 16th-century wing at
[5–6 Mermaid Street](https://historicengland.org.uk/listing/the-list/list-entry/1251938).
It is a style palette, not a claim that every piece reproduces one historical building.

## Checks

- 35 unit tests pass, including a regression that failed before the wall-head beam was added.
- The build, Javadoc, 771 generated resources, Java 8 artifact audit and Eclipse production
  classpath checks pass. The normal Eclipse output is synchronized and excludes probes.
- Twelve packaged server runs pass: core/reload, five era selections, three older preview
  imports, Furniture coexistence and all content families disabled.
- The initialized Forge era matrix checks eight parsed configurations, allowed crafting
  and excluded outputs, shared-material membership and unchanged creative availability.
- Default, late-medieval and mixed selections have 1,077 recipes. Iron Age alone has 19;
  empty and unknown-only selections have zero. All keep the 460 visible variants and
  4,448 tested saved states. The existing all-content-disabled switch still yields zero.
- Older previews retain 96 original timber records, 656 wall/roof records and all 4,448
  records from the previous jetty alpha. Drops, rotations, mirrors, item placement and
  actual walking/collision checks also pass.
- Native client checks cover every wall/floor/roof model and inventory subtype, material
  sprites and the actual creative-tab contents: 14 shared variants and 460 later variants.
- The ordinary preview reload uses only Architecture and Forge's three loader/game mods.
  Its second load has no missing verification-mod warning.

Runtime SHA-256:
`523c91a24825b6f2fc1cb2f47dcf00a2ede0c56fee363759433fdc92a8a4053c`

Evidence is under `D:\IronAgeArchitecture\manual-testing\eras-joins-0.1.3-final`.
Early failures and intermediate captures are retained under the separate `-early` root,
including the red wall-head test and a probe-only compile correction. The first final
palette capture did not keep the requested tab selected; its files are retained separately
from `native-client-tabs-04`, whose probe selects the private tab setter explicitly and
checks the selected tab before capture. The intermediate capture assertion failure and
stopped capture are retained. These changes affect only the test mod, not the player JAR.
Automated checks and screenshot inspection do not replace the
user's manual visual/gameplay review. Hosted CI has not been run for this local increment.

The separate bundle is `D:\IronAgeArchitecture\candidates\0.1.3.110021-era-palettes-and-joins`.
It contains the three player-facing artifacts, checksums, source snapshot, clean preview ZIP,
selected screenshots and verification logs/receipts. The earlier bundles' manifests were
rechecked: all 21, 25 and 35 delivery files respectively remain unchanged.
