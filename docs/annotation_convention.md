# Annotation Convention

This document defines the annotation naming and hierarchy rules used throughout the workflow.

---

# Required annotation classes

The following annotation classes are required for the main workflow:

| Annotation class | Meaning |
|---|---|
| `PNI-` | PNI-negative region |
| `PNI+: Immune Responsive` | PNI-positive region with immune infiltration |
| `PNI+: Immune Excluded` | PNI-positive region with immune exclusion |

These annotations are typically parent-level ROIs.

---

# Optional helper annotations

| Name / Class | Purpose |
|---|---|
| `Normalization Patch` | Region used for intensity normalization and QC |
| `dark_ref` | Background / dark reference region |
| `bright_ref` | Bright / positive reference region |
| `nerve_regions` | Nerve region annotation |

---

# Hierarchy recommendation

Recommended hierarchy:

```
PNI Annotation
├── Counting Boxes
├── Cell Objects
└── nerve_regions
```

The toolkit includes hierarchy repair utilities inside:

```
scripts/00_setup/
```

---

# Important naming rules

- Annotation class names are case-sensitive.
- `Normalization Patch`, `dark_ref`, and `bright_ref` are matched by annotation name.
- `PNI-`, `PNI+: Immune Responsive`, and `PNI+: Immune Excluded` are matched by annotation class.
- `nerve_regions` is expected as a PathClass name.

---

# Recommended workflow

1. Draw parent PNI annotations.
2. (Optional) Draw normalization / reference annotations.
3. Run cell segmentation.
4. Run cell classification.
5. Organize nerve regions.
6. Export measurements.