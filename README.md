<div align="center">

# Android Security Task Manager (STM)

**A Windows Task Manager-style process monitor for Android, with a security-analyst's eye.**

See every running process, its CPU, memory, user and open ports. Flag the suspicious ones. Kill the ones that need to go.

![Platform](https://img.shields.io/badge/platform-Android%208.0%2B-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)
![Root](https://img.shields.io/badge/root-optional%20(full%20mode)-orange)
![Build](https://img.shields.io/github/actions/workflow/status/<your-github-username>/SecurityTaskManager/build.yml?label=build)
![License](https://img.shields.io/badge/license-MIT-blue)

</div>


## What it does

STM reads `/proc` and turns it into a live, sortable process table, the way Windows Task Manager does for a PC.

| Tab | What you see |
|---|---|
| **APPS** | Running apps you installed, plus launcher-visible system apps such as Chrome |
| **BACKGROUND** | Services and daemons: vendor/OEM apps, radio, media and similar |
| **CORE** | Root-owned and critical system processes (`init`, `zygote`, `system_server`, …) |

**Columns:** `NAME · PID · CPU · MEM · USER · PORT · TIME`. Tap a column header to sort, tap a row for details.

### Highlights
- **Live CPU %** calculated from the change between scans (% of one core, like `top`), plus a device-wide CPU and memory strip
- **Ports and connections mapped to the process that owns them** (socket inode → PID)
- **Security findings** that point at processes worth a closer look (see below)
- **Kill that actually sticks:** `am force-stop` for apps, then `kill -9`, then a check that tells you whether the process died, was restarted by Android, or could not be killed
- **Search** by name, package, user, PID or port
- **Pull-to-refresh** and a selectable refresh rate (1 / 3 / 5 seconds). Polling pauses when the app is in the background
- **Export** a snapshot to JSON or CSV through the system file picker, with no storage permission needed
- **Private by design:** no `INTERNET` permission, no analytics, nothing leaves your phone (the only permission is `QUERY_ALL_PACKAGES`, used to match processes to app names)

---

##  Root vs. no root

STM works in two modes. It checks on launch and tells you which one you are in.

| Feature | Without root (limited mode) | With root (full mode) |
|---|:---:|:---:|
| Install and launch the app | ✅ | ✅ |
| Search, sort, settings, About screen | ✅ | ✅ |
| See **STM's own** process | ✅ | ✅ |
| See **every other app's** process (PID, CPU, memory, user) | ❌ ¹ | ✅ |
| **APPS** and **BACKGROUND** tabs fully populated | ⚠️ mostly empty ¹ | ✅ |
| **CORE** tab (system processes) | 🔒 locked | ✅ |
| Open ports and network connections of other processes | ❌ | ✅ |
| Executable path of a process | ❌ | ✅ |
| **Security findings** (writable-path binaries, deleted binaries, exposed listeners) | ❌ | ✅ |
| **Kill / force-stop a process** | ❌ button disabled | ✅ |
| Device CPU / memory strip | ⚠️ depends on Android version ² | ✅ |
| Export a snapshot (JSON / CSV) | ✅ exports what is visible | ✅ full snapshot |

¹ Since Android 10, a normal app can only see its own processes in `/proc`. On Android 8 and 9 you may see a little more.
² Newer Android versions restrict some `/proc` files (such as `/proc/stat`) for non-root apps.

**In short:** without root, STM is a viewer for itself. With root, it is a full process monitor and control tool.

> Root can be provided by Magisk, KernelSU or any `su` implementation that libsu can use. STM asks for it once at launch; deny it and you simply stay in limited mode.

---

##  Security findings (root)

STM runs a few cheap, explainable checks on every process. These are **hints to investigate, not verdicts**: tools such as `frida-server`, Termux and `adb` legitimately trigger them.

| Finding | Severity | Why it is flagged |
|---|:---:|---|
| Runs from a writable location (`/data/local/tmp`, SD card, `/dev/shm`) | 🔴 High | Anything that can write there can plant a binary there |
| Binary deleted while running | 🔴 High | A common way for malware to hide; an in-place update can also cause it |
| Native binary from an app directory (`/data/data/…`) | 🟠 Warn | Normal for Termux, worth checking for anything else |
| Listening on all interfaces (`0.0.0.0` / `::`) | 🟠 Warn | Other devices on the same network can reach it |
| Zombie process |  Info | Exited, but its parent has not collected the exit status |

Flagged processes show a ⚠ in the list and an explanation in the detail sheet.

---

##  Install

1. Open the [**Releases**](../../releases) page.
2. Download **`STM-debug.apk`** and install it.
3. Open STM and grant root when asked, or continue in limited mode.

**Requirements:** Android 8.0 (API 26) or newer. Root is optional but needed for the full feature set.

> Tested on: `<your device / Android version>`

---

##  How it works

```
core/    pure Kotlin, no Android imports, unit-tested on the JVM
         models · /proc parsers · categorizer · security analyzer · snapshot builder · export
data/    Android glue: libsu root shell, PackageManager index, repository
domain/  KillProcess
ui/      Jetpack Compose: theme, shared column spec, table, detail sheet, screens, ViewModel
```

- **One shell call per refresh.** A single script dumps `/proc/*/stat`, UIDs, process names and `/proc/net/*` in one go, instead of hundreds of separate reads.
- **Slow data is cached.** Executable paths and socket owners are refreshed every third scan and on pull-to-refresh.
- **Aligned columns by construction.** The header and every row build their columns from one shared definition (`ProcessColumns.kt`), so they cannot drift apart.
- **Categories:** `CORE` = kernel threads, critical names, UID < 1000; `APP` = user-installed apps and launcher-visible system apps outside a configurable OEM list; `BACKGROUND` = everything else.

### Why a plain `kill -9` is not enough
Android's ActivityManager restarts killed apps that have sticky services. STM runs `am force-stop <package>` first so the whole app is stopped, then `kill -9` for whatever is left, then checks `/proc`. It tells you honestly if the process came back. System daemons supervised by `init` can still respawn.

### Tech stack
Kotlin · Jetpack Compose · Material 3 · [libsu](https://github.com/topjohnwu/libsu) · Gson · GitHub Actions

---

## Build from source

Requires JDK 17 and the Android SDK (API 34).

```bash
git clone https://github.com/<your-github-username>/SecurityTaskManager.git
cd SecurityTaskManager
./gradlew testDebugUnitTest      # run the unit tests
./gradlew assembleDebug          # build the debug APK
```

The APK lands in `app/build/outputs/apk/debug/`. Every push to `main` / `master` also builds the APKs on GitHub Actions and publishes a Release.


---

##  Limitations

- Process names rely on `ps -o NAME` (toybox, Android 8+). Without it STM falls back to the 15-character kernel name.
- CPU % is a measurement between two scans, so the first reading after launch is 0.
- Security findings are heuristics and will have false positives.
- Killing a critical system process can freeze or reboot your device. STM asks for confirmation and refuses `init`, kernel threads and itself.

##  Responsible use

Use STM only on devices you own or are authorised to test. It is an inspection and troubleshooting tool, not an exploitation tool.

##  Contributing

Issues and pull requests are welcome.

## 📄 License

MIT. See [`LICENSE`](LICENSE).

##  Author

Innocent I.
[X / Twitter](https://x.com/1024cyber) · 
[LinkedIn](https://www.linkedin.com/in/issatx)
