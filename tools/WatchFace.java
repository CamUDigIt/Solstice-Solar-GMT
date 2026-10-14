import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;

/**
 * Generates Solstice GMT's Watch Face Format (v3) XML.
 *
 * Run from the project root:  java tools/WatchFace.java
 *
 * Writes app/src/main/res/raw/watchface.xml and app/src/main/res/values/strings.xml.
 * Edit this file, not the generated XML: the per-airport bezel math is repeated
 * for every airport option and is not something to maintain by hand.
 *
 * Coordinate space is 450 x 450 with the dial centre at (225, 225); angles are
 * degrees clockwise from 12 o'clock, matching WFF.
 */
public class WatchFace {

    // ---- Airports ---------------------------------------------------------

    record Airport(String code, String city, String zone, double lat, double lon) {}

    // Ordered west to east (by standard UTC offset) so turning the crown in the
    // editor moves steadily around the globe.
    static final List<Airport> AIRPORTS = List.of(
            // UTC-10 .. UTC-3:30
            new Airport("HNL", "Honolulu", "Pacific/Honolulu", 21.32, -157.92),
            new Airport("ANC", "Anchorage", "America/Anchorage", 61.17, -149.99),
            new Airport("LAX", "Los Angeles", "America/Los_Angeles", 33.94, -118.41),
            new Airport("SFO", "San Francisco", "America/Los_Angeles", 37.62, -122.38),
            new Airport("SEA", "Seattle", "America/Los_Angeles", 47.45, -122.31),
            new Airport("YVR", "Vancouver", "America/Vancouver", 49.19, -123.18),
            new Airport("DEN", "Denver", "America/Denver", 39.86, -104.67),
            new Airport("PHX", "Phoenix", "America/Phoenix", 33.43, -112.01),
            new Airport("ORD", "Chicago", "America/Chicago", 41.97, -87.91),
            new Airport("DFW", "Dallas", "America/Chicago", 32.90, -97.04),
            new Airport("MEX", "Mexico City", "America/Mexico_City", 19.44, -99.07),
            new Airport("JFK", "New York", "America/New_York", 40.64, -73.78),
            new Airport("ATL", "Atlanta", "America/New_York", 33.64, -84.43),
            new Airport("MIA", "Miami", "America/New_York", 25.79, -80.29),
            new Airport("YYZ", "Toronto", "America/Toronto", 43.68, -79.63),
            new Airport("BOG", "Bogota", "America/Bogota", 4.70, -74.15),
            new Airport("SCL", "Santiago", "America/Santiago", -33.39, -70.79),
            new Airport("YYT", "St. John's", "America/St_Johns", 47.62, -52.75),
            new Airport("GRU", "Sao Paulo", "America/Sao_Paulo", -23.43, -46.47),
            new Airport("EZE", "Buenos Aires", "America/Argentina/Buenos_Aires", -34.82, -58.54),
            // UTC+0 .. UTC+5
            new Airport("LHR", "London", "Europe/London", 51.47, -0.45),
            new Airport("DUB", "Dublin", "Europe/Dublin", 53.43, -6.25),
            new Airport("CDG", "Paris", "Europe/Paris", 49.01, 2.55),
            new Airport("AMS", "Amsterdam", "Europe/Amsterdam", 52.31, 4.76),
            new Airport("FRA", "Frankfurt", "Europe/Berlin", 50.04, 8.56),
            new Airport("ZRH", "Zurich", "Europe/Zurich", 47.46, 8.55),
            new Airport("MAD", "Madrid", "Europe/Madrid", 40.49, -3.57),
            new Airport("FCO", "Rome", "Europe/Rome", 41.80, 12.25),
            new Airport("CAI", "Cairo", "Africa/Cairo", 30.12, 31.41),
            new Airport("JNB", "Johannesburg", "Africa/Johannesburg", -26.14, 28.24),
            new Airport("IST", "Istanbul", "Europe/Istanbul", 41.26, 28.74),
            new Airport("NBO", "Nairobi", "Africa/Nairobi", -1.32, 36.93),
            new Airport("DOH", "Doha", "Asia/Qatar", 25.27, 51.61),
            new Airport("IKA", "Tehran", "Asia/Tehran", 35.42, 51.15),
            new Airport("DXB", "Dubai", "Asia/Dubai", 25.25, 55.36),
            new Airport("KBL", "Kabul", "Asia/Kabul", 34.57, 69.21),
            new Airport("KHI", "Karachi", "Asia/Karachi", 24.91, 67.16),
            // UTC+5:30 .. UTC+13
            new Airport("DEL", "Delhi", "Asia/Kolkata", 28.56, 77.10),
            new Airport("BOM", "Mumbai", "Asia/Kolkata", 19.09, 72.87),
            new Airport("KTM", "Kathmandu", "Asia/Kathmandu", 27.70, 85.36),
            new Airport("DAC", "Dhaka", "Asia/Dhaka", 23.84, 90.40),
            new Airport("RGN", "Yangon", "Asia/Yangon", 16.91, 96.13),
            new Airport("BKK", "Bangkok", "Asia/Bangkok", 13.69, 100.75),
            new Airport("CGK", "Jakarta", "Asia/Jakarta", -6.13, 106.66),
            new Airport("SIN", "Singapore", "Asia/Singapore", 1.36, 103.99),
            new Airport("HKG", "Hong Kong", "Asia/Hong_Kong", 22.31, 113.91),
            new Airport("PEK", "Beijing", "Asia/Shanghai", 40.08, 116.58),
            new Airport("MNL", "Manila", "Asia/Manila", 14.51, 121.02),
            new Airport("PER", "Perth", "Australia/Perth", -31.94, 115.97),
            new Airport("ICN", "Seoul", "Asia/Seoul", 37.46, 126.44),
            new Airport("HND", "Tokyo", "Asia/Tokyo", 35.55, 139.78),
            new Airport("ADL", "Adelaide", "Australia/Adelaide", -34.95, 138.53),
            new Airport("SYD", "Sydney", "Australia/Sydney", -33.95, 151.18),
            new Airport("MEL", "Melbourne", "Australia/Melbourne", -37.67, 144.84),
            new Airport("NOU", "Noumea", "Pacific/Noumea", -22.01, 166.21),
            new Airport("AKL", "Auckland", "Pacific/Auckland", -37.01, 174.79),
            new Airport("TBU", "Nuku'alofa", "Pacific/Tongatapu", -21.24, -175.15));

    // Editor order: continents in this order; within each, west to east, then A-Z.
    static final java.util.LinkedHashMap<String, List<String>> CONTINENTS = new java.util.LinkedHashMap<>();
    static {
        CONTINENTS.put("North America", List.of("ANC", "ATL", "ORD", "DFW", "DEN", "HNL", "LAX", "MEX", "MIA",
                "JFK", "PHX", "SFO", "SEA", "YYT", "YYZ", "YVR"));
        CONTINENTS.put("South America", List.of("BOG", "EZE", "SCL", "GRU"));
        CONTINENTS.put("Europe", List.of("AMS", "DUB", "FRA", "IST", "LHR", "MAD", "CDG", "FCO", "ZRH"));
        CONTINENTS.put("Africa", List.of("CAI", "JNB", "NBO"));
        // Asia includes the Middle East (Doha, Dubai, Tehran).
        CONTINENTS.put("Asia", List.of("BKK", "PEK", "DEL", "DAC", "DOH", "DXB", "HKG", "CGK", "KBL", "KHI", "KTM",
                "MNL", "BOM", "ICN", "SIN", "IKA", "HND", "RGN"));
        CONTINENTS.put("Oceania", List.of("ADL", "AKL", "MEL", "NOU", "TBU", "PER", "SYD"));
    }

    /** Each continent's cities west to east (by standard UTC offset), then A-Z by city. */
    static List<Airport> editorOrder() {
        var byCode = new java.util.HashMap<String, Airport>();
        for (Airport a : AIRPORTS) byCode.put(a.code, a);
        var order = new java.util.ArrayList<Airport>();
        for (var e : CONTINENTS.entrySet()) {
            e.getValue().stream().map(byCode::get)
                    .sorted(java.util.Comparator.comparingInt(WatchFace::stdMinutes).thenComparing(Airport::city))
                    .forEach(order::add);
        }
        if (order.size() != AIRPORTS.size() || order.contains(null)) {
            throw new IllegalStateException("CONTINENTS must list every airport exactly once");
        }
        return order;
    }

    /**
     * Editor name, e.g. "Denver" over "DEN · UTC−7". The offset is the zone's
     * standard (winter) offset, as time zone pickers usually list it: the
     * name is fixed text, while the label on the face shows the live offset.
     */
    static String displayName(Airport a) {
        int m = stdMinutes(a);
        String off = (m < 0 ? "−" : "+") + Math.abs(m) / 60 + (m % 60 == 0 ? "" : String.format(":%02d", Math.abs(m) % 60));
        // Always two lines (city, then code and offset) so every entry looks alike.
        return a.city + "\n" + a.code + " · UTC" + off;
    }

    static final String DEFAULT_AIRPORT = "LHR";
    /** Option id for "GMT hand follows local time"; its bezel uses the weather forecast. */
    static final String LOCAL = "LOCAL";

    // ---- Destination UTC offset, computed from the UTC clock ----------------
    //
    // The offset label ("LHR +7H") needs the airport's offset and the wearer's
    // offset in one expression. A part with <Localization> sees *every* time
    // source in the airport's zone (including TIMEZONE_OFFSET_MINUTES_DST), so
    // the label is not localised; instead the airport's current offset is built
    // from [MINUTES_SINCE_EPOCH] plus its daylight-saving rule. DstCheck.java
    // verifies these expressions against java.time for every hour of 2026-2040.

