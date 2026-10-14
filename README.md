# Solstice GMT

A luxury tool-watch style Wear OS watch face built with **Watch Face Format v3**
(Wear OS 5.1+, API 35). It's a resource-only APK with no app code.

## What's on the face

- **Solar bezel.** Red/blue shows day and night at the destination, always
  drawn on a 24-hour scale (midnight at the top) so the bands show the real
  day and night lengths, and sunrise/sunset sit in the same place whichever
  labels are chosen: an 18:15 sunset is at 9:15 on the 12-hour labels and just
  past 18 on the 24-hour ones. Sunrise and sunset are computed on the watch
  from the date and the airport's latitude/longitude (NOAA approximation,
  within a few minutes). The 40-unit band has numerals scaled to match.
  - *12-hour:* standard watch labels (12 at the top, 6 at the bottom).
  - *24-hour:* 24-hour labels.
  - On both faces the GMT hand turns once a day, so it always points at the
    destination's current time on the band (18:15 at the 9:15 position of
    the 12-hour labels), and its colour underneath is day or night there.
  - In always-on mode the bands stay at the same brightness as the GMT hand
    (about 78%); bright day colours use darker always-on shades so the face stays
    under the 15% always-on brightness limit (all colourways estimated at
    11.5% or less, even with day covering 80% of the ring).
- **GMT hand.** It shows the time at one of 49 airports. The editor lists LOCAL
  first, then groups cities by continent (North America, South
  America, Europe, Africa, Asia including the Middle East, Oceania), A–Z by
  city name, each shown with its standard UTC offset (`Denver · DEN · UTC−7`).
  Each airport's IANA time zone is
  applied with WFF `Localization`, so daylight saving and half-hour offsets are
  handled by the OS. Add or remove airports in the `AIRPORTS` list in
  `tools/WatchFace.java`.
- **Offset label.** For example `LHR +1H`, `HNL −10H` or `KTM +5:45H`: the
  airport's current offset from UTC, including daylight saving. The label
  computes it from the UTC clock and the airport's daylight-saving rule.
  `tools/DstCheck.java` verifies those rules against `java.time` for every hour through 2040.
- **LOCAL.** The first airport option. The GMT hand follows local time, and
  because WFF has no location source, the bezel's daylight comes from the
  watch's hourly weather forecast, with each day/night change drawn halfway
  between forecast hours (within about 30 minutes). With no forecast, or one
  that says "day" for all 24 hours (the watch-face editor's preview data), it
  shows a nominal 06:00–18:00 day so both colours still appear. The label
  shows the local zone's abbreviation and offset from UTC (`MDT −6H`,
  `BST +1H`, `NPT +5:45H`). WFF's `[TIMEZONE_ABB]` only reports the standard
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
  long-text slot at 6 o'clock, drawn straight on the dial (no box). It shows
  text over title; text like "Daily Planning · Wed 7:00 AM" is split at the
  separator into name and time on two lines; other text
  wraps onto two lines.
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
  Flat), the bezel format (12h or 24h), one of 26 drink-themed bezel colourways (colas and their diet versions,
  citrus, fruit, root beer and ginger, plus softer earthy and pastel teas,
  lattes and cream soda; see `COLORWAYS` in the generator),
  the GMT airport, and each complication.

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
