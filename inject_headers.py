from __future__ import annotations

import argparse
import re
from pathlib import Path
from typing import Dict


# ============================================================
# Header Injection Script for QuPath-MxIF-Analysis-Toolkit
# ============================================================
#
# What it does:
#   1. Recursively scans .groovy files under scripts/
#   2. Removes old top-of-file block headers of the form /** ... */
#   3. Inserts standardized new headers
#   4. Keeps imports and all remaining code unchanged
#
# Safe behavior:
#   - Only removes a block comment if it is at the very beginning of the file,
#     ignoring UTF-8 BOM and leading whitespace.
#   - Does NOT remove block comments in the middle of scripts.
#   - Can run in --dry-run mode before writing.
#
# Usage:
#   python inject_headers.py --root "D:\code\QuPath-MxIF-Analysis-Toolkit"
#   python inject_headers.py --root "D:\code\QuPath-MxIF-Analysis-Toolkit" --dry-run
# ============================================================


HEADERS: Dict[str, str] = {
    "scripts/00_setup/00_sample_counting_boxes.groovy": """
/**
 * Script:
 *   00_sample_counting_boxes.groovy
 *
 * Purpose:
 *   Randomly sample fixed-size counting boxes inside selected annotations.
 *
 * Workflow position:
 *   Step 0 — Setup / ROI preparation
 *
 * Input:
 *   - Selected annotation objects
 *   - Calibrated pixel size
 *
 * Output:
 *   - Child annotation objects named "Counting Box X"
 *
 * Recommended usage:
 *   Use for normalization patches or ROI sampling before downstream analysis.
 *
 * Notes:
 *   Existing counting boxes under the selected annotation will be removed.
 *   Visual QC is recommended after sampling.
 */
""".strip(),

    "scripts/00_setup/01_merge_counting_boxes_to_parent.groovy": """
/**
 * Script:
 *   01_merge_counting_boxes_to_parent.groovy
 *
 * Purpose:
 *   Merge all counting-box annotations into a target parent annotation hierarchy.
 *
 * Workflow position:
 *   Step 0 — Setup / hierarchy organization
 *
 * Input:
 *   - Annotation named "Normalization Patch"
 *   - Child annotations starting with "Counting Box"
 *
 * Output:
 *   - Updated annotation hierarchy
 *
 * Recommended usage:
 *   Run after counting-box generation if hierarchy organization is incorrect.
 *
 * Notes:
 *   This script modifies QuPath hierarchy relationships.
 *   Save the project after execution.
 */
""".strip(),

    "scripts/00_setup/03_move_nerve_regions_to_parent_annotation.groovy": """
/**
 * Script:
 *   03_move_nerve_regions_to_parent_annotation.groovy
 *
 * Purpose:
 *   Move annotations classified as "nerve_regions" under a selected parent annotation.
 *
 * Workflow position:
 *   Step 0 / Step 4 — Hierarchy organization
 *
 * Input:
 *   - Selected parent annotation
 *   - Annotation objects classified as "nerve_regions"
 *
 * Output:
 *   - Updated parent-child annotation hierarchy
 *
 * Recommended usage:
 *   Use after nerve-region segmentation or manual annotation cleanup.
 *
 * Notes:
 *   Existing hierarchy relationships may be modified.
 *   Visual inspection in the Hierarchy panel is recommended.
 */
""".strip(),

    "scripts/00_setup/99_debug_delete_cells_in_selected_annotation.groovy": """
/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_cells_in_selected_annotation.groovy
 *
 * Purpose:
 *   Delete detection objects inside the selected annotation.
 *
 * Input:
 *   - Selected annotation object
 *
 * Output:
 *   - Detection objects removed from hierarchy
 *
 * Notes:
 *   This operation cannot be easily undone.
 *   Save backups before use.
 */
""".strip(),

    "scripts/00_setup/99_debug_delete_counting_boxes.groovy": """
/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_counting_boxes.groovy
 *
 * Purpose:
 *   Delete counting-box annotations under the selected parent annotation.
 *
 * Input:
 *   - Selected parent annotation
 *
 * Output:
 *   - Counting-box child annotations removed
 *
 * Notes:
 *   Uses regex matching for "Counting Box [1-8]".
 *   Visual confirmation is recommended before saving.
 */
""".strip(),

    "scripts/01_normalization/00_estimate_display_anchors_from_reference_patches.groovy": """
/**
 * Script:
 *   00_estimate_display_anchors_from_reference_patches.groovy
 *
 * Purpose:
 *   Estimate display normalization anchors from reference annotation patches.
 *
 * Workflow position:
 *   Step 1 — Normalization / threshold estimation
 *
 * Input:
 *   - Project images
 *   - Annotations named "Normalization Patch"
 *   - Target image channels
 *
 * Output:
 *   - Percentile-based display anchor estimates
 *   - Suggested normalization ranges
 *
 * Recommended usage:
 *   Run before segmentation and classification for intensity standardization.
 *
 * Notes:
 *   Requires calibrated MxIF images.
 *   Channel names may need adjustment for different datasets.
 */
""".strip(),

    "scripts/02_cell_segmentation/00_segment_cells_selected_annotations.groovy": """
/**
 * Script:
 *   00_segment_cells_selected_annotations.groovy
 *
 * Purpose:
 *   Segment DAPI-positive nuclei/cells inside selected annotations.
 *
 * Workflow position:
 *   Step 2 — Cell segmentation
 *
 * Input:
 *   - Selected annotation objects
 *   - DAPI image channel
 *
 * Output:
 *   - Cell / detection objects
 *   - Intensity measurements generated by WatershedCellDetection
 *
 * Recommended usage:
 *   First use on selected annotations for parameter tuning before batch analysis.
 *
 * Notes:
 *   Existing child objects inside selected annotations will be cleared.
 *   Visual QC is strongly recommended.
 */
""".strip(),

    "scripts/03_cell_classification/01_classify_cd3_cells_selected_annotation.groovy": """
/**
 * Script:
 *   01_classify_cd3_cells_selected_annotation.groovy
 *
 * Purpose:
 *   Classify immune cells using CD3 marker intensity inside the selected annotation.
 *
 * Workflow position:
 *   Step 3 — Cell classification
 *
 * Input:
 *   - Selected annotation object
 *   - Cell measurement: "Cell: Opal 620 mean"
 *
 * Output:
 *   - Cell classification labels such as "immune_cell"
 *
 * Recommended usage:
 *   Run after cell segmentation and verify classification visually.
 *
 * Notes:
 *   Thresholds are dataset-dependent and may require tuning.
 *   Channel display ranges are synchronized for visualization consistency.
 */
""".strip(),

    "scripts/03_cell_classification/99_debug_delete_immune_classification.groovy": """
/**
 * Debug utility.
 *
 * Script:
 *   99_debug_delete_immune_classification.groovy
 *
 * Purpose:
 *   Delete objects classified as "immune_cell" under the selected annotation.
 *
 * Input:
 *   - Selected parent annotation
 *
 * Output:
 *   - immune_cell objects removed
 *
 * Notes:
 *   This operation modifies the hierarchy permanently unless reverted manually.
 */
""".strip(),

    "scripts/04_nerve_region/00_segment_nerve_regions_selected_annotation_experimental.groovy": """
/**
 * Experimental module.
 *
 * Script:
 *   00_segment_nerve_regions_selected_annotation_experimental.groovy
 *
 * Purpose:
 *   Perform semi-automatic nerve-region segmentation using Otsu thresholding.
 *
 * Workflow position:
 *   Step 4 — Nerve region segmentation
 *
 * Input:
 *   - Selected annotation object
 *   - Target nerve-marker channel
 *
 * Output:
 *   - Annotation objects classified as "nerve_regions"
 *
 * Recommended usage:
 *   Use as a helper tool followed by manual visual inspection.
 *
 * Notes:
 *   This module is experimental and may require dataset-specific adjustment.
 *   Segmentation quality should always be manually verified.
 */
""".strip(),

    "scripts/04_nerve_region/01_move_nerve_regions_to_parent_annotation.groovy": """
/**
 * Script:
 *   01_move_nerve_regions_to_parent_annotation.groovy
 *
 * Purpose:
 *   Organize nerve-region annotations under a selected parent annotation.
 *
 * Workflow position:
 *   Step 4 — Nerve region organization
 *
 * Input:
 *   - Selected parent annotation
 *   - Annotation objects classified as "nerve_regions"
 *
 * Output:
 *   - Updated annotation hierarchy
 *
 * Recommended usage:
 *   Use after nerve segmentation or manual annotation editing.
 *
 * Notes:
 *   Existing hierarchy relationships will be modified.
 *   Verify results in the Hierarchy panel.
 */
""".strip(),

    "scripts/06_export/02_export_project_cell_measurements_batch.groovy": """
/**
 * Script:
 *   02_export_project_cell_measurements_batch.groovy
 *
 * Purpose:
 *   Export project-wide cell measurements into a combined CSV file.
 *
 * Workflow position:
 *   Step 6 — Export
 *
 * Input:
 *   - QuPath project
 *   - Cell / detection objects
 *
 * Output:
 *   - combined_cell_measurements.csv
 *
 * Recommended usage:
 *   Run after segmentation, classification, and spatial measurement steps.
 *
 * Notes:
 *   Missing distance measurements will be computed automatically if possible.
 *   Exported files are intended for downstream Python / R analysis.
 */
""".strip(),

    "scripts/experimental/experimental_batch_cell_classification.groovy": """
/**
 * Experimental module.
 *
 * Script:
 *   experimental_batch_cell_classification.groovy
 *
 * Purpose:
 *   Perform batch immune-cell classification across all project images.
 *
 * Workflow position:
 *   Step 3 — Batch cell classification
 *
 * Input:
 *   - Project images
 *   - Annotations named "Normalization Patch"
 *   - Cell intensity measurements
 *
 * Output:
 *   - Updated immune-cell classifications
 *
 * Recommended usage:
 *   Verify thresholds on selected annotations before running batch analysis.
 *
 * Notes:
 *   Thresholds and channel naming are dataset-dependent.
 *   Visual QC is strongly recommended after execution.
 */
""".strip(),

    "scripts/experimental/experimental_delete_immune_classification.groovy": """
/**
 * Experimental utility.
 *
 * Script:
 *   experimental_delete_immune_classification.groovy
 *
 * Purpose:
 *   Remove immune-cell classification objects from the selected annotation.
 *
 * Input:
 *   - Selected annotation object
 *
 * Output:
 *   - immune_cell objects deleted
 *
 * Notes:
 *   Intended for experimental cleanup and debugging workflows.
 */
""".strip(),
}