    enum Dst { NONE, EU, NA, AU, NZ, CL, EG }

    static java.time.zone.ZoneRules rules(Airport a) {
        return java.time.ZoneId.of(a.zone).getRules();
    }

    // Rules are classified from 2027 onward, so zones whose law changed recently
    // (e.g. British Columbia's permanent UTC-7 from November 2026) use the new rule.
    static final java.time.Instant RULES_FROM = java.time.Instant.parse("2027-01-01T00:00:00Z");

    /** Standard offset in minutes (the year-round offset for zones without DST). */
    static int stdMinutes(Airport a) {
        var r = rules(a);
        return (dstRule(a) == Dst.NONE ? r.getOffset(RULES_FROM) : r.getStandardOffset(RULES_FROM)).getTotalSeconds() / 60;
    }

    static Dst dstRule(Airport a) {
        var r = rules(a);
        var next = r.nextTransition(RULES_FROM);
        if (r.isFixedOffset() || next == null || next.getInstant().isAfter(RULES_FROM.plusSeconds(366L * 86400))) {
            return Dst.NONE;
        }
        String z = a.zone;
        if (z.startsWith("Europe/")) return Dst.EU;
        if (z.equals("America/Santiago")) return Dst.CL;
        if (z.equals("Africa/Cairo")) return Dst.EG;
        if (z.equals("Pacific/Auckland")) return Dst.NZ;
        if (z.startsWith("Australia/")) return Dst.AU;
        if (z.startsWith("America/")) return Dst.NA;
        throw new IllegalStateException("No DST rule family for " + z);
    }

    /** Days since 1970-01-01 for month m (>= 3) / day d of [YEAR] (civil-from-days, Hinnant). */
    static String day(int m, int d) {
        int k = (153 * (m - 3) + 2) / 5 + d - 719469;
        return "(365 * [YEAR] + floor([YEAR] / 4) - floor([YEAR] / 100) + floor([YEAR] / 400) + " + k + ")";
    }

    /** Last weekday wd (0 = Sunday) on or before day expression n. */
    static String lastWd(String n, int wd) {
        return "(" + n + " - (" + n + " + " + (11 - wd) + ") % 7)";
    }

    /** First weekday wd (0 = Sunday) on or after day expression n. */
    static String firstWd(String n, int wd) {
        return "(" + n + " + (" + (wd + 7) + " - (" + n + " + 4) % 7) % 7)";
    }

    /** UTC minute at which local clocks change, on day expression dayExpr. */
    static String at(String dayExpr, int utcMinuteOfDay) {
        return "((" + dayExpr + ") * 1440 + " + utcMinuteOfDay + ")";
    }

    /** Expression for the airport's current UTC offset in minutes. */
    static String destOffset(Airport a) {
        int s = stdMinutes(a);
        String now = "[MINUTES_SINCE_EPOCH]";
        String on = switch (dstRule(a)) {
            case NONE -> null;
            // Last Sunday of March / October, 01:00 UTC.
            case EU -> now + " >= " + at(lastWd(day(3, 31), 0), 60) + " && " + now + " < " + at(lastWd(day(10, 31), 0), 60);
            // Second Sunday of March 02:00 standard / first Sunday of November 02:00 daylight.
            case NA -> now + " >= " + at(firstWd(day(3, 8), 0), 120 - s) + " && " + now + " < " + at(firstWd(day(11, 1), 0), 60 - s);
            // First Sunday of October 02:00 standard / first Sunday of April 03:00 daylight.
            case AU -> now + " >= " + at(firstWd(day(10, 1), 0), 120 - s) + " || " + now + " < " + at(firstWd(day(4, 1), 0), 120 - s);
            // Last Sunday of September 02:00 standard / first Sunday of April 03:00 daylight.
            case NZ -> now + " >= " + at(lastWd(day(9, 30), 0), 120 - s) + " || " + now + " < " + at(firstWd(day(4, 1), 0), 120 - s);
            // Midnight starting the first Sunday on/after 2 Sep (standard) / 2 Apr (daylight).
            case CL -> now + " >= " + at(firstWd(day(9, 2), 0), -s) + " || " + now + " < " + at(firstWd(day(4, 2), 0), -s - 60);
            // Midnight starting the last Friday of April / ending the last Thursday of October.
            case EG -> now + " >= " + at(lastWd(day(4, 30), 5), -s) + " && " + now + " < " + at(lastWd(day(10, 31), 4) + " + 1", -s - 60);
        };
        return on == null ? Integer.toString(s) : "(" + s + " + ((" + on + ") ? 60 : 0))";
    }

    // ---- Bezel colourways ---------------------------------------------------
    // Colours: [0] night, [1] day, [2] night and [3] day in always-on mode.
    // Always-on night colours lift near-black nights so the dimmed ring stays
    // visible; bright days (silver, gold, lime, orange) are darker so the
    // face stays under the 15% always-on brightness limit (WO-P7).

    record Colorway(String id, String name, String night, String day, String ambientNight, String ambientDay) {}

    // Drink-inspired colourways with generic names, grouped colas, citrus,
    // fruit, root beer and ginger, then softer teas, lattes and cream soda.
    // Each pair stays in one colour family (no white, no clashing opposites
    // like red/green); the darker shade is night. Diet versions use silver as
    // the day colour.
    static final List<Colorway> COLORWAYS = List.of(
            // Colas
            new Colorway("blue_cola", "Blue Cola", "#1D4ED8", "#C81E1E", "#1D4ED8", "#C81E1E"),
            new Colorway("diet_blue_cola", "Diet Blue Cola", "#1D4ED8", "#B8BEC6", "#1D4ED8", "#656B72"),
            new Colorway("midnight_cola", "Midnight Cola", "#0B0B10", "#1E5BD7", "#3F3F46", "#1E5BD7"),
            new Colorway("classic_cola", "Classic Cola", "#4A2512", "#B91C1C", "#6B3A20", "#B91C1C"),
            new Colorway("diet_cola", "Diet Cola", "#9F1D24", "#B8BEC6", "#9F1D24", "#656B72"),
            new Colorway("zero_sugar_cola", "Zero Sugar Cola", "#0B0B0D", "#E41E2B", "#3F3F46", "#E41E2B"),
            new Colorway("gold_cola", "Gold Cola", "#8E1B1B", "#D4A72C", "#8E1B1B", "#866A18"),
            new Colorway("cherry_cola", "Cherry Cola", "#3D0C1C", "#B0174F", "#6B1A33", "#B0174F"),
            new Colorway("diet_cherry_cola", "Diet Cherry Cola", "#B0174F", "#B8BEC6", "#B0174F", "#656B72"),
            new Colorway("spiced_cherry", "Spiced Cherry", "#5C1324", "#A9B0B8", "#6A1A2C", "#656B72"),
            new Colorway("spiced_red", "Spiced Red", "#4A0D1A", "#C8102E", "#6B1A2A", "#C8102E"),
            // Citrus and lemon-lime
            new Colorway("lemon_lime", "Lemon-Lime", "#0B3D91", "#1E9E4A", "#0B3D91", "#19873F"),
            new Colorway("lime_green", "Lime Green", "#00553A", "#2DB34A", "#00553A", "#228537"),
            new Colorway("grapefruit", "Grapefruit", "#2E6B1E", "#E5C21F", "#2E6B1E", "#7F6C12"),
            new Colorway("neon_citrus", "Neon Citrus", "#0F5132", "#8DC63F", "#186040", "#557723"),
            new Colorway("diet_neon_citrus", "Diet Neon Citrus", "#0F5132", "#B8BEC6", "#186040", "#656B72"),
            // Fruit
            new Colorway("orange_soda", "Orange Soda", "#9A3412", "#F97316", "#9A3412", "#BF5A11"),
            new Colorway("grape_soda", "Grape Soda", "#3B0764", "#8B5CF6", "#5B21B6", "#7C4FD8"),
            // Root beer and ginger
            new Colorway("root_beer", "Root Beer", "#4A2511", "#E39B0B", "#7C4A2A", "#966707"),
            new Colorway("ginger_ale", "Ginger Ale", "#0B4D2C", "#C9A227", "#115C37", "#846A19"),
            // Soft and earthy: cream soda, teas and lattes
            new Colorway("cream_soda", "Cream Soda", "#5B4636", "#E8D3A8", "#5B4636", "#78694C"),
            new Colorway("matcha_latte", "Matcha Latte", "#2F4A32", "#A9C49A", "#3A5A3E", "#5C7056"),
            new Colorway("green_tea", "Green Tea", "#2F3519", "#8C9440", "#3A4220", "#6A702E"),
            new Colorway("peach_tea", "Peach Tea", "#6B3B2A", "#F2B48C", "#6B3B2A", "#8B634A"),
            new Colorway("lavender_lemonade", "Lavender Lemonade", "#4B3F72", "#C9B8E8", "#4B3F72", "#70658E"),
            new Colorway("hibiscus_tea", "Hibiscus Tea", "#5E1F3A", "#E8A0B4", "#6A2442", "#955D71"),
            new Colorway("blueberry_soda", "Blueberry Soda", "#2C3E66", "#A9BCE0", "#2C3E66", "#5C6B86"),
            // Pixel-inspired tonal pairs
            new Colorway("cucumber_water", "Cucumber Water", "#134E44", "#8ED1BC", "#1A5A4F", "#4D7368"),
            new Colorway("butterfly_pea_tea", "Butterfly Pea Tea", "#2C2F6B", "#9C9CF2", "#383C80", "#6666A3"),
            new Colorway("yuzu_soda", "Yuzu Soda", "#5A4A10", "#F2D36B", "#5A4A10", "#7C6B2F"),
            new Colorway("tonic_water", "Tonic Water", "#37414D", "#A7B3C2", "#3F4A57", "#626B78"));


