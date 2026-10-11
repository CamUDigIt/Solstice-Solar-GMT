# Solstice GMT — Wear OS 4 / Wear OS 5 Watch Face Project

This project contains everything needed to install and run **Solstice GMT** on any Wear OS smartwatch:
- **Google Pixel Watch 1 / 2 / 3**
- **Samsung Galaxy Watch 4 / 5 / 6 / 7 / Ultra**
- **OnePlus Watch 2 / 2R**
- **Xiaomi Watch 2 / Pro**
- Wear OS 4+ Android Studio Emulator

---

## What's Included in this Export:
1. `app/src/main/res/raw/watchface.xml` — Official declarative Watch Face Format (WFF) XML file.
2. `app/src/main/AndroidManifest.xml` — Complete Wear OS watch face wallpaper service manifest.
3. `build.gradle.kts` & `app/build.gradle.kts` — Ready-to-compile Gradle project.
4. `solstice-config.json` — Snapshot of current colorway, airport (LHR - London), and complication layout.
5. `preview-watchface.svg` — High-resolution scalable vector artwork of the watch face.

---

## Quick Method 1: Sideload in 5 Minutes (Wireless ADB)

### Step 1: Enable Developer Options on Watch
1. On your watch, navigate to **Settings** > **System** > **About** > **Versions**.
2. Tap **Build number** 7 times rapidly until you see *"You are now a developer!"*.
3. Go back to **Settings** > **Developer options**.
4. Turn ON **ADB debugging**.
5. Turn ON **Wireless debugging** and tap into it.
6. Make sure your computer and watch are connected to the same Wi-Fi network.

### Step 2: Connect & Install via ADB
Open a terminal in this project root:
```bash
# 1. Pair with your watch (find pairing code & port under 'Pair new device' in watch settings):
adb pair <WATCH_IP>:<PAIRING_PORT> <6_DIGIT_PAIRING_CODE>

# 2. Connect to the watch:
adb connect <WATCH_IP>:<CONNECT_PORT>

# 3. Confirm connected device:
adb devices

# 4. Build and install directly onto your wrist:
./gradlew installDebug
```

---

## Quick Method 2: Samsung Watch Face Studio (No Terminal Needed)

1. Download **Samsung Watch Face Studio (WFS)** (free for macOS and Windows):
   https://developer.samsung.com/watch-face-studio
2. Launch WFS and create a new project.
3. Import or copy the `watchface.xml` file.
4. Click **Run on Device** in the top-right toolbar.
5. Enter your watch's IP address. WFS packages and installs the watch face wirelessly in ~10 seconds!

---

## Active Watch Face Features
- **GMT Airport Hand**: Tracks **LHR** (London, United Kingdom).
- **Astronomical Solar Horizon**: Day bezel (07:16 - 18:20) updates according to airport coordinates.
- **Wear OS Perimeter Arc Complications**:
  - Top-Left: Battery Reserve
  - Top-Right: Weather / Temp
  - Bottom-Left: Steps
  - Bottom-Right: Heart Rate
- **6 o'clock Complication**: Wear OS Native Long Text Slot
