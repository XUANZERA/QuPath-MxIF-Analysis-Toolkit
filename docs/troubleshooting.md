# Troubleshooting

## No cells detected

Possible causes:

- incorrect DAPI channel name
- threshold too high
- annotation not selected
- image not calibrated

---

## All cells classified as positive

Possible causes:

- threshold too low
- incorrect channel measurement name
- autofluorescence contamination

---

## Nerve regions missing

Possible causes:

- nerve_regions class not assigned
- hierarchy broken
- annotation naming mismatch

---

## Batch scripts do not process annotations

Check:

- annotation class spelling
- selected annotations
- QuPath version compatibility

---

## Recommended workflow

Always:

1. test on selected annotations first
2. visually inspect results
3. only then run batch scripts