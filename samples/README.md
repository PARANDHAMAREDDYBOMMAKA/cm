# ClaimGuard sample claim documents

Synthetic Indian hospital bills used to demo and smoke-test ClaimGuard's extraction and fraud
pipeline. Nothing here is a real patient, hospital, or claim — every hospital, patient, UHID and
invoice number is fabricated, and the amounts use Indian lakh digit grouping (e.g. `1,40,000`)
because that is the format the extractor is tuned to read.

All files are generated deterministically by `SampleGenerator` — running it again reproduces
byte-identical PDFs and the same file names.

## Regenerating

From the `backend` directory:

```bash
./mvnw -q compile exec:java -Dexec.mainClass=com.claimguard.samples.SampleGenerator
```

`exec:java` also works on its own once the plugin's default `mainClass` picks up
`com.claimguard.samples.SampleGenerator`. By default the files are written to `../samples` (i.e.
this directory); pass a different path as the first argument to write elsewhere, e.g.:

```bash
./mvnw -q compile exec:java -Dexec.mainClass=com.claimguard.samples.SampleGenerator -Dexec.args=/tmp/out
```

## Clean set — should read cleanly and auto-approve

| File | What it is | Expected outcome |
|---|---|---|
| `clean-cataract-bill.pdf` | Single-day cataract surgery (phaco + IOL), Lakeview Eye & General Hospital. Line items sum exactly to the stated total (Rs. 64,200), well under the cataract cost ceiling. | No fraud signals. Risk score 0 (CLEAN). Auto-approved once read. |
| `clean-appendectomy-bill.pdf` | 3-day laparoscopic appendectomy admission, Sunrise Multispeciality Hospital, invoice `SMH/2026/06/0876`. | No fraud signals. Auto-approved. Its invoice number is deliberately reused by `fake-reused-invoice.pdf`. |
| `clean-delivery-bill.pdf` | Normal vaginal delivery, 3-day admission, St. Anne's Maternity & Surgical Centre. | No fraud signals. Auto-approved. |
| `clean-cataract-bill.png` | `clean-cataract-bill.pdf` rasterized to PNG at 150 DPI, so the image-upload extraction/hashing path gets exercised too (not just PDF). | Reads the same as the PDF version. |

## Fakes — each is built to trip a specific detector

Each file targets the signal named below. Some legitimately trip a second signal as well, because a
real forgery rarely fails in only one way — those are noted.

| File | Detector signal | How it's rigged |
|---|---|---|
| `fake-exact-duplicate.pdf` | `EXACT_DUPLICATE` (CRITICAL) | Byte-identical copy of `clean-cataract-bill.pdf` — same SHA-256. Upload it on a *different* claim than the cataract bill. Being an exact copy it carries the same invoice number too, so `REUSED_INVOICE_NUMBER` (HIGH) normally fires alongside it, taking the score to the 100 cap. |
| `fake-near-duplicate.png` | `NEAR_DUPLICATE_IMAGE` (HIGH) | `clean-cataract-bill.pdf` re-rendered to PNG at 132 DPI instead of 150 DPI. Different bytes, but the perceptual (difference) hash lands within Hamming distance 6 of `clean-cataract-bill.png` (the generator prints the actual distance when it runs — currently 1). |
| `fake-reused-invoice.pdf` | `REUSED_INVOICE_NUMBER` (HIGH) | A fracture bill for a different patient at a different hospital, but stamped with invoice number `SMH/2026/06/0876` — the same one already on `clean-appendectomy-bill.pdf`. |
| `fake-inflated-total.pdf` | `LINE_ITEM_MISMATCH` (MEDIUM) | Gallstone/cholecystectomy bill whose line items add up to Rs. 53,000, but the printed total says Rs. 1,40,000 — well past the 2% drift tolerance. |
| `fake-amount-out-of-band.pdf` | `AMOUNT_OUT_OF_BAND` (HIGH) | Cataract bill totalling Rs. 4,20,000 (7x `ClinicalReference`'s Rs. 60,000 ceiling for cataract, itself already past the detector's 1.5x allowance). |
| `fake-procedure-mismatch.pdf` | `PROCEDURE_DIAGNOSIS_MISMATCH` (MEDIUM) | Diagnosis is "Cataract", but every line item (ORIF plating, intramedullary nail, ward stay) is orthopaedic — none of it matches the cataract treatment vocabulary (phaco/IOL/lens/cataract). |
| `fake-date-inconsistency.pdf` | `DATE_INCONSISTENCY` (MEDIUM) | Appendicitis bill with a discharge date (22 Jul 2026) before the admission date (25 Jul 2026). |
| `fake-edited-in-acrobat.pdf` | `EDITING_SOFTWARE` (HIGH) + `MODIFIED_AFTER_CREATION` (MEDIUM) | Otherwise-clean cataract bill whose PDF Producer/Creator is set to "Adobe Acrobat Pro DC" and whose modification date is 5 days after its creation date. |

Every fake bill besides the two duplicate files carries its own unique invoice number, patient,
and metadata (matching creation/modification timestamps, a non-editor Producer) so that each one
trips only the signal it's named for — the demo can point at one fraud reason at a time.

## Notes on the data

- All patient names, UHIDs, GSTINs and hospitals are invented; none refer to real people or
  organisations.
- Diagnoses and treatment line items are drawn from the vocabulary in
  `backend/src/main/java/com/claimguard/fraud/ClinicalReference.java` so the
  `PROCEDURE_DIAGNOSIS_MISMATCH` and `AMOUNT_OUT_OF_BAND` checks behave the same way they would
  against a real claim.
- Every date in the set falls before the file's generation date so nothing accidentally trips the
  "date is in the future" consistency check.
