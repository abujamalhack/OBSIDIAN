# Vulnerability methodology

## Classes

| Class | Test | Evidence |
|---|---|---|
| `MISCONFIG` | Present only because of this device's state | one `dumpsys` line + the setting |
| `DESIGN-WEAKNESS` | In platform/app code, reproducible on a clean image | two clean-image repros |
| `0-DAY-CANDIDATE` | Design weakness + absent from NVD/OSV + reachable unprivileged + working PoC | PoC + root cause + dedup result |

## Steps

1. Enumerate surface: exported components, `grantUriPermissions` providers,
   mutable PendingIntents, `taskAffinity`, `addJavascriptInterface`.
2. Capture `baseline.json` on a freshly flashed image of the same model.
3. Diff live against baseline.
4. Triage reachability: unprivileged app → browser `intent://` → ADB.
5. Build the minimal PoC and record it verbatim.
6. Root-cause with jadx; cite the method and the missing check.
7. Dedup: `https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch=`
   and OSV. Empty result is required before labelling.
8. Score CVSS 3.1 with a vector string.
9. Report with reproducible steps.

`Exploit.classify` implements step 1–3 automatically and emits the step-7 URL.
It does not auto-confirm; steps 4–6 are manual by design.

## What `Exploit.scan` will not tell you

It cannot distinguish a genuinely new bug from a known one without the dedup
query being read. `exported=true` without a permission check is a **lead**.