    // ---- Palette ----------------------------------------------------------

    static final String BG = "#000000";
    static final String DIAL_HI = "#161B23";
    static final String DIAL_LO = "#07090C";
    static final String STEEL = "#C9CED6";
    static final String LUME = "#F4F1E4";
    static final String MUTED = "#8A93A0";
    static final String GMT_RED = "#D72626";
    static final String FLAT_PANEL = "#1C2027";   // Flat complication backgrounds
    static final String FLAT_DATE_GOLD = "#E3B341";
    static final String PIXEL_PANEL = "#262A31";  // Pixel tonal complication fill
    static final String PIXEL_TRACK = "#3A3F48";  // Pixel progress ring track

    // ---- Geometry ---------------------------------------------------------

    static final double C = 225;
    static final double BEZEL_OUTER = 225, BEZEL_INNER = 185;   // 40-unit band
    static final double BEZEL_MID = (BEZEL_OUTER + BEZEL_INNER) / 2;
    static final double BEZEL_THICK = BEZEL_OUTER - BEZEL_INNER;
    static final double DIAL_R = BEZEL_INNER - 2;
    static final double TRACK_OUT = 181, TRACK_IN = 175, TRACK_IN_5 = 171;   // minute track radii
    static final double PLOT_R = 161;                                        // Pilot lume plots
    static final double TICK_IN = 154;                                       // Flat long hour ticks
    static final double PIXEL_DOT_R = 168;                                   // Pixel hour dots

    // Text sizes, in face units. Wear OS quality guideline WO-V14 asks for 12sp
    // essential / 10sp non-essential text. The face scales with the screen, so
    // these are sized for the smallest common watch (384 px at 320 dpi, where
    // one unit = 384/450 px = 0.43sp): 28 units = 12sp, 24 units = 10.2sp.
    static final int TEXT_ESSENTIAL = 28;
    static final int TEXT_SECONDARY = 24;
    static final int BEZEL_NUMERAL = 26;   // bezel numerals, scaled with the 40-unit band

    // ---- Styles -----------------------------------------------------------
    // Pilot (GMT-Master II inspired): Oswald, Mercedes/sword hands, lume plots.
    // Flat (Apple Watch GMT inspired): Inter, flat stick hands, long
    // hour ticks. Pixel (Pixel Watch inspired): Google Sans Flex, capsule
    // hands, dot markers, tonal panels. Fonts are bundled in res/font (SIL OFL 1.1, see /licenses).
    // Hand geometry must match HandArt.java.

    /** Hand width, pivot-to-tip and pivot-to-butt, without the HAND_PAD margin. */
    record HandGeom(int w, int len, int tail) {}

    static final int HAND_PAD = 2;   // margin around each hand image (HandArt.PAD)

    record Style(String id, String name, String font, String handPrefix,
                 HandGeom hour, HandGeom minute, HandGeom second, HandGeom gmt) {}

    static final Style PILOT = new Style("pilot", "Pilot", "oswald", "",
            new HandGeom(26, 104, 20), new HandGeom(14, 176, 22), new HandGeom(14, 182, 40), new HandGeom(30, 186, 0));
    static final Style FLAT = new Style("flat", "Flat", "inter", "flat_",
            new HandGeom(12, 100, 12), new HandGeom(10, 174, 12), new HandGeom(10, 182, 36), new HandGeom(10, 186, 0));
    static final Style PIXEL = new Style("pixel", "Pixel", "googlesans", "pixel_",
            new HandGeom(18, 100, 16), new HandGeom(14, 172, 16), new HandGeom(10, 182, 30), new HandGeom(14, 160, 0));
    static final List<Style> STYLES = List.of(PILOT, FLAT, PIXEL);

    /** The style / bezel format being emitted inside styled() / formatted(). */
    static Style style = PILOT;
    static boolean h24 = false;

    /**
     * Emits body once per style, switched on the style setting. A Condition
     * (not a ListConfiguration) is used because configurations can't be nested,
     * and much of this lives inside the airport ListConfiguration.
     */
    static void styled(Runnable body) {
        Style saved = style;
        chain(new String[] {"[CONFIGURATION.style] == \"flat\"", "[CONFIGURATION.style] == \"pixel\""},
                new Runnable[] {() -> { style = FLAT; body.run(); }, () -> { style = PIXEL; body.run(); }},
                () -> { style = PILOT; body.run(); });
        style = saved;
    }

    /** Emits body once per bezel format (12h / 24h setting). */
    static void formatted(Runnable body) {
        boolean saved = h24;
        switchOn("[CONFIGURATION.bezel_format] == \"24\"", () -> { h24 = true; body.run(); }, () -> { h24 = false; body.run(); });
        h24 = saved;
    }

    static int conditionCount = 0;

    static void switchOn(String expr, Runnable whenTrue, Runnable otherwise) {
        String name = "c" + (conditionCount++);
        open("Condition");
        open("Expressions");
        line("%s", "<Expression name=\"" + name + "\"><![CDATA[" + expr + "]]></Expression>");
        close("Expressions");
        open("Compare expression=\"%s\"", name);
        whenTrue.run();
        close("Compare");
        open("Default");
        otherwise.run();
        close("Default");
        close("Condition");
    }

    /**
     * Font attributes for the current style. Each weight points straight at
     * its TTF (res/font/<font>_<weight>.ttf): an XML <font-family> resource
     * renders on the emulator but hangs the watch face runtime on a Pixel
     * Watch 4 (Wear OS 6.1, API 37).
     */
    static String font(int size, String weight, String color) {
        String w = switch (weight) {
            case "BOLD" -> "700";
            case "SEMI_BOLD" -> "600";
            case "MEDIUM" -> "500";
            case "NORMAL" -> "400";
            default -> throw new IllegalArgumentException("No bundled weight for " + weight);
        };
        return String.format(Locale.ROOT, "Font family=\"%s_%s\" size=\"%d\" color=\"%s\"", style.font, w, size, color);
    }

    static final StringBuilder out = new StringBuilder();
    static int indent = 0;

