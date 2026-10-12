# Solstice GMT

A luxury tool-watch style Wear OS watch face built with **Watch Face Format v3**
(Wear OS 5.1+, API 35). It's a resource-only APK with no app code.

## What's on the face

- **12-hour solar bezel.** The two-tone ring is a rolling 12-hour daylight
  window for the selected airport, read against the red GMT hand. The hand
  always sits on "now" at the airport. Going clockwise from it, the ring shows
  the day colour for the hours that will be daylight there and the night colour
  for the rest. Sunrise and sunset are computed on the watch from the date and
  the airport's latitude/longitude (NOAA approximation, within a few minutes).
- **GMT hand.** It shows the time at one of 50 airports, listed west to east in
  the editor (UTC first, then HNL to AKL). Each airport's IANA time zone is
  applied with WFF `Localization`, so daylight saving and half-hour offsets are
  handled by the OS. Add or remove airports in the `AIRPORTS` list in
  `tools/WatchFace.java`.
- **Offset label.** For example `LHR +7H`, `HNL −4H` or `KTM +11:45H`: how far
  the airport is ahead of or behind local time. Text inside a localised part
  can't see the wearer's offset, so the label computes the airport's offset
  from the UTC clock and its daylight-saving rule. `tools/DstCheck.java` verifies
  those rules against `java.time` for every hour through 2040.
- **LOCAL.** The first airport option. The GMT hand follows local time, and
  because WFF has no location source, the bezel's daylight comes from the
  watch's hourly weather forecast (it snaps to whole hours and shows night only
  when there's no weather data).
- **Local time.** Steel sword hands with lume, and a sweeping lollipop seconds hand.
- **Complications.** Four arc complications (ranged value / goal progress /
  short text) and a long-text slot at 6 o'clock.
- **Date window** at 3 o'clock.
- **Always-on mode.** Outline hands, thin bezel, no seconds, and no dial fill.
- **Customisation.** In the watch-face editor you can pick a bezel colourway
  (Pepsi, Batman, Coke, Root Beer, Golden Hour, Polar), the GMT airport, and
  each complication.

## Project layout

| Path | What it is |
|---|---|
| `tools/WatchFace.java` | **Source of truth.** Generates `res/raw/watchface.xml`, `res/values/strings.xml` and the editor icons. |
| `tools/HandArt.java` | Renders the hand / bezel-pip PNGs into `res/drawable-nodpi`. |
| `tools/Validate.java` | Validates the XML against the official WFF v3 schema (`tools/wff`, Apache 2.0, from google/watchface). |
| `tools/DstCheck.java` | Checks the offset label's daylight-saving rules against `java.time`. Run it after changing airports. |
| `tools/preview.sh` | Regenerates, validates, builds, installs on a device and saves a screenshot. |
| `app/src/main/res/raw/watchface.xml` | Generated. Don't hand-edit. |

## Edit → validate → build

Run these from the project root (JDK 17+):

```bash
java tools/HandArt.java
```

```bash
java tools/WatchFace.java
```

```bash
java -cp "tools/wff/lib/*" tools/Validate.java
```

```bash
javac -d tools/out/cls tools/WatchFace.java tools/DstCheck.java && java -cp tools/out/cls DstCheck
```

```bash
./gradlew assembleDebug
```

## Install on a watch or emulator (Wear OS 5.1+)

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Then long-press the current watch face, scroll to **Solstice GMT**, and select it.
If the XML has a runtime error, the face renders blank; check the log with:

```bash
adb logcat -s WatchFace
```