TOP_HEADER_RE = re.compile(
    r"^\ufeff?\s*/\*\*.*?\*/\s*",
    re.DOTALL,
)


def normalize_rel(path: Path, root: Path) -> str:
    return path.relative_to(root).as_posix()


def remove_old_top_header(text: str) -> str:
    return TOP_HEADER_RE.sub("", text, count=1).lstrip()


def inject_header(path: Path, root: Path, dry_run: bool = False) -> str:
    rel = normalize_rel(path, root)

    if rel not in HEADERS:
        return f"SKIP    {rel}  (no header mapping)"

    old_text = path.read_text(encoding="utf-8")
    body = remove_old_top_header(old_text)
    new_text = HEADERS[rel] + "\n\n" + body

    if old_text == new_text:
        return f"OK      {rel}  (unchanged)"

    if not dry_run:
        path.write_text(new_text, encoding="utf-8", newline="\n")

    return f"UPDATE  {rel}"


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Clean old top-of-file Groovy headers and inject standardized headers."
    )
    parser.add_argument(
        "--root",
        required=True,
        help="Repository root path, e.g. D:\\code\\QuPath-MxIF-Analysis-Toolkit",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Preview changes without writing files.",
    )

    args = parser.parse_args()
    root = Path(args.root).resolve()
    scripts_dir = root / "scripts"

    if not root.exists():
        raise FileNotFoundError(f"Repository root does not exist: {root}")
    if not scripts_dir.exists():
        raise FileNotFoundError(f"scripts/ directory does not exist: {scripts_dir}")

    print(f"Repository root: {root}")
    print(f"Dry run: {args.dry_run}")
    print("=" * 72)

    groovy_files = sorted(scripts_dir.rglob("*.groovy"))

    updated = 0
    skipped = 0

    for path in groovy_files:
        result = inject_header(path, root, dry_run=args.dry_run)
        print(result)

        if result.startswith("UPDATE"):
            updated += 1
        elif result.startswith("SKIP"):
            skipped += 1

    print("=" * 72)
    print(f"Total Groovy files scanned: {len(groovy_files)}")
    print(f"Updated files: {updated}")
    print(f"Skipped files: {skipped}")

    if args.dry_run:
        print("\nDry-run only. No files were modified.")


if __name__ == "__main__":
    main()