    public static void main(String[] args) throws Exception {
        line("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        line("<!-- GENERATED by tools/WatchFace.java. Edit the generator, not this file. -->");
        open("WatchFace width=\"450\" height=\"450\" clipShape=\"CIRCLE\"");
        line("<Metadata key=\"CLOCK_TYPE\" value=\"ANALOG\"/>");
        line("<Metadata key=\"PREVIEW_TIME\" value=\"10:08:32\"/>");
        userConfigurations();
        open("Scene backgroundColor=\"" + BG + "\"");
        dial();
        dateWindow();
        complications();
        airportGroups();
        bezelMarkings();
        localHands();
        centerCap();
        close("Scene");
        close("WatchFace");

        write("app/src/main/res/raw/watchface.xml", out.toString());
        write("app/src/main/res/values/strings.xml", strings());
        icons(new File("app/src/main/res/drawable-nodpi"));
        System.out.println("Wrote watchface.xml, strings.xml and editor icons");
    }

    // ---- Editor icons -----------------------------------------------------

    static final int ICON = 96;

    static void icons(File dir) throws Exception {
        dir.mkdirs();
        Colorway first = COLORWAYS.get(0);
        icon(dir, "icon_cfg_bezel", g -> bezelSwatch(g, first));
        for (Colorway c : COLORWAYS) icon(dir, "icon_bezel_" + c.id, g -> bezelSwatch(g, c));
        icon(dir, "icon_cfg_style", g -> styleBadge(g, PILOT));
        for (Style s : STYLES) icon(dir, "icon_style_" + s.id, g -> styleBadge(g, s));
        icon(dir, "icon_cfg_airport", WatchFace::globe);
    }

    interface Painter { void paint(java.awt.Graphics2D g); }

    static void icon(File dir, String name, Painter p) throws Exception {
        int ss = 4;
        var big = new java.awt.image.BufferedImage(ICON * ss, ICON * ss, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g = big.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.scale(ss, ss);
        p.paint(g);
        g.dispose();
        var img = new java.awt.image.BufferedImage(ICON, ICON, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var g2 = img.createGraphics();
        g2.drawImage(big.getScaledInstance(ICON, ICON, java.awt.Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g2.dispose();
        javax.imageio.ImageIO.write(img, "png", new File(dir, name + ".png"));
    }

    static java.awt.Color color(String hex) {
        return java.awt.Color.decode(hex);
    }

    /** Two-tone bezel ring: day on the top half, night below. */
    static void bezelSwatch(java.awt.Graphics2D g, Colorway c) {
        double o = 4, d = ICON - 8;
        g.setColor(color(c.night));
        g.fill(new java.awt.geom.Ellipse2D.Double(o, o, d, d));
        g.setColor(color(c.day));
        g.fill(new java.awt.geom.Arc2D.Double(o, o, d, d, 0, 180, java.awt.geom.Arc2D.PIE));
        g.setColor(color("#0B0E14"));
        g.fill(new java.awt.geom.Ellipse2D.Double(24, 24, ICON - 48, ICON - 48));
        g.setColor(new java.awt.Color(255, 255, 255, 90));
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.draw(new java.awt.geom.Ellipse2D.Double(o, o, d, d));
    }

    /** Globe with a red GMT arrow. */
    static void globe(java.awt.Graphics2D g) {
        g.setColor(color("#0B0E14"));
        g.fill(new java.awt.geom.Ellipse2D.Double(4, 4, ICON - 8, ICON - 8));
        g.setColor(color(STEEL));
        g.setStroke(new java.awt.BasicStroke(3f));
        g.draw(new java.awt.geom.Ellipse2D.Double(16, 16, ICON - 32, ICON - 32));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new java.awt.geom.Ellipse2D.Double(34, 16, ICON - 68, ICON - 32));
        g.draw(new java.awt.geom.Line2D.Double(16, ICON / 2.0, ICON - 16, ICON / 2.0));
        g.draw(new java.awt.geom.Line2D.Double(ICON / 2.0, 16, ICON / 2.0, ICON - 16));
        g.setColor(color(GMT_RED));
        var arrow = new java.awt.geom.Path2D.Double();
        arrow.moveTo(ICON - 14, 14);
        arrow.lineTo(ICON - 34, 18);
        arrow.lineTo(ICON - 18, 34);
        arrow.closePath();
        g.fill(arrow);
        g.setStroke(new java.awt.BasicStroke(3f));
        g.draw(new java.awt.geom.Line2D.Double(ICON / 2.0, ICON / 2.0, ICON - 22, 22));
    }

    /** "12" set in the style's own bundled font. */
    static void styleBadge(java.awt.Graphics2D g, Style s) {
        g.setColor(color("#0B0E14"));
        g.fill(new java.awt.geom.Ellipse2D.Double(4, 4, ICON - 8, ICON - 8));
        g.setColor(color("#3A424D"));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new java.awt.geom.Ellipse2D.Double(5, 5, ICON - 10, ICON - 10));
        String file = s.font + "_600.ttf";
        try {
            var f = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, new File("app/src/main/res/font/" + file));
            g.setFont(f.deriveFont(44f));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        var fm = g.getFontMetrics();
        g.setColor(color("#F1F2F4"));
        g.drawString("12", (ICON - fm.stringWidth("12")) / 2f, ICON / 2f + fm.getAscent() / 2f - 6);
    }

    // ---- User configuration -----------------------------------------------

    static void userConfigurations() {
        open("UserConfigurations");
        open("ListConfiguration id=\"style\" displayName=\"cfg_style\" screenReaderText=\"cfg_style\" icon=\"icon_cfg_style\" defaultValue=\"pilot\"");
        for (Style s : STYLES) {
            line("<ListOption id=\"%s\" displayName=\"style_%s\" icon=\"icon_style_%s\"/>", s.id, s.id, s.id);
        }
        close("ListConfiguration");
        open("ListConfiguration id=\"bezel_format\" displayName=\"cfg_bezel_format\" screenReaderText=\"cfg_bezel_format\" defaultValue=\"12\"");
        line("<ListOption id=\"12\" displayName=\"bezel_format_12\"/>");
        line("<ListOption id=\"24\" displayName=\"bezel_format_24\"/>");
        close("ListConfiguration");
        open("ColorConfiguration id=\"bezel\" displayName=\"cfg_bezel\" screenReaderText=\"cfg_bezel\" icon=\"icon_cfg_bezel\" defaultValue=\"blue_cola\"");
        for (Colorway c : COLORWAYS) {
            line("<ColorOption id=\"%s\" displayName=\"bezel_%s\" icon=\"icon_bezel_%s\" colors=\"%s %s %s %s\"/>",
                    c.id, c.id, c.id, c.night, c.day, c.ambientNight, c.ambientDay);
        }
        close("ColorConfiguration");
        open("ListConfiguration id=\"airport\" displayName=\"cfg_airport\" screenReaderText=\"cfg_airport\" icon=\"icon_cfg_airport\" defaultValue=\"%s\"", DEFAULT_AIRPORT);
        line("<ListOption id=\"%s\" displayName=\"airport_local\"/>", LOCAL);
        for (Airport a : editorOrder()) {
            String id = a.code.toLowerCase(Locale.ROOT);
            line("<ListOption id=\"%s\" displayName=\"airport_%s\"/>", a.code, id);
        }
        close("ListConfiguration");
        close("UserConfigurations");
    }

    // ---- Static dial ------------------------------------------------------

    static void dial() {
        comment("Dial: radial gradient, minute track and per-style hour markers");
        open("Group name=\"dial\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(C - DIAL_R), f(C - DIAL_R), f(2 * DIAL_R), f(2 * DIAL_R));
        open("Fill color=\"%s\"", DIAL_LO);
        line("<RadialGradient centerX=\"225\" centerY=\"200\" radius=\"%s\" colors=\"%s %s\" positions=\"0 1\"/>", f(DIAL_R + 20), DIAL_HI, DIAL_LO);
        close("Fill");
        close("Ellipse");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        close("PartDraw");

        // Minute track: 60 ticks, heavier every 5 minutes. Pixel has no track.
        switchOn("[CONFIGURATION.style] != \"pixel\"", () -> {
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            for (int m = 0; m < 60; m++) {
                boolean five = m % 5 == 0;
                double a = m * 6;
                double r1 = five ? TRACK_IN_5 : TRACK_IN;
                open("Line startX=\"%s\" startY=\"%s\" endX=\"%s\" endY=\"%s\"", px(r1, a), py(r1, a), px(TRACK_OUT, a), py(TRACK_OUT, a));
                line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"BUTT\"/>", five ? STEEL : MUTED, five ? "2" : "1");
                close("Line");
            }
            line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"110\"/>");
            close("PartDraw");
        }, () -> line("<Group name=\"no_track\" x=\"0\" y=\"0\" width=\"1\" height=\"1\"/>"));
        styled(WatchFace::hourMarkers);
        close("Group");
    }

    /**
     * Pilot: small lume plots with a triangle at 12. Flat: long lumed hour ticks.
     * Pixel: round dots, larger at 12, 6 and 9. 3 o'clock is the date.
     */
    static void hourMarkers() {
        open("Group name=\"markers_%s\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"", style.id);
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        for (int h = 1; h <= 12; h++) {
            double a = h * 30;
            // 3 is the date; Pilot has its own 12 triangle and leaves 5 and 7 clear for
            // the taller event box that two-line Oswald text needs.
            if (h == 3 || (style == PILOT && (h == 12 || h == 5 || h == 7))) continue;
            if (style == PIXEL) {
                double d = h % 3 == 0 ? 12 : 6;
                open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(dx(PIXEL_DOT_R, a) - d / 2), f(dy(PIXEL_DOT_R, a) - d / 2), f(d), f(d));
                line("<Fill color=\"%s\"/>", h % 3 == 0 ? "#F1F2F4" : "#AEB4BD");
                close("Ellipse");
            } else if (style == PILOT) {
                double d = 10;
                open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(dx(PLOT_R, a) - d / 2), f(dy(PLOT_R, a) - d / 2), f(d), f(d));
                line("<Stroke color=\"%s\" thickness=\"1.4\"/>", STEEL);
                line("<Fill color=\"%s\"/>", LUME);
                close("Ellipse");
            } else {
                open("Line startX=\"%s\" startY=\"%s\" endX=\"%s\" endY=\"%s\"", px(TICK_IN, a), py(TICK_IN, a), px(TRACK_OUT, a), py(TRACK_OUT, a));
                line("<Stroke color=\"%s\" thickness=\"4\" cap=\"BUTT\"/>", LUME);
                close("Line");
            }
        }
        close("PartDraw");
        if (style == PILOT) {
            int w = 20, h = 18;
            open("PartImage x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", (int) C - w / 2, (int) (C - PLOT_R - h / 2.0), w, h);
            line("<Image resource=\"pilot_marker_12\"/>");
            close("PartImage");
        }
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"140\"/>");
        close("Group");
    }

    static final int DATE_CX = 362;

    // What tapping the date opens when no app is chosen for "Date tap action".
    static String DATE_LAUNCH = "CALENDAR";

