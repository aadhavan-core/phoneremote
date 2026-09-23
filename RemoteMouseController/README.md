# Remote Mouse Controller

An Android app (Kotlin + Jetpack Compose) that turns your phone into a
wireless trackpad, air mouse and volume/brightness remote for your PC,
plus a Python host receiver that turns the packets it sends into real
OS mouse/volume/brightness changes.

## What's here

```
app/                    Android app source (Kotlin, Jetpack Compose)
host-receiver/          Python script that runs on your PC
.github/workflows/      CI that builds the APK in the cloud
```

## Opening the app project

1. Install Android Studio (Ladybug/Koala or newer).
2. File > Open > select the `RemoteMouseController` folder.
3. Android Studio will notice there's no `gradlew`/`gradle-wrapper.jar`
   in the repo (see "About the Gradle wrapper" below) and offer to
   generate one — accept that, or run `gradle wrapper --gradle-version 8.9`
   once from a terminal that has Gradle installed.
4. Let Gradle sync, then Run ▶ on a device or emulator.

## How it's wired

- **Landscape lock + forced dark mode** — set in `MainActivity` and
  `RemoteMouseApp` (the `Application` subclass).
- **Left/right click zones** (`TrackpadClickZone`) — tap = click,
  double-tap = double-click, press-and-hold-and-drag = click-and-drag,
  same as a laptop trackpad's corner buttons.
- **Center** — left empty except for the Gyroscopic/Trackpad toggle;
  in Trackpad Mode it doubles as the swipe surface for cursor movement.
- **Far-right sliders** (`VerticalEdgeSlider`) — volume and brightness,
  fade out while you're clicking/dragging elsewhere, and reappear as
  soon as you touch that edge again.
- **Gyroscopic Mode** (`GyroSensorManager`) — reads the gyroscope,
  applies an exponential low-pass filter to cut hand-jitter, and turns
  the smoothed angular velocity into cursor deltas.
- **Trackpad Mode** — raw finger-drag deltas from the center surface.
- **Transports** — `BluetoothTransport` (classic RFCOMM/SPP, the
  default) and `WifiTransport` (UDP, for when Bluetooth isn't
  available). Both share one wire protocol (`PacketProtocol`) and one
  send-loop (`PacketSender`) that coalesces MOVE packets but never
  drops a click.
- **Pairing** — Bluetooth tab (pick from already-paired devices), Wi-Fi
  tab (type an IP/port), or **Scan QR code** (CameraX + ML Kit), which
  reads a `{"host":..,"port":..}` or `{"mac":..}` payload that
  `host-receiver/show_pairing_qr.py` prints for you.

## Why Bluetooth needs a receiver at all

Two ways an Android phone can "be" a Bluetooth mouse:

1. **Bluetooth HID Device Profile** — the phone advertises itself as a
   real Bluetooth mouse peripheral; your PC pairs with it directly and
   needs *no* companion software at all. It's the cleanest UX, but it
   needs the `BluetoothHidDevice` API, isn't supported on every phone's
   Bluetooth chipset/ROM, and Android's public API for it is narrow
   (you can't easily customize click/drag semantics through it).
2. **Classic RFCOMM/SPP socket** (what this app uses) — the phone opens
   a serial-style connection to a small script running on the PC, which
   is the one that actually moves the OS cursor. Slightly more setup
   (you have to run `receiver.py`), but it works on effectively every
   phone and gives full control over gestures, drag-lock, volume and
   brightness — which the spec asked for.

If you later want the zero-receiver HID route, `BluetoothHidDevice` is
the API to look at — it'd replace `BluetoothTransport` and the receiver
script's mouse-handling half, but not the volume/brightness half (HID
mice can't set system volume).

## Running the host receiver

```
cd host-receiver
pip install -r requirements.txt
python receiver.py --bt-port COM5          # Windows Bluetooth, once paired
python receiver.py                          # Wi-Fi (UDP) only
python show_pairing_qr.py                   # optional: print a Wi-Fi pairing QR
```

Getting a Bluetooth COM port on Windows: Settings > Bluetooth & devices
> Devices > More Bluetooth settings > COM Ports tab > Add > Incoming >
select your phone once it's paired. Linux: `sudo rfcomm listen
/dev/rfcomm0 1`. macOS: classic Bluetooth serial is deprecated — use
Wi-Fi there.

## Cloud development — keeping Gradle off your laptop

Three separate things eat disk space in a normal Android setup, and
they're not the same thing:

- The **Gradle project files** themselves (`build.gradle.kts`,
  `settings.gradle.kts`, this whole repo) — a few KB of text. Not the
  problem.
- The **Gradle distribution + dependency cache** (`~/.gradle`) — a few
  hundred MB to a few GB, shared across *every* project on the machine
  by default (it's not per-project).
- The **Android SDK** (`~/Android/Sdk` or similar) — several GB on its
  own, also shared across projects.

Given you don't want any of that sitting on your laptop, the cleanest
option is: **keep the source on GitHub, build with GitHub Actions, and
download only the finished .apk.** That's exactly what
`.github/workflows/build-apk.yml` in this repo does:

1. Push this project to a GitHub repo (the `.gitignore` already keeps
   `build/`, `.gradle/`, and `.idea/` out of it — only ~50 KB of actual
   source and config gets committed).
2. Every push to `main` (or a manual "Run workflow" click) spins up a
   throwaway Ubuntu runner, installs JDK 17 + Gradle 8.9 *on GitHub's
   machine*, builds `assembleDebug`, and uploads the resulting `.apk`
   as a downloadable artifact under the Actions tab.
3. Your laptop never runs Gradle, never downloads the Android SDK, and
   never caches a dependency.

The trade-off: you still need *some* editor to write Kotlin. Two ways
to avoid installing Android Studio locally for that too:

- **GitHub Codespaces / Gitpod** — a browser-based VS Code connected to
  a cloud container. You can edit files and even run `gradle build`
  inside the container's disk, not yours, then let the Actions workflow
  (or the Codespace itself) produce the APK.
- **Just edit in Android Studio, build in CI** — Android Studio itself
  needs local disk for the SDK to give you code completion/lint, but
  you can point `ANDROID_SDK_ROOT` and `GRADLE_USER_HOME` at an
  external drive if that's the constraint, and still let Actions do
  the actual `assembleDebug`/`assembleRelease` build.

Either way, the repo as structured needs no `gradlew` binary and no
local SDK to produce a working `.apk` — CI handles all of it.

## Turning this into a signed release build

`assembleDebug` (what CI builds by default) is auto-signed with the
Gradle debug keystore and installs straight onto your phone — fine for
personal use. For a properly signed release `.apk`/`.aab` later, add a
`signingConfigs { create("release") { ... } }` block to
`app/build.gradle.kts` pointing at a keystore, store its passwords as
GitHub Actions secrets, and switch the workflow's build step to
`gradle assembleRelease`.
