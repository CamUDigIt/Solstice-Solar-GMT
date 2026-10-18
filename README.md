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
  - In always-on mode the band narrows to a thin 8-unit ring just inside the
    numerals, at the same brightness as the GMT hand (about 78%), with darker
    always-on shades of the day colours. Measured on a Pixel Watch 4, the
    always-on face averages about 4% brightness with about 15% of pixels lit.
    Even if the full band were shown all in day colour, every colourway would
    stay at or under 12.8% average brightness (limit 15%).
- **GMT hand.** It shows the time at one of 90 airports, one per metro area
  (the busiest where a city has several). The editor groups them by region, in
  columns from west to east and north to south within each column: North
  America, Latin America & Caribbean, Europe, Africa, Middle East & Central
  Asia, Asia, Oceania & Pacific; within each region, west to east by standard
  offset and then A–Z. Each entry has two lines: a two-letter region prefix
  and the city, then its code and standard UTC offset (`NA · Denver` over
  `DEN · UTC−7`). Each airport's IANA time zone is
  applied with WFF `Localization`, so daylight saving and half-hour offsets are
  handled by the OS. Add or remove airports in the `AIRPORTS` list and
  `REGIONS` in `tools/WatchFace.java`. Any IANA zone works, including ones
  with irregular daylight-saving rules (e.g. Morocco, Israel).
- **Sunrise and sunset** are computed on the watch for the selected airport
  from its latitude/longitude and the date (NOAA approximation, within a few
  minutes). There is no "local time" option: WFF has no location source, so
  pick your home airport to follow local time.
- **Offset label.** For example `LHR +1H`, `HNL −10H` or `KTM +5:45H`: the
  airport's current offset from UTC, including daylight saving. The label
  is localised to the airport's time zone and reads the offset the OS keeps
  for it, so it needs no daylight-saving maths and only changes when the
  offset does.
- **Three styles.**
  - *Pilot* (inspired by classic GMT tool watches): bundled Oswald, Mercedes
    hour hand, sword minute hand, lollipop seconds, a GMT hand with a large
    triangle, small lume plots with a triangle at 12, and a Cyclops-style
    magnified date. Its 24h bezel has even numerals with triangles between.
  - *Flat* (inspired by flat smartwatch GMT faces): bundled Inter (a wider, rounder sans),
    flat white stick hands with a thin neck, long lumed hour ticks and flat
    complication panels. Its 24h bezel is numbered every second hour.
  - *Pixel* (inspired by Pixel Watch faces): bundled Google Sans Flex, solid
    white capsule hands; the second hand, a shorter GMT hand ending in a dot
    and the label underline take the colourway's day colour as an accent,
    and the centre dot its night colour. Round hour dots (larger at 12, 6 and 9) with
    no minute track, a tonal filled complication disc, the date in a pill
    tinted with the day colour, and a dark gap between the dial and the bezel.
    Its 24h bezel matches Flat's.
  - All three fonts are SIL OFL 1.1 (licences in `licenses/`, shipped in the APK
    under `assets/licenses/`). Text points straight at each weight's TTF
    (e.g. `oswald_600`): an XML font-family resource hangs the watch-face
    runtime on a Pixel Watch 4 even though the emulator renders it.
  - Google publishes Google Sans Flex only as a variable font, which WFF
    can't select weights from, so `googlesans_400`..`_700` are the static
    Latin instances from Fontsource (`@fontsource/google-sans-flex` 5.3.2),
    unwrapped from WOFF with `java tools/Woff2Ttf.java in.woff out.ttf`.
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
- **Notifications.** When there are unread notifications
  (`[UNREAD_NOTIFICATION_COUNT]`), the 12 o'clock marker is replaced by a
  small pill with the count (`1`..`9`, then `9+`): red on Pilot and Flat,
  tinted with the colourway's day colour on Pixel. It stays visible, dimmed,
  in always-on mode.
- **Date** at 3 o'clock: a Cyclops-style window on Pilot, plain white
  gold numerals on Flat (#E3B341, an Apple-style date accent).
- **Tap actions.** Two invisible complication slots sit over the date and the
  GMT label. They draw nothing; whatever provider you choose for them in the
  editor's complications page ("Date tap action", "GMT label tap action")
  decides what a tap opens. Choose **App shortcut** to open any app.
  Defaults: the date slot is Empty, so taps fall through to the date's own
  `Launch target="CALENDAR"` (the system calendar app). The label slot is
  also Empty by default, so no hidden provider keeps sending updates; assign
  an app to it to make the label tappable.
- **Always-on mode.** Outline hands, thin bezel, no seconds, and no dial fill.
- **Efficiency.** Bezel numerals are pre-rendered images (one per style and
  format) instead of outlined text, the GMT hand updates once a minute, and
  nothing depends on the per-minute clock in always-on mode (measured on a
  Pixel Watch 4: about 0.2% of a CPU core in always-on mode, down from 3.7%).
- **Customisation.** In the watch-face editor you can pick the style (Pilot,
  Flat or Pixel), the bezel format (12h or 24h), one of 28 drink-themed bezel colourways (colas and their diet versions,
  citrus, fruit, root beer and ginger, plus softer earthy and pastel teas,
  lattes and cream soda, and Pixel-style tonal pairs; see `COLORWAYS` in the generator),
  the GMT airport, and each complication.

## Project layout

| Path | What it is |
|---|---|
| `tools/WatchFace.java` | **Source of truth.** Generates `res/raw/watchface.xml`, `res/values/strings.xml` and the editor icons. |
| `tools/HandArt.java` | Renders the hand / bezel-pip PNGs into `res/drawable-nodpi`. |
| `tools/Validate.java` | Validates the XML against the official WFF v3 schema (`tools/wff`, Apache 2.0, from google/watchface). |
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
