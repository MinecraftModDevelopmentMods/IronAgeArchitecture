# Initial cottage kit — local handoff

This records the local checks when the candidate was prepared. References to uncommitted
work and hosted checks describe that point; Git history records later commits and merges.

Version: **0.1.0.110021**, Minecraft 1.10.2, Forge 12.18.3.2511.

The implementation is local and uncommitted on feature/1.10-initial-cottage-kit.
Both GitHub repositories use master-1.10 as their default branch, preserving the original scaffold/history.
No implementation push, PR, merge, public release or hosted CI run has occurred.

## Try the mod

Open Eclipse workspace `D:\IronAgeArchitecture\IAA110`, import the IronAgeArchitecture Gradle project,
and use runClient.launch. Alternatively run `gradlew.bat runClient` with the environment in DEVELOPMENT.md.
Select Singleplayer → **Architecture Cottage Preview**. The ordinary run has no verification mod installed.
The preview is creative, with daylight, mob spawning and fire spread paused for building review.

The cottage kit has 34 visible variants and 46 recipes. Raw/limewashed cob comes as full blocks,
stairs and slabs. All six vanilla woods have plain/cross-braced panels in both finishes.
Thatch/slate slopes form automatic hips and valleys; ridge caps complete the apex.
Dedicated beams, window surrounds, extra roof pitches and geological slate acquisition remain later work.

## Automated results

- Five geometry tests pass, covering surface seams, ridge pitch, all rotations and collision bounds.
- 98 generated resources match their generator and resolve local parents/textures.
- Native packaged servers pass all 46 crafting results without recipe shadowing, 256 placed states and drops,
  96 panel placement combinations, two slab merges, and walking both roof materials in all four directions.
- All 16 configuration combinations pass inside the initialized Forge runtime. Configuration is tested there
  because Forge's config constructor requires loader initialization; the failed plain-JUnit attempt is retained.
- Core save/reload, coexistence with the retained Furniture 0.3.0.110021 JAR, and all-content-disabled runs pass.
- The actual obfuscated Minecraft client and Forge universal JAR load the packaged mod; all roof variants bake
  and native screenshots capture the cottages and corners. This also runs the common native assertions.
- A copy of the preview loads and saves with Architecture alone, with the verification mod removed from its
  saved mod list. This cleaned copy is installed in run/saves and included in the candidate bundle.
- Java 8 bytecode/artifact audits pass. Eclipse production classpaths exclude tests/probes, and bin/main
  contains synchronized classes and resources. Git whitespace checks pass.

Runtime SHA-256:
`277df64749f4ac38cc692879ddeb7e9e3814cd5d4bd189b9960551f30c44f13a`

Candidate bundle: `D:\IronAgeArchitecture\candidates\0.1.0.110021-initial-cottage-kit`.
It contains player/sources/Javadoc JARs, checksums, native screenshots, receipts, test/build evidence and
Architecture Cottage Preview.zip. manifest.json binds the delivered files to their SHA-256 values.
The scripts and retained full runtime folders live under `D:\IronAgeArchitecture\manual-testing`.

Failed compilation/harness/setup runs are retained alongside the successful evidence; they are not passes.
The older Forge/Gradle tooling emits startup/deprecation warnings, including the Forge signature warning.
The development launcher additionally logs multi-release-library discovery errors; the final native client
uses the ordinary obfuscated runtime and avoids that development-launcher seam.

Manual visual and gameplay acceptance remains pending. Windows-local verification does not prove hosted CI
or cross-platform artifact equality. No published-release claim is made.

## Art

The four 32x32 material textures and their full-resolution sources are saved in the repository.
[art/README.md](../art/README.md) records the complete prompts and use of the built-in image-generation tool.
Existing 2019 model drafts and their credits remain in modeltemplates/.
