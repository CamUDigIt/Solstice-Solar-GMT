# Solstice GMT

A luxury tool-watch style Wear OS watch face built with **Watch Face Format v3**
(Wear OS 5.1+, API 35). It's a resource-only APK with no app code.

## What's on the face

- **Solar bezel, 12h or 24h** (a "Bezel" setting). Sunrise and sunset are
  computed on the watch from the date and the airport's latitude/longitude
  (NOAA approximation, within a few minutes).
  - *12-hour:* a 12-hour window centred on the red GMT hand (six hours
    behind, six ahead), so sunrise and sunset in that span sit at their real
    clock positions. The two ends of the window meet opposite the hand, marked
    by a thin dark seam.
  - *24-hour:* a fixed day scale. The GMT hand makes one turn a day and the
    day colour runs from sunrise to sunset.
- **GMT hand.** It shows the time at one of 50 airports. The editor lists LOCAL
  and UTC first, then groups cities by continent (Americas, Europe, Africa,
  Middle East, Asia, Oceania), A–Z by city name. Each airport's IANA time zone is
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
  when there's no weather data). The label shows the local zone's abbreviation
  (MDT, BST, CEST, NPT…). WFF's `[TIMEZONE_ABB]` only reports the standard
  name, so summer names are derived from it; zones without a named
  abbreviation show `UTC +13:45` style instead.
- **Two styles.**
  - *Pilot* (inspired by classic GMT tool watches): bundled Oswald, Mercedes
    hour hand, sword minute hand, lollipop seconds, a GMT hand with a large
    triangle, small lume plots with a triangle at 12, and a Cyclops-style
    magnified date. Its 24h bezel has even numerals with triangles between.
  - *Flat* (inspired by flat smartwatch GMT faces): bundled Inter (a wider, rounder sans),
    flat white stick hands with a thin neck, long lumed hour ticks and flat
    complication panels. Its 24h bezel is numbered every second hour.
  - Both fonts are SIL OFL 1.1 (licences in `licenses/`, shipped in the APK
    under `assets/licenses/`). Text points straight at each weight's TTF
    (e.g. `oswald_600`): an XML font-family resource hangs the watch-face
    runtime on a Pixel Watch 4 even though the emulator renders it.
  - All hands have a soft dark edge.
- **Readability.** Text meets Wear OS guideline WO-V14 (12sp essential, 10sp
  secondary) on watches as small as 384 px at 320 dpi, and every tap area is
  at least 48dp there (112 face units). The GMT label is printed just below
  12, clear of where the hands usually sit.
- **Complications.** A round slot at 9 o'clock (ranged value, goal progress,
  short text or icon, with a progress ring in the bezel's day colour) and a
  long-text slot at 6 o'clock. The long-text slot shows text over title, or
  wraps the text onto two lines when the provider has no title (for example
  calendar events like "Team Standup · 3:00 PM").
- **Date** at 3 o'clock: a Cyclops-style window on Pilot, plain white
  gold numerals on Flat (#E3B341, an Apple-style date accent).
- **Tap actions.** Two invisible complication slots sit over the date and the
  GMT label. They draw nothing; whatever provider you choose for them in the
  editor's complications page ("Date tap action", "GMT label tap action")
  decides what a tap opens. Choose **App shortcut** to open any app.
  Defaults: the date slot is Empty, so taps fall through to the date's own
  `Launch target="CALENDAR"` (the system calendar app); the label opens World
  clock.
- **Always-on mode.** Outline hands, thin bezel, no seconds, and no dial fill.
- **Customisation.** In the watch-face editor you can pick the style (Pilot or
  Flat), the bezel format (12h or 24h), a bezel colourway (Pepsi, Batman, Coke,
  Root Beer, Golden Hour, Polar), the GMT airport, and each complication.

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
