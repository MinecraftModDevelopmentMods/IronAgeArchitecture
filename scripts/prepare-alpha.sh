#!/usr/bin/env bash
set -euo pipefail

build_dir="$1"
stage_dir="$2"
version="${RELEASE_VERSION:?Set RELEASE_VERSION}"
expected_sha="${PLAYER_SHA256:?Set PLAYER_SHA256}"
jar="iron-age-architecture-$version.jar"

[[ "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.110021$ && "$expected_sha" =~ ^[0-9a-f]{64}$ ]]
grep -Fxq "mod_version=$version" gradle.properties
grep -Fxq "minecraft_version=1.10.2" gradle.properties
grep -Fxq "mod_archive_name=iron-age-architecture" gradle.properties

(cd "$build_dir" && sha256sum --strict --check SHA256SUMS.txt)
printf '%s  %s\n' "$expected_sha" "$jar" | (cd "$build_dir" && sha256sum --strict --check -)

mkdir -p "$stage_dir"
awk -v heading="$version - Alpha" '
  /^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+ - / { if (found) exit; if ($0 == heading) found=1; next }
  found { print; if ($0 ~ /^- /) notes=1 }
  END { if (!notes) exit 1 }
' CHANGELOG.txt > "$stage_dir/CHANGELOG.md"
cp "$build_dir/$jar" "$stage_dir/$jar"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  printf 'Checked `%s` from build `%s`. SHA-256: `%s`.\n' \
    "$jar" "$BUILD_RUN_ID" "$expected_sha" >> "$GITHUB_STEP_SUMMARY"
fi