    static void dateWindow() {
        comment("Date at 3 o'clock: Pilot has a Cyclops-style magnified window, Flat plain numerals");
        styled(() -> {
            boolean pilot = style == PILOT;
            int w = pilot ? 52 : 48, h = pilot ? 44 : 38, size = pilot ? 36 : TEXT_ESSENTIAL;
            int lens = pilot ? 4 : 0;
            int x = DATE_CX - w / 2 - lens, y = (int) C - h / 2 - lens;
            open("Group name=\"date_%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", style.id, x, y, w + 2 * lens, h + 2 * lens);
            // Opens the system calendar unless the tap slot above has an app chosen.
            line("<Launch target=\"%s\"/>", DATE_LAUNCH);
            if (pilot) {
                open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"", w + 2 * lens, h + 2 * lens);
                // Lens rim and a faint glass tint around the window.
                open("RoundRectangle x=\"0.6\" y=\"0.6\" width=\"%s\" height=\"%s\" cornerRadiusX=\"8\" cornerRadiusY=\"8\"", f(w + 2 * lens - 1.2), f(h + 2 * lens - 1.2));
                line("<Stroke color=\"#AEB6C0\" thickness=\"1.2\"/>");
                line("<Fill color=\"#1FFFFFFF\"/>");
                close("RoundRectangle");
                open("RoundRectangle x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" cornerRadiusX=\"3\" cornerRadiusY=\"3\"", lens + 1, lens + 1, w - 2, h - 2);
                line("<Stroke color=\"%s\" thickness=\"2\"/>", STEEL);
                line("<Fill color=\"#F5F5F2\"/>");
                close("RoundRectangle");
                line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
                close("PartDraw");
                // Active: dark numerals on white. Ambient: light numerals on black.
                dateText(lens, w, h, size, "#111111", 255, 0);
                dateText(lens, w, h, size, STEEL, 0, 255);
            } else if (style == PIXEL) {
                // Pixel: white numerals in a pill tinted with the colourway's day colour.
                open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" alpha=\"90\"", w, h);
                line("<RoundRectangle x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" cornerRadiusX=\"%s\" cornerRadiusY=\"%s\"><Fill color=\"[CONFIGURATION.bezel.1]\"/></RoundRectangle>",
                        w, h, f(h / 2.0), f(h / 2.0));
                line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
                close("PartDraw");
                dateText(0, w, h, size, "#FFFFFF", 255, 200);
            } else {
                // Flat: gold numerals straight on the dial (Apple-style date accent).
                dateText(0, w, h, size, FLAT_DATE_GOLD, 255, 200);
            }
            close("Group");
        });
    }

    static void dateText(int inset, int w, int h, int size, String color, int alpha, int ambientAlpha) {
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" alpha=\"%d\"", inset, inset, w, h, alpha);
        open("Text align=\"CENTER\"");
        open(font(size, "BOLD", color));
        line("<Template>%%s<Parameter expression=\"[DAY]\"/></Template>");
        close("Font");
        close("Text");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambientAlpha);
        close("PartText");
    }

    // ---- Complications ----------------------------------------------------

    // Tap areas are at least 112 units: 48dp (the Wear OS minimum touch target)
    // on a 384 px / 320 dpi watch. The drawn elements stay smaller inside them.
    static final int TAP_MIN = 112;

    // Round slot at 9 o'clock, mirroring the date window at 3.
    static final int ROUND_D = 100;            // drawn disc
    static final int ROUND_TAP = 116;          // tap circle (>= TAP_MIN)
    static final int ROUND_CX = 122;
    static final int ROUND_X = ROUND_CX - ROUND_TAP / 2, ROUND_Y = (int) C - ROUND_TAP / 2;

    // 6 o'clock long text: drawn box LONG_H tall at the top of a TAP_MIN-tall tap box.
    static final int LONG_X = 120, LONG_Y = 284, LONG_W = 210, LONG_H = 72;

    static void complications() {
        comment("Round complication at 9 o'clock");
        roundSlot();
        comment("6 o'clock long-text complication");
        longTextSlot();
        comment("Invisible tap targets: the provider chosen for these sets what the date and GMT label open");
        tapSlot(201, "slot_date_tap", DATE_CX - TAP_MIN / 2, (int) C - TAP_MIN / 2, TAP_MIN, TAP_MIN, "EMPTY");
        tapSlot(202, "slot_label_tap", (int) C - LABEL_W / 2, LABEL_Y + LABEL_H / 2 - TAP_MIN / 2, LABEL_W, TAP_MIN, "WORLD_CLOCK");
    }

    /**
     * A complication slot that draws nothing. Tapping it runs the chosen
     * provider's tap action, so picking "App shortcut" in the editor makes the
     * area underneath open any app.
     */
    static void tapSlot(int id, String name, int x, int y, int w, int h, String provider) {
        List<String> types = List.of("SHORT_TEXT", "LONG_TEXT", "SMALL_IMAGE", "MONOCHROMATIC_IMAGE", "RANGED_VALUE", "GOAL_PROGRESS");
        open("ComplicationSlot slotId=\"%d\" displayName=\"%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" supportedTypes=\"%s EMPTY\"",
                id, name, x, y, w, h, String.join(" ", types));
        line("<DefaultProviderPolicy defaultSystemProvider=\"%s\" defaultSystemProviderType=\"%s\"/>", provider, provider.equals("EMPTY") ? "EMPTY" : "SHORT_TEXT");
        line("<BoundingRoundBox x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" cornerRadius=\"8\"/>", w, h);
        for (String t : types) line("<Complication type=\"%s\"/>", t);
        close("ComplicationSlot");
    }

    static void roundSlot() {
        int d = ROUND_D, o = (ROUND_TAP - ROUND_D) / 2;
        double c = ROUND_TAP / 2.0;
        open("ComplicationSlot slotId=\"101\" displayName=\"slot_left\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" supportedTypes=\"RANGED_VALUE GOAL_PROGRESS SHORT_TEXT MONOCHROMATIC_IMAGE EMPTY\"",
                ROUND_X, ROUND_Y, ROUND_TAP, ROUND_TAP);
        line("<DefaultProviderPolicy defaultSystemProvider=\"WATCH_BATTERY\" defaultSystemProviderType=\"RANGED_VALUE\"/>");
        line("<BoundingOval x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"/>", ROUND_TAP, ROUND_TAP);
        String ranged = "clamp(([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN]) / ([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN]), 0, 1)";
        String goal = "clamp([COMPLICATION.GOAL_PROGRESS_VALUE] / [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE], 0, 1)";
        for (String type : List.of("RANGED_VALUE", "GOAL_PROGRESS", "SHORT_TEXT", "MONOCHROMATIC_IMAGE")) {
            open("Complication type=\"%s\"", type);
            // Pilot: recessed sub-dial with a hairline rim. Flat / Pixel: solid flat disc.
            styled(() -> {
                open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"", ROUND_TAP, ROUND_TAP);
                open("Ellipse x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", o + 1, o + 1, d - 2, d - 2);
                if (style == PILOT) line("<Stroke color=\"#334155\" thickness=\"1\"/>");
                line("<Fill color=\"%s\"/>", style == PILOT ? "#0A0D12" : style == PIXEL ? PIXEL_PANEL : FLAT_PANEL);
                close("Ellipse");
                line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
                close("PartDraw");
            });
            if (type.equals("RANGED_VALUE") || type.equals("GOAL_PROGRESS")) {
                String frac = type.equals("RANGED_VALUE") ? ranged : goal;
                styled(() -> {
                    double thick = style == PILOT ? 4 : style == PIXEL ? 6 : 5;
                    open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"", ROUND_TAP, ROUND_TAP);
                    localArc(c, d / 2.0 - 7, 0, 360, style == PILOT ? "#2A313B" : style == PIXEL ? PIXEL_TRACK : "#343A44", thick, null);
                    localArc(c, d / 2.0 - 7, 0, 0, "[CONFIGURATION.bezel.1]", thick,
                            "<Transform target=\"endAngle\" value=\"" + frac + " * 360\"/>");
                    line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"170\"/>");
                    close("PartDraw");
                });
            }
            if (type.equals("MONOCHROMATIC_IMAGE")) {
                icon(c - 22, c - 22, 44);
            } else {
                styled(() -> roundText(o, d, c, style == PILOT ? STEEL : "#FFFFFF"));
            }
            close("Complication");
        }
        close("ComplicationSlot");
    }

    /**
     * Round slot icon and text, centred in the disc. The value sits on the
     * disc's centre with the provider's icon (when it has one) above it. Times
     * like "7:00 AM" don't fit at 12sp, so a trailing AM/PM is split off and
     * drawn smaller under the time.
     */
    static void roundText(int o, int d, double c, String color) {
        String t = "[COMPLICATION.TEXT]", len = "textLength(" + t + ")";
        String tail = "subText(" + t + ", " + len + " - 2, " + len + ")";
        switchOn(len + " > 2 && (" + tail + " == \"AM\" || " + tail + " == \"PM\")", () -> {
            icon(c - 9, o + 12, 18);
            textLine(o + 6, o + 28, d - 12, 34, TEXT_ESSENTIAL, "MEDIUM", color, "subText(" + t + ", 0, " + len + " - 2)");
            textLine(o + 6, o + 58, d - 12, 24, TEXT_SECONDARY, "NORMAL", MUTED, tail);
        }, () -> {
            icon(c - 11, o + 11, 22);
            textLine(o + 6, o + 33, d - 12, 34, TEXT_ESSENTIAL, "MEDIUM", color, t);
        });
    }

    /** The provider's monochrome icon, tinted steel. */
    static void icon(double x, double y, int size) {
        open("PartImage x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" tintColor=\"%s\"", (int) x, (int) y, size, size, STEEL);
        line("<Image resource=\"[COMPLICATION.MONOCHROMATIC_IMAGE]\"/>");
        close("PartImage");
    }

    /** Arc around (c, c) in a part's own coordinates. */
    static void localArc(double c, double r, double start, double end, String color, double thick, String extra) {
        open("Arc centerX=\"%s\" centerY=\"%s\" width=\"%s\" height=\"%s\" startAngle=\"%s\" endAngle=\"%s\"",
                f(c), f(c), f(2 * r), f(2 * r), f(start), f(end));
        line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"ROUND\"/>", color, f(thick));
        if (extra != null) line("%s", extra);
        close("Arc");
    }

    static void longTextSlot() {
        // As wide as the dial allows at this height (box corners stay inside the minute track).
        int w = LONG_W, h = LONG_H;
        open("ComplicationSlot slotId=\"301\" displayName=\"slot_bottom\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" supportedTypes=\"LONG_TEXT SHORT_TEXT EMPTY\"",
                LONG_X, LONG_Y, w, TAP_MIN);
        line("<DefaultProviderPolicy defaultSystemProvider=\"NEXT_EVENT\" defaultSystemProviderType=\"LONG_TEXT\"/>");
        line("<BoundingRoundBox x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" cornerRadius=\"8\"/>", w, TAP_MIN);
        for (String type : List.of("LONG_TEXT", "SHORT_TEXT")) {
            open("Complication type=\"%s\"", type);
            styled(() -> {
                // Oswald's line height is ~41.5 units at 12sp, so two lines need a
                // taller box on Pilot; it's narrower to keep its corners inside the
                // minute track. Inter fits two lines in LONG_H.
                boolean pilot = style == PILOT;
                int bw = pilot ? 180 : w, bh = pilot ? 86 : h, bx = (w - bw) / 2;
                open("Group name=\"long_text_%s\" x=\"%d\" y=\"0\" width=\"%d\" height=\"%d\"", style.id, bx, bw, bh);
                // No box: the dial shows through behind the event text.
                longTextBody(bw, bh);
                close("Group");
            });
            close("Complication");
        }
        close("ComplicationSlot");
    }

    /**
     * Text over title when the provider sends a title; otherwise the text gets
     * both lines (e.g. calendar providers that put "Event · 3:00 PM" in TEXT).
     */
    static void longTextBody(int w, int h) {
        open("Condition");
        open("Expressions");
        // A blank or one-character title counts as no title, so the text gets both lines.
        line("<Expression name=\"hasTitle\"><![CDATA[[COMPLICATION.TITLE] != null && textLength([COMPLICATION.TITLE]) > 1]]></Expression>");
        close("Expressions");
        open("Compare expression=\"hasTitle\"");
        int textH = style == PILOT ? 42 : 36;   // one line of Oswald / Inter at 12sp
        textLine(4, 2, w - 8, textH, TEXT_ESSENTIAL, "MEDIUM", STEEL, "[COMPLICATION.TEXT]");
        textLine(4, 2 + textH, w - 8, h - textH - 4, TEXT_SECONDARY, "NORMAL", MUTED, "[COMPLICATION.TITLE]");
        close("Compare");
        open("Default");
        splitOnSeparator(w, h, textH);
        close("Default");
        close("Condition");
    }

    static final String[] SEPARATORS = {" · ", " - ", " – ", " — "};
    static final int MAX_SPLIT = 48;   // longest event name the split looks for

    /**
     * Calendar providers often send one string like "Daily Planning · Wed
     * 7:00 AM". WFF has no find function, so each position is checked for a
     * separator: the first one found splits the text into the event name and
     * the time on two lines, dropping the separator. Text without one wraps
     * onto two lines as it is.
     */
    static void splitOnSeparator(int w, int h, int textH) {
        String t = "[COMPLICATION.TEXT]", len = "textLength(" + t + ")";
        String[] exprs = new String[MAX_SPLIT];
        Runnable[] bodies = new Runnable[MAX_SPLIT];
        for (int i = 1; i <= MAX_SPLIT; i++) {
            StringBuilder any = new StringBuilder();
            for (String sep : SEPARATORS) {
                if (any.length() > 0) any.append(" || ");
                any.append("subText(").append(t).append(", ").append(i).append(", ").append(i + 3).append(") == \"").append(sep).append("\"");
            }
            exprs[i - 1] = len + " > " + (i + 3) + " && (" + any + ")";
            final int at = i;
            bodies[i - 1] = () -> {
                textLine(4, 2, w - 8, textH, TEXT_ESSENTIAL, "MEDIUM", STEEL, "subText(" + t + ", 0, " + at + ")");
                textLine(4, 2 + textH, w - 8, textH, TEXT_ESSENTIAL, "MEDIUM", STEEL, "subText(" + t + ", " + (at + 3) + ", " + len + ")");
            };
        }
        chain(exprs, bodies, () -> textLine(4, 2, w - 8, h - 4, TEXT_ESSENTIAL, "MEDIUM", STEEL, t, 2));
    }

    static void textLine(int x, int y, int w, int h, int size, String weight, String color, String expr) {
        textLine(x, y, w, h, size, weight, color, expr, 1);
    }

    static void textLine(int x, int y, int w, int h, int size, String weight, String color, String expr, int maxLines) {
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        // On a Pixel Watch 4 (API 37) ellipsis forces a single line even with
        // maxLines > 1, so multi-line text relies on the part's bounds instead.
        if (maxLines > 1) open("Text align=\"CENTER\" maxLines=\"%d\"", maxLines);
        else open("Text align=\"CENTER\" ellipsis=\"TRUE\" maxLines=\"1\"");
        open(font(size, weight, color));
        line("<Template>%%s<Parameter expression=\"%s\"/></Template>", expr);
        close("Font");
        close("Text");
        close("PartText");
    }

    // ---- Airport-dependent parts: solar bezel, label, GMT hand ------------

    static void airportGroups() {
        // Localization must sit on each Part/AnalogClock: on a Group it does not
        // change the time data sources used by its children's expressions.
        comment("Per-airport group: bezel, label and GMT hand in the airport's time zone");
        open("ListConfiguration id=\"airport\"");
        open("ListOption id=\"%s\"", LOCAL);
        open("Group name=\"airport_local\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        weatherBezel(false);
        weatherBezel(true);
        localLabel();
        gmtHand(null);
        close("Group");
        close("ListOption");
        for (Airport a : AIRPORTS) {
            open("ListOption id=\"%s\"", a.code);
            open("Group name=\"airport_%s\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"", a.code);
            solarBezel(a, false);
            solarBezel(a, true);
            airportLabel(a);
            gmtHand(a.zone);
            close("Group");
            close("ListOption");
        }
        close("ListConfiguration");
    }

    /**
     * Solar bezel: red/blue shows day and night at the destination, drawn on a
     * 24-hour scale (midnight at the top) whichever bezel labels are chosen, so
     * sunrise and sunset sit in the same place on the 12h and 24h faces: an
     * 18:15 sunset is at 9:15 on the 12h labels and just past 18 on the 24h
     * ones. The bands show the real day and night lengths.
     *
     * Sunrise/sunset come from the NOAA approximation (equation of time and
     * solar declination from the day of year), accurate to a few minutes.
     */
    static void solarBezel(Airport a, boolean ambient) {
        double latR = Math.toRadians(a.lat);
        // cos(90.833 deg) / cos(lat), and tan(lat): constant per airport.
        double k1 = Math.cos(Math.toRadians(90.833)) / Math.cos(latR);
        double k2 = Math.tan(latR);

        String g = "(6.283185 / 365 * ([DAY_OF_YEAR] - 0.5))";
        String eqTime = "(229.18 * (0.000075 + 0.001868 * cos(" + g + ") - 0.032077 * sin(" + g + ")"
                + " - 0.014615 * cos(2 * " + g + ") - 0.040849 * sin(2 * " + g + ")))";
        String decl = "(0.006918 - 0.399912 * cos(" + g + ") + 0.070257 * sin(" + g + ")"
                + " - 0.006758 * cos(2 * " + g + ") + 0.000907 * sin(2 * " + g + ")"
                + " - 0.002697 * cos(3 * " + g + ") + 0.00148 * sin(3 * " + g + "))";
        String halfDayMin = "(4 * deg(acos(clamp(" + n(k1) + " / cos(" + decl + ") - " + n(k2)
                + " * tan(" + decl + "), -1, 1))))";
        // Shift the longitude by 360 degrees where the zone sits across the date
        // line from it (Tonga: UTC+13 at 175 W), so local noon lands near 12:00.
        double lon = a.lon;
        while (720 - 4 * lon + stdMinutes(a) > 1440) lon += 360;
        while (720 - 4 * lon + stdMinutes(a) < 0) lon -= 360;
        String noonMin = "(720 - 4 * " + n(lon) + " - " + eqTime + " + [TIMEZONE_OFFSET_MINUTES_DST])";
        String sunrise = "((" + noonMin + " - " + halfDayMin + ") / 60)";
        String sunset = "((" + noonMin + " + " + halfDayMin + ") / 60)";

        bezelPart(ambient, a.zone, () -> dayArc(sunrise + " * 15", sunset + " * 15"));
    }

    /**
     * LOCAL's bezel: the watch has no location source in WFF, so daylight comes
     * from the hourly weather forecast's is-day flags for the wearer's location,
     * on the same 24-hour scale as the airport bezels.
     *
     * Each flag describes the start of its hour, so a change between two hours
     * is drawn halfway between them (within about 30 minutes of the real
     * sunrise/sunset). The current hour uses the hourly forecast too: the
     * "current conditions" flag can lag by hours. With no weather data the ring
     * is night.
     */
    static void weatherBezel(boolean ambient) {
        String hour = "floor([HOUR_0_23] + [MINUTE] / 60)";
        // No forecast at all, or "day" for every one of the next 24 hours (what
        // the watch-face editor's preview feeds in), can't place sunrise/sunset:
        // show a nominal 06:00-18:00 day so the ring still shows both colours.
        StringBuilder allDay = new StringBuilder();
        for (int k = 0; k < 24; k++) {
            if (k > 0) allDay.append(" && ");
            allDay.append("[WEATHER.HOURS.").append(k).append(".IS_DAY]");
        }
        String noForecast = "![WEATHER.HOURS.0.IS_AVAILABLE] && ![WEATHER.IS_AVAILABLE]";
        switchOn("(" + noForecast + ") || (" + allDay + ")",
                () -> bezelPart(ambient, null, () -> dayArc("90", "270")),
                () -> bezelPart(ambient, null, () -> {
                    for (int k = 0; k < 24; k++) {
                        String hourly = hourlyDay(k);
                        // The current hour's own flag can disagree with the hours either
                        // side of it (seen on a Pixel Watch 4: night at 10:55 AM between
                        // two day hours). When the next hour and the previous one (hour
                        // 23, tomorrow) agree, follow them; the flag only decides at
                        // sunrise and sunset, where they differ.
                        String current = "(" + hourly + ") || (![WEATHER.HOURS.0.IS_AVAILABLE] && [WEATHER.IS_AVAILABLE] && [WEATHER.IS_DAY])";
                        String next = "(" + hourlyDay(1) + ")", prev = "(" + hourlyDay(23) + ")";
                        String isDay = k != 0 ? hourly
                                : "(" + next + " && " + prev + ") || (" + next + " != " + prev + " && (" + current + "))";
                        String s = "(" + hour + " + " + k + " - 0.5) * 15";
                        dayArc(s, "(" + isDay + ") ? (" + s + " + 15) : " + s);
                    }
                }));
    }

    /** Whether forecast hour k (0 = this hour) is daytime; false when that hour isn't available. */
    static String hourlyDay(int k) {
        return "[WEATHER.HOURS." + k + ".IS_AVAILABLE] && [WEATHER.HOURS." + k + ".IS_DAY]";
    }

    // In always-on mode the full-width bands stay at the same alpha as the GMT
    // hand (200/255, ~78%), so they read as bright as the hands. Polar, the
    // palest colourway, is the worst case for the 15% average-brightness limit.
    static final int AMBIENT_BAND_ALPHA = 200;

    /**
     * A bezel ring part: night colour all round, with day arcs drawn by body.
     * The ambient copy uses the lifted always-on night colour, since near-black
     * night colours vanish once dimmed.
     */
    static void bezelPart(boolean ambient, String zone, Runnable body) {
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\" alpha=\"%d\"", ambient ? 0 : 255);
        if (zone != null) line("<Localization timeZone=\"%s\"/>", zone);
        bandAmbient = ambient;
        arc(bandR(), 0, 360, nightColor(ambient), bandThick(), "BUTT", null);
        body.run();
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambient ? AMBIENT_BAND_ALPHA : 0);
        close("PartDraw");
    }

    static boolean bandAmbient = false;   // set by bezelPart for its day arcs

    // Always-on band: a thin ring just inside the numerals (radius 188-196)
    // instead of the full 40-unit band, so far fewer pixels stay lit.
    static final double AMBIENT_BAND_R = 192, AMBIENT_BAND_THICK = 8;

    static double bandR() { return bandAmbient ? AMBIENT_BAND_R : BEZEL_MID; }
    static double bandThick() { return bandAmbient ? AMBIENT_BAND_THICK : BEZEL_THICK; }

    static void dayArc(String startExpr, String endExpr) {
        arc(bandR(), 0, 0, bandAmbient ? "[CONFIGURATION.bezel.3]" : "[CONFIGURATION.bezel.1]", bandThick(), "BUTT",
                "<Transform target=\"startAngle\" value=\"" + xml(startExpr) + "\"/>\n"
                        + "<Transform target=\"endAngle\" value=\"" + xml(endExpr) + "\"/>");
    }

    static String nightColor(boolean ambient) {
        return ambient ? "[CONFIGURATION.bezel.2]" : "[CONFIGURATION.bezel.0]";
    }

    /** "LHR +1H", "HNL −10H", "KTM +5:45H": the airport's offset from UTC. */
    static void airportLabel(Airport a) {
        label(a.code + " %s%s%s%sH", signedHours(destOffset(a)));
    }

    /** Template parameters for a signed minute offset: sign, hours, ":" and minutes when not whole. */
    static String[] signedHours(String minutes) {
        String whole = "abs(" + minutes + ") % 60 == 0";
        return new String[] {
                // Zero reads "+0H".
                minutes + " < 0 ? \"−\" : \"+\"",
                "floor(abs(" + minutes + ") / 60)",
                whole + " ? \"\" : \":\"",
                whole + " ? \"\" : abs(" + minutes + ") % 60",
        };
    }

    /**
     * LOCAL's label: the local zone's abbreviation and its offset from UTC,
     * e.g. "MDT −6H".
     *
     * [TIMEZONE_ABB] always reports the standard-time name (MST even in
     * summer), so during daylight saving the summer name is derived from it:
     * xST -> xDT (MDT, PDT, AKDT, AEDT, NZDT), three-letter xxT -> xxST (CEST,
     * EEST, WEST, CLST), and GMT -> BST (IST in Dublin). Zones without a named
     * abbreviation ("GMT+12:45", "UTC-3", "+0545"), or summer names the rules
     * can't derive, fall back to "UTC +12:45" from the local offset.
     */
    static void localLabel() {
        String abb = "[TIMEZONE_ABB]", len = "textLength(" + abb + ")", dst = "[IS_DAYLIGHT_SAVING_TIME]";
        String unnamed = "((subText(" + abb + ", 0, 3) == \"GMT\" || subText(" + abb + ", 0, 3) == \"UTC\") && " + len + " > 3)"
                + " || subText(" + abb + ", 0, 1) == \"+\" || subText(" + abb + ", 0, 1) == \"-\"";
        String[] off = signedHours("[TIMEZONE_OFFSET_MINUTES_DST]");
        Runnable utc = () -> labelText("UTC %s%s%s%s", off);
        label(() -> chain(new String[] {
                unnamed,
                dst + " && [TIMEZONE_ID] == \"Europe/Dublin\"",
                dst + " && " + abb + " == \"GMT\"",
                dst + " && subText(" + abb + ", " + len + " - 2, " + len + ") == \"ST\"",
                dst + " && " + len + " == 3 && subText(" + abb + ", 2, 3) == \"T\"",
                dst,
        }, new Runnable[] {
                utc,
                () -> labelText("IST %s%s%s%sH", off),
                () -> labelText("BST %s%s%s%sH", off),
                () -> labelText("%sDT %s%s%s%sH", cat("subText(" + abb + ", 0, " + len + " - 2)", off)),
                () -> labelText("%sST %s%s%s%sH", cat("subText(" + abb + ", 0, 2)", off)),
                utc,
        }, () -> labelText("%s %s%s%s%sH", cat(abb, off))));
    }

    static String[] cat(String first, String[] rest) {
        String[] all = new String[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }

    /** A Condition that runs the body of the first matching expression, else the default. */
    static void chain(String[] exprs, Runnable[] bodies, Runnable otherwise) {
        open("Condition");
        open("Expressions");
        String[] names = new String[exprs.length];
        for (int i = 0; i < exprs.length; i++) {
            names[i] = "c" + (conditionCount++);
            line("%s", "<Expression name=\"" + names[i] + "\"><![CDATA[" + exprs[i] + "]]></Expression>");
        }
        close("Expressions");
        for (int i = 0; i < exprs.length; i++) {
            open("Compare expression=\"%s\"", names[i]);
            bodies[i].run();
            close("Compare");
        }
        open("Default");
        otherwise.run();
        close("Default");
        close("Condition");
    }

    // The GMT label is printed dial text just below 12 (10sp secondary size),
    // above where the hands usually sit.
    static final int LABEL_W = 160, LABEL_H = 28, LABEL_Y = 76;

    static void label(String template, String... params) {
        label(() -> labelText(template, params));
    }

    static void label(Runnable text) {
        styled(text);
        // The underline ties the label to the GMT hand: red, or Pixel's accent colour.
        switchOn("[CONFIGURATION.style] == \"pixel\"", () -> underline("[CONFIGURATION.bezel.1]"), () -> underline(GMT_RED));
    }

    static void underline(String color) {
        open("PartDraw x=\"%d\" y=\"%d\" width=\"24\" height=\"3\"", (int) (C - 12), LABEL_Y + LABEL_H + 1);
        line("<Rectangle x=\"0\" y=\"0\" width=\"24\" height=\"2\"><Fill color=\"%s\"/></Rectangle>", color);
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        close("PartDraw");
    }

    static void labelText(String template, String... params) {
        int w = LABEL_W, h = LABEL_H, x = (int) (C - w / 2), y = LABEL_Y;
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        open("Text align=\"CENTER\"");
        open(font(TEXT_SECONDARY, "MEDIUM", STEEL));
        if (params.length == 0) {
            line("<Template>%%s<Parameter expression=\"&quot;%s&quot;\"/></Template>", template);
        } else {
            StringBuilder t = new StringBuilder("<Template>").append(template);
            for (String p : params) t.append("<Parameter expression=\"").append(xml(p)).append("\"/>");
            line("%s", t.append("</Template>").toString());
        }
        close("Font");
        close("Text");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"170\"/>");
        close("PartText");
    }

    /**
     * GMT hand in the given zone, or local time when zone is null.
     */
    static void gmtHand(String zone) {
        // One turn per day on both faces, so the hand points at its time on the
        // 24-hour band (e.g. 18:15 at the 9:15 position of the 12-hour labels).
        styled(() -> {
            HandBox b = handBox(style.gmt);
            // Pixel's white GMT hand takes the colourway's day colour as its accent.
            open("PartImage x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" pivotX=\"0.5\" pivotY=\"%s\"%s", b.x, b.y, b.w, b.h, n(b.pivotY),
                    style == PIXEL ? " tintColor=\"[CONFIGURATION.bezel.1]\"" : "");
            if (zone != null) line("<Localization timeZone=\"%s\"/>", zone);
            line("<Image resource=\"%shand_gmt\"/>", style.handPrefix);
            line("<Transform target=\"angle\" value=\"([HOUR_0_23] + [MINUTE] / 60 + [SECOND] / 3600) * 15\"/>");
            line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"200\"/>");
            close("PartImage");
        });
    }

    // ---- Bezel markings (drawn above the coloured ring) --------------------

    // Numerals get a dark outline so they stay legible on light day colours
    // (e.g. Polar). The Font <Outline> decoration validates but the runtime
    // doesn't draw it, so the outline is built from offset dark copies.
    static final int[][] HALO = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1},
            {-2, 0}, {2, 0}, {0, -2}, {0, 2}};

    static void bezelMarkings() {
        comment("Bezel scale: 12h standard watch scale or 24h; Pilot uses triangles between 24h numerals, Flat ticks");
        open("Group name=\"bezel_markings\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        // Engraved inner edge; Pixel has a wider dark gap so the ring reads as its own layer.
        switchOn("[CONFIGURATION.style] == \"pixel\"", () -> {
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            arc(BEZEL_INNER, 0, 360, BG, 6, "BUTT", null);
            close("PartDraw");
        }, () -> {
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            arc(BEZEL_INNER, 0, 360, "#05070A", 1.5, "BUTT", null);
            close("PartDraw");
        });
        formatted(() -> styled(WatchFace::bezelScale));
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"120\"/>");
        close("Group");
    }

    /**
     * 12-hour: a standard watch scale, 1-11 with the lume pip at 12 and dots at
     * the half hours. 24-hour: 2, 4 ... 22 with triangles between (Pilot) or
     * 2 ... 24 with ticks between (Flat).
     */
    static void bezelScale() {
        open("Group name=\"scale_%s_%s\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"", style.id, h24 ? "24" : "12");
        if (!h24) {
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            for (int h = 0; h < 12; h++) halfHourDot(h * 30 + 15);
            close("PartDraw");
            for (int h = 1; h <= 11; h++) outlinedNumeral(Integer.toString(h), h * 30);
            pip();
        } else if (style == PILOT) {
            for (int h = 2; h <= 22; h += 2) outlinedNumeral(Integer.toString(h), h * 15);
            for (int h = 1; h <= 23; h += 2) {
                double a = h * 15;
                int w = 12, hh = 10;
                open("PartImage x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" angle=\"%s\"",
                        (int) Math.round(dx(BEZEL_MID, a) - w / 2.0), (int) Math.round(dy(BEZEL_MID, a) - hh / 2.0), w, hh, f(a));
                line("<Image resource=\"bezel_triangle\"/>");
                close("PartImage");
            }
            pip();
        } else {
            for (int h = 2; h <= 24; h += 2) outlinedNumeral(Integer.toString(h), (h % 24) * 15);
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            for (int h = 1; h <= 23; h += 2) {
                double a = h * 15;
                for (String[] layer : new String[][] {{"#0B0E14", "4.4"}, {"#F1F2F4", "2"}}) {
                    open("Line startX=\"%s\" startY=\"%s\" endX=\"%s\" endY=\"%s\"",
                            px(BEZEL_MID - 6, a), py(BEZEL_MID - 6, a), px(BEZEL_MID + 6, a), py(BEZEL_MID + 6, a));
                    line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"ROUND\"/>", layer[0], layer[1]);
                    close("Line");
                }
            }
            close("PartDraw");
        }
        close("Group");
    }

    static void halfHourDot(double a) {
        double d = 4.6;
        open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(dx(BEZEL_MID, a) - d / 2), f(dy(BEZEL_MID, a) - d / 2), f(d), f(d));
        line("<Stroke color=\"#0B0E14\" thickness=\"1.2\"/>");
        line("<Fill color=\"#E8EAED\"/>");
        close("Ellipse");
    }

    /** Lume triangle at the top of the bezel (12 or 24); a round dot on Pixel. */
    static void pip() {
        if (style == PIXEL) {
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            double d = 16, y = C - BEZEL_MID;
            open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(C - d / 2), f(y - d / 2), f(d), f(d));
            line("<Stroke color=\"#0B0E14\" thickness=\"2\"/>");
            line("<Fill color=\"#F1F2F4\"/>");
            close("Ellipse");
            close("PartDraw");
            return;
        }
        int w = 26, h = 22;
        open("PartImage x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", (int) C - w / 2, (int) Math.round(C - BEZEL_MID - h / 2.0), w, h);
        line("<Image resource=\"bezel_pip\"/>");
        close("PartImage");
    }

    static void outlinedNumeral(String text, double angle) {
        for (int[] o : HALO) numeral(text, angle, o[0], o[1], "#0B0E14");
        numeral(text, angle, 0, 0, "#F1F2F4");
    }

    static void numeral(String text, double a, int ox, int oy, String color) {
        int w = 44, hh = 32;
        int x = (int) Math.round(dx(BEZEL_MID, a) - w / 2.0) + ox, y = (int) Math.round(dy(BEZEL_MID, a) - hh / 2.0) + oy;
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" angle=\"%s\"", x, y, w, hh, f(a));
        open("Text align=\"CENTER\"");
        open(font(BEZEL_NUMERAL, "SEMI_BOLD", color));
        line("<Template>%%s<Parameter expression=\"%s\"/></Template>", text);
        close("Font");
        close("Text");
        close("PartText");
    }

    // ---- Local time hands -------------------------------------------------

    static void localHands() {
        comment("Local time: hour, minute, sweeping seconds (outline hands in ambient)");
        styled(() -> {
            open("AnalogClock x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            hand("HourHand", "hand_hour", style.hour, 255, 0);
            hand("HourHand", "hand_hour_ambient", style.hour, 0, 255);
            hand("MinuteHand", "hand_minute", style.minute, 255, 0);
            hand("MinuteHand", "hand_minute_ambient", style.minute, 0, 255);
            HandBox b = handBox(style.second);
            // Pixel's white second hand takes the colourway's day colour as its accent.
            open("SecondHand resource=\"%shand_second\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" pivotX=\"0.5\" pivotY=\"%s\"%s",
                    style.handPrefix, b.x, b.y, b.w, b.h, n(b.pivotY), style == PIXEL ? " tintColor=\"[CONFIGURATION.bezel.1]\"" : "");
            line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
            line("<Sweep frequency=\"15\"/>");
            close("SecondHand");
            close("AnalogClock");
        });
    }

    /** Where a padded hand image sits so its pivot lands on the dial centre (see HandArt.java). */
    record HandBox(int x, int y, int w, int h, double pivotY) {}

    static HandBox handBox(HandGeom g) {
        int w = g.w + 2 * HAND_PAD, h = g.len + g.tail + 2 * HAND_PAD, pivot = g.len + HAND_PAD;
        if (w % 2 != 0) throw new IllegalStateException("Hand image widths must be even to centre exactly: " + g);
        return new HandBox((int) C - w / 2, (int) C - pivot, w, h, (double) pivot / h);
    }

    static void hand(String tag, String res, HandGeom g, int alpha, int ambientAlpha) {
        HandBox b = handBox(g);
        open("%s resource=\"%s%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" pivotX=\"0.5\" pivotY=\"%s\" alpha=\"%d\"",
                tag, style.handPrefix, res, b.x, b.y, b.w, b.h, n(b.pivotY), alpha);
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambientAlpha);
        close(tag);
    }

    static void centerCap() {
        // Red centre, or the colourway's night colour on Pixel.
        switchOn("[CONFIGURATION.style] == \"pixel\"", () -> cap("[CONFIGURATION.bezel.0]"), () -> cap(GMT_RED));
    }

    static void cap(String centre) {
        open("PartDraw x=\"215\" y=\"215\" width=\"20\" height=\"20\"");
        line("<Ellipse x=\"3\" y=\"3\" width=\"14\" height=\"14\"><Fill color=\"%s\"/></Ellipse>", STEEL);
        line("<Ellipse x=\"7.5\" y=\"7.5\" width=\"5\" height=\"5\"><Fill color=\"%s\"/></Ellipse>", centre);
        close("PartDraw");
    }

    // ---- Shape helpers ----------------------------------------------------

    static void arc(double r, double start, double end, String color, double thick, String cap, String extra) {
        open("Arc centerX=\"225\" centerY=\"225\" width=\"%s\" height=\"%s\" startAngle=\"%s\" endAngle=\"%s\"",
                f(2 * r), f(2 * r), f(start), f(end));
        line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"%s\"/>", color, f(thick), cap);
        if (extra != null) for (String l : extra.split("\n")) line("%s", l);
        close("Arc");
    }

    static double dx(double r, double deg) { return C + r * Math.sin(Math.toRadians(deg)); }
    static double dy(double r, double deg) { return C - r * Math.cos(Math.toRadians(deg)); }
    static String px(double r, double deg) { return f(dx(r, deg)); }
    static String py(double r, double deg) { return f(dy(r, deg)); }

    /** Escapes an expression for use inside an XML attribute. */
    static String xml(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Coordinates: 2 decimals, trailing zeros trimmed. */
    static String f(double v) { return trim(String.format(Locale.ROOT, "%.2f", v)); }
    /** Constants in expressions: 6 decimals. */
    static String n(double v) { return trim(String.format(Locale.ROOT, "%.6f", v)); }

    static String trim(String s) {
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.equals("-0") ? "0" : s;
    }

    // ---- XML writer -------------------------------------------------------

    static void line(String fmt, Object... args) {
        out.append("  ".repeat(indent)).append(String.format(Locale.ROOT, fmt, args)).append('\n');
    }

    static void open(String fmt, Object... args) {
        line("%s", "<" + String.format(Locale.ROOT, fmt, args) + ">");
        indent++;
    }

    static void close(String tag) {
        indent--;
        line("</" + tag + ">");
    }

    static void comment(String text) { line("<!-- " + text + " -->"); }

    // ---- strings.xml ------------------------------------------------------

    static String strings() {
        StringBuilder s = new StringBuilder();
        s.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n");
        s.append("<!-- GENERATED by tools/WatchFace.java. Edit the generator, not this file. -->\n");
        s.append("<resources>\n");
        str(s, "app_name", "Solstice GMT");
        str(s, "cfg_style", "Style");
        for (Style st : STYLES) str(s, "style_" + st.id, st.name);
        str(s, "cfg_bezel_format", "Bezel");
        str(s, "bezel_format_12", "12-hour");
        str(s, "bezel_format_24", "24-hour");
        str(s, "cfg_bezel", "Bezel colourway");
        str(s, "cfg_airport", "GMT airport");
        for (Colorway c : COLORWAYS) str(s, "bezel_" + c.id, c.name);
        str(s, "airport_local", "LOCAL\nYour time zone");
        for (Airport a : AIRPORTS) str(s, "airport_" + a.code.toLowerCase(Locale.ROOT), displayName(a));
        str(s, "slot_left", "Left");
        str(s, "slot_bottom", "Bottom");
        str(s, "slot_date_tap", "Date tap action");
        str(s, "slot_label_tap", "GMT label tap action");
        s.append("</resources>\n");
        return s.toString();
    }

    static void str(StringBuilder s, String name, String value) {
        s.append("    <string name=\"").append(name).append("\">")
                .append(value.replace("&", "&amp;").replace("'", "\\'").replace("\n", "\\n")).append("</string>\n");
    }

    static void write(String path, String content) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
    }
}
