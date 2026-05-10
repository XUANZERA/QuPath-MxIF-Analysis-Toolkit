# Channel Convention

This document defines the default image channel names used by the workflow.

---

# Recommended channel names

| Biological target | Default channel |
|---|---|
| Nuclei | `DAPI` or `DAPI (C1)` |
| Immune marker | `Opal 620` |
| Tumor marker | `Opal 780 (C6)` |
| Nerve marker | `Opal 480` |

---

# Important notes

- Channel names may differ between datasets.
- Modify the configuration block at the top of each script if needed.
- The workflow assumes calibrated MxIF whole-slide images.

---

# Example

```groovy
def measurement_620 = "Cell: Opal 620 mean"
def measurement_780 = "Cell: Opal 780 (C6) mean"
```

---

# Recommendation

Always verify:

- channel ordering
- channel naming
- intensity scaling
- display range

before running batch analysis.