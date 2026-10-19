# Security Task Manager (STM)

A Windows Task Manager-style process monitor for Android, built with **Kotlin + Jetpack Compose**.

| Tab | Shows |
|---|---|
| **APPS** | Installed apps that are running (user apps, plus launcher-visible system apps like Chrome) |
| **BACKGROUND** | Services and daemons (OEM/vendor apps, radio, media, ...) |
| **CORE** | root-owned and critical system processes (`init`, `zygote`, `system_server`, ...). **Root only.** |

Columns: **NAME · PID · CPU · MEM · USER · PORT · TIME**. Tap a row for the detail sheet
(all fields, executable path, open sockets, security findings, kill).

## Features
- Root detection on the splash screen: **full mode** (root) or **limited mode** (Android only shows a non-root app its own processes)
- Real CPU % from the change between two scans (% of one core, like `top`), plus a device CPU/MEM strip
- Ports and connections mapped to processes (socket inode to PID)
- **Security findings** (heuristic hints, not verdicts): binary running from `/data/local/tmp` or SD card, deleted-but-running binaries, native binaries in app dirs, listening on all interfaces, zombies
- **Kill**: `am force-stop` for apps (so Android doesn't instantly restart them), then `kill -9`, then verification, reported as killed / restarted by Android / failed
- Pull-to-refresh, search, sortable columns, 1/3/5 s polling that pauses when the app is in the background
- Export a snapshot to JSON or CSV via the system file picker (no storage permission needed)

## Architecture
```
core/    pure Kotlin, no Android imports, unit-tested on the JVM
         models, /proc parsers, categorizer, security analyzer, SnapshotBuilder, filter/sort, export formats
data/    Android glue: libsu root shell, PackageManager index, repository (one batched shell call per refresh)
domain/  KillProcess
ui/      Compose: theme, shared column spec (ProcessColumns), table, detail sheet, screens, ViewModel
```
Each refresh is **one** root-shell invocation (`ScanScript`) that dumps `/proc/*/stat`, uids, `ps` names and
`/proc/net/*`. The slower sections (executables, sockets) run every 3rd refresh and on pull-to-refresh.

Header and rows share `ui/components/ProcessColumns.kt`, so the column layout cannot drift apart.

## Build
CI (`.github/workflows/build.yml`) runs the unit tests, builds debug + release APKs and publishes a GitHub Release on every push to `main`/`master`.

Locally: open in Android Studio, or `./gradlew testDebugUnitTest assembleDebug` (JDK 17, Android SDK 34).


## Notes and limits
- Needs `ps` with `-o NAME` support (toybox, Android 8+). Without it, names fall back to the 15-character kernel name.
- Root daemons such as `init` restart their children, so killing a native service can bring it right back; STM tells you when that happens.
