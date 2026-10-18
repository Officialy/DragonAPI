"""Check the boss RGB shader against its pinned Minecraft 26.3 source."""

import argparse
import hashlib
from pathlib import Path
import zipfile


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("source_jar", nargs="?", type=Path,
                        default=root / "build/moddev/artifacts/minecraft-patched-26.3.0.76-sources.jar")
    args = parser.parse_args()
    with zipfile.ZipFile(args.source_jar) as sources:
        native = sources.read("assets/minecraft/shaders/core/lightmap.fsh")
    expected_hash = "360d9dd915b69682e56636b0e1b1d8da2bd5fea315bd081e12117de12600c928"
    if hashlib.sha256(native).hexdigest() != expected_hash:
        raise SystemExit("Native lightmap shader changed; review its math before updating the pinned hash.")
    text = native.decode("utf-8").replace("\r\n", "\n")
    uniform = "    vec3 NightVisionColor;"
    multiplier = "color * vec3(0.7, 0.6, 0.6)"
    if text.count(uniform) != 1 or text.count(multiplier) != 1:
        raise SystemExit("Expected native shader edit locations were not unique.")
    expected = text.replace(uniform, uniform + "\n    vec3 BossColor;")
    expected = expected.replace(multiplier, "color * lightmapInfo.BossColor")
    target = root / "src/main/resources/assets/dragonapi/shaders/core/boss_lightmap.fsh"
    if target.read_text(encoding="utf-8") != expected:
        raise SystemExit("Boss shader differs from native math plus the two reviewed RGB edits.")
    print("PASS: pinned 26.3 shader; only the RGB uniform and boss multiplier differ.")


if __name__ == "__main__":
    main()
