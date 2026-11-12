#!/usr/bin/env python3
"""Keep the README roadmap percentage and native SVG bar in sync with statuses.

Run from anywhere in the repository with:
    python3 tools/update-roadmap-progress.py

Only exact two-column roadmap rows (Name | Status) are counted. Items marked ➖
are outside the completion denominator, including optional Plugins marked ✅.
"""

from __future__ import annotations

import re
import sys
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
README = ROOT / "readme.md"
SVG = ROOT / "docs" / "roadmap-progress.svg"

START = "<!-- roadmap-progress:start -->"
END = "<!-- roadmap-progress:end -->"
ROW = re.compile(r"^\|\s*[^|\n]+\s*\|\s*(✅|📜|➖)\s*\|\s*$", re.MULTILINE)


def build_progress(readme: str) -> tuple[str, str, int, int, str]:
    section = re.search(r"(?ms)^## Roadmap\s*\n(.*?)(?=^## |\Z)", readme)
    if not section:
        raise ValueError("Missing '## Roadmap' heading")

    statuses = ROW.findall(section.group(1))
    if not statuses:
        raise ValueError("No two-column roadmap status rows found")

    complete = statuses.count("✅")
    countable = len(statuses) - statuses.count("➖")
    if countable <= 0:
        raise ValueError("No countable roadmap items")

    fraction = Decimal(complete) / Decimal(countable)
    percentage = (fraction * 100).quantize(Decimal("0.1"), rounding=ROUND_HALF_UP)
    label = f"{percentage:.1f}%"

    # The progress graphic lives in this repository: no external image service.
    inner_width = Decimal(698)
    fill_width = (inner_width * fraction).quantize(Decimal("0.01"), rounding=ROUND_HALF_UP)

    svg = f"""<svg xmlns="http://www.w3.org/2000/svg" width="700" height="42" viewBox="0 0 700 42" role="img" aria-labelledby="title description">
  <title id="title">Roadmap completion: {label}</title>
  <desc id="description">{complete} of {countable} counted roadmap entries are marked complete.</desc>
  <defs>
    <clipPath id="bar-clip">
      <rect x="1" y="4" width="698" height="34" rx="11"/>
    </clipPath>
  </defs>
  <rect x="1" y="4" width="698" height="34" rx="11" fill="#253041" stroke="#46566a" stroke-width="1"/>
  <g clip-path="url(#bar-clip)">
    <rect x="1" y="4" width="{fill_width:.2f}" height="34" fill="#2ed18a"/>
  </g>
  <text x="677" y="26" font-family="Arial, Helvetica, sans-serif" font-size="16" font-weight="700" fill="#ffffff" text-anchor="end">{label}</text>
</svg>
"""

    block = (
        f"{START}\n"
        f"**Overall completion: {label}** · **{complete} / {countable}** counted roadmap items marked ✅\n\n"
        f"![Roadmap completion: {label} — {complete} of {countable} items](docs/roadmap-progress.svg)\n"
        f"{END}"
    )
    marker = re.compile(re.escape(START) + r"[\s\S]*?" + re.escape(END))
    if len(marker.findall(section.group(1))) != 1:
        raise ValueError("Expected exactly one marked roadmap progress block")

    new_readme = marker.sub(lambda _: block, readme, count=1)
    return new_readme, svg, complete, countable, label


def main() -> int:
    original = README.read_text(encoding="utf-8")
    try:
        updated, image, complete, countable, percentage = build_progress(original)
    except ValueError as error:
        print(f"Progress generation failed: {error}", file=sys.stderr)
        return 1

    check = "--check" in sys.argv[1:]
    existing_svg = SVG.read_text(encoding="utf-8") if SVG.exists() else ""
    dirty = updated != original or image != existing_svg
    if check:
        if dirty:
            print("README progress is outdated; run tools/update-roadmap-progress.py", file=sys.stderr)
            return 1
    else:
        if updated != original:
            README.write_text(updated, encoding="utf-8")
        if image != existing_svg:
            SVG.write_text(image, encoding="utf-8")

    print(f"Roadmap: {complete}/{countable} complete ({percentage}); {'needs refresh' if dirty and check else 'up to date'}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
