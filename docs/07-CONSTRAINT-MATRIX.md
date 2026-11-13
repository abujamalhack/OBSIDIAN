# Constraint matrix

| Constraint | 10 | 11 | 12 | 13 | 14 | 15 |
|---|---|---|---|---|---|---|
| Background FGS start | OK | OK | blocked | blocked | blocked | blocked |
| FGS type mandatory | no | no | no | no | yes | yes |
| `CAMERA_DISABLED` without camera FGS | no | no | no | no | yes | yes |
| DO silent runtime grant | yes | yes | yes | yes | yes | yes |
| DO covers appops | no | no | no | no | no | no |
| MediaProjection needs tap | yes | yes | yes | yes | yes | yes |
| One-time permission auto-revoke | n/a | yes | yes | yes | yes | yes |
| Exact alarm without `USE_EXACT_ALARM` | OK | OK | OK | needs user | needs user | needs user |

`Ladder` covers the API 31 background-start block. `CaptureService` sets the
FGS type before binding. `provision.sh` sets `USE_EXACT_ALARM` and the doze
whitelist.

## OEM killers

| OEM | Killer | Survives with |
|---|---|---|
| AOSP/Pixel | Doze, standby buckets | doze exemption — reliable |
| Samsung | Sleeping apps, Knox auto-stop | manual exclusion |
| Xiaomi | Autostart, battery saver | manual Autostart + No restrictions |
| Huawei/Honor | PowerGenie | Protected apps |
| Oppo/Realme/Vivo/OnePlus | deep sleep | Allow background activity |
| Android Go | low-RAM trim | nothing reliable |

DO cannot flip any OEM-private store. Only root is reliable there.
