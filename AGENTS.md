# Iron Age Architecture

Read README.md and docs/DEVELOPMENT.md before changing this mod.

- Target: Forge 1.10.2, Java 8. Use the pinned wrapper with Java 17 running Gradle.
- Origin is SkyBlade1978/IronAgeArchitecture; upstream is MinecraftModDevelopmentMods/IronAgeArchitecture.
- This is an independent member of the series. Do not introduce a Furniture dependency.
- Keep shared behaviour in focused base classes; roof rendering and collision share RoofGeometry.
- Metadata is limited to four bits. Spend models before block IDs; do not add tile entities for simple variants.
- Edit the resource generator, then run it explicitly. Its --check mode must never rewrite resources.
- Keep common code free of client classes. Keep src/probe code outside player artifacts and ordinary Eclipse launches.
- Use disposable worlds. Do not alter Furniture worlds, checkouts, configs or existing qualification evidence.
- Compile and regenerate Eclipse output/launches before evaluating a runtime change.
- Separate automated results from manual visual/gameplay acceptance. No automatic feature push, PR, merge or release.
