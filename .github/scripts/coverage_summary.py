"""Print a Markdown coverage table (per package) from JaCoCo's jacoco.csv.

Usage: python3 coverage_summary.py target/site/jacoco/jacoco.csv
Used by the CI workflow to show coverage on the GitHub Actions run page.
"""
import csv
import sys
from collections import defaultdict


def pct(covered, missed):
    total = covered + missed
    return "-" if total == 0 else f"{100.0 * covered / total:.1f}%"


def main(path):
    totals = defaultdict(lambda: [0, 0, 0, 0])  # line missed, line covered, branch missed, branch covered
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            t = totals[row["PACKAGE"]]
            t[0] += int(row["LINE_MISSED"])
            t[1] += int(row["LINE_COVERED"])
            t[2] += int(row["BRANCH_MISSED"])
            t[3] += int(row["BRANCH_COVERED"])
    print("### Test coverage (JaCoCo)\n")
    print("| Package | Lines | Branches |")
    print("|---|---:|---:|")
    all_t = [0, 0, 0, 0]
    for package in sorted(totals):
        lm, lc, bm, bc = totals[package]
        all_t = [a + b for a, b in zip(all_t, totals[package])]
        name = f"**{package}**" if package.endswith(".service") else package
        print(f"| {name} | {pct(lc, lm)} | {pct(bc, bm)} |")
    print(f"| **Total** | **{pct(all_t[1], all_t[0])}** | **{pct(all_t[3], all_t[2])}** |")


if __name__ == "__main__":
    main(sys.argv[1])
