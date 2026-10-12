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
    // editor moves steadily around the globe. UTC stays first.
    static final List<Airport> AIRPORTS = List.of(
            new Airport("UTC", "Coordinated Universal Time", "Etc/UTC", 51.48, 0.0),
            // UTC-10 .. UTC-3
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
            new Airport("GRU", "Sao Paulo", "America/Sao_Paulo", -23.43, -46.47),
            new Airport("EZE", "Buenos Aires", "America/Argentina/Buenos_Aires", -34.82, -58.54),
            // UTC+0 .. UTC+4
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
            new Airport("DXB", "Dubai", "Asia/Dubai", 25.25, 55.36),
            // UTC+5:30 .. UTC+13
            new Airport("DEL", "Delhi", "Asia/Kolkata", 28.56, 77.10),
            new Airport("BOM", "Mumbai", "Asia/Kolkata", 19.09, 72.87),
            new Airport("KTM", "Kathmandu", "Asia/Kathmandu", 27.70, 85.36),
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
            new Airport("AKL", "Auckland", "Pacific/Auckland", -37.01, 174.79));

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
    // Colours: [0] night, [1] day, [2] night in always-on mode. The thin
    // always-on ring sits on pure black, so near-black night colours are lifted
    // there to keep the whole ring visible.

    record Colorway(String id, String name, String night, String day, String ambientNight) {}

    static final List<Colorway> COLORWAYS = List.of(
            new Colorway("pepsi", "Pepsi", "#1D4ED8", "#C81E1E", "#1D4ED8"),
            new Colorway("batman", "Batman", "#0B0B0E", "#2563EB", "#52525B"),
            new Colorway("coke", "Coke", "#111318", "#B91C1C", "#52525B"),
            new Colorway("rootbeer", "Root Beer", "#3A2A1F", "#B7791F", "#7C5A3F"),
            new Colorway("golden_hour", "Golden Hour", "#2E1065", "#EA580C", "#6D28D9"),
            new Colorway("polar", "Polar", "#0F172A", "#CBD5E1", "#475569"));

    // ---- Palette ----------------------------------------------------------

    static final String BG = "#000000";
    static final String DIAL_HI = "#161B23";
    static final String DIAL_LO = "#07090C";
    static final String STEEL = "#C9CED6";
    static final String LUME = "#F4F1E4";
    static final String MUTED = "#8A93A0";
    static final String GMT_RED = "#D72626";

    // ---- Geometry ---------------------------------------------------------

    static final double C = 225;
    static final double BEZEL_OUTER = 225, BEZEL_INNER = 198;
    static final double BEZEL_MID = (BEZEL_OUTER + BEZEL_INNER) / 2;
    static final double BEZEL_THICK = BEZEL_OUTER - BEZEL_INNER;
    static final double DIAL_R = 196;
    static final double COMP_R = 166;   // radius of the arc complications (just inside the minute track)

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
        Colorway pepsi = COLORWAYS.get(0);
        icon(dir, "icon_cfg_bezel", g -> bezelSwatch(g, pepsi));
        for (Colorway c : COLORWAYS) icon(dir, "icon_bezel_" + c.id, g -> bezelSwatch(g, c));
        icon(dir, "icon_cfg_airport", WatchFace::globe);
        icon(dir, "icon_airport_local", g -> codeBadge(g, "LOCAL", 22));
        for (Airport a : AIRPORTS) {
            icon(dir, "icon_airport_" + a.code.toLowerCase(Locale.ROOT), g -> codeBadge(g, a.code, 30));
        }
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

    /** Dark disc with the airport code and a red GMT underline. */
    static void codeBadge(java.awt.Graphics2D g, String code, int size) {
        g.setColor(color("#0B0E14"));
        g.fill(new java.awt.geom.Ellipse2D.Double(4, 4, ICON - 8, ICON - 8));
        g.setColor(color("#3A424D"));
        g.setStroke(new java.awt.BasicStroke(2f));
        g.draw(new java.awt.geom.Ellipse2D.Double(5, 5, ICON - 10, ICON - 10));
        g.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, size));
        var fm = g.getFontMetrics();
        g.setColor(color("#F1F2F4"));
        g.drawString(code, (ICON - fm.stringWidth(code)) / 2f, ICON / 2f + fm.getAscent() / 2f - 4);
        g.setColor(color(GMT_RED));
        g.fill(new java.awt.geom.Rectangle2D.Double(ICON / 2.0 - 12, ICON / 2.0 + fm.getAscent() / 2.0 + 2, 24, 3));
    }

    // ---- User configuration -----------------------------------------------

    static void userConfigurations() {
        open("UserConfigurations");
        open("ColorConfiguration id=\"bezel\" displayName=\"cfg_bezel\" screenReaderText=\"cfg_bezel\" icon=\"icon_cfg_bezel\" defaultValue=\"pepsi\"");
        for (Colorway c : COLORWAYS) {
            line("<ColorOption id=\"%s\" displayName=\"bezel_%s\" icon=\"icon_bezel_%s\" colors=\"%s %s %s\"/>",
                    c.id, c.id, c.id, c.night, c.day, c.ambientNight);
        }
        close("ColorConfiguration");
        open("ListConfiguration id=\"airport\" displayName=\"cfg_airport\" screenReaderText=\"cfg_airport\" icon=\"icon_cfg_airport\" defaultValue=\"%s\"", DEFAULT_AIRPORT);
        line("<ListOption id=\"%s\" displayName=\"airport_local\" icon=\"icon_airport_local\"/>", LOCAL);
        for (Airport a : AIRPORTS) {
            String id = a.code.toLowerCase(Locale.ROOT);
            line("<ListOption id=\"%s\" displayName=\"airport_%s\" icon=\"icon_airport_%s\"/>", a.code, id, id);
        }
        close("ListConfiguration");
        close("UserConfigurations");
    }

    // ---- Static dial ------------------------------------------------------

    static void dial() {
        comment("Dial: sunray-style radial gradient and minute track");
        open("Group name=\"dial\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(C - DIAL_R), f(C - DIAL_R), f(2 * DIAL_R), f(2 * DIAL_R));
        open("Fill color=\"%s\"", DIAL_LO);
        line("<RadialGradient centerX=\"225\" centerY=\"200\" radius=\"%s\" colors=\"%s %s\" positions=\"0 1\"/>", f(DIAL_R + 20), DIAL_HI, DIAL_LO);
        close("Fill");
        close("Ellipse");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        close("PartDraw");

        // Minute track: 60 ticks, heavier every 5 minutes.
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        for (int m = 0; m < 60; m++) {
            boolean five = m % 5 == 0;
            double a = m * 6;
            double r1 = five ? 184 : 188, r2 = 194;
            open("Line startX=\"%s\" startY=\"%s\" endX=\"%s\" endY=\"%s\"", px(r1, a), py(r1, a), px(r2, a), py(r2, a));
            line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"BUTT\"/>", five ? STEEL : MUTED, five ? "2" : "1");
            close("Line");
        }
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"110\"/>");
        close("PartDraw");
        close("Group");
    }

    static void dateWindow() {
        comment("Date window at 3 o'clock");
        int x = 366, y = 210, w = 40, h = 30;
        open("Group name=\"date\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"", w, h);
        open("RoundRectangle x=\"1\" y=\"1\" width=\"%d\" height=\"%d\" cornerRadiusX=\"3\" cornerRadiusY=\"3\"", w - 2, h - 2);
        line("<Stroke color=\"%s\" thickness=\"2\"/>", STEEL);
        line("<Fill color=\"#F5F5F2\"/>");
        close("RoundRectangle");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        close("PartDraw");
        // Active: dark numerals on white. Ambient: light numerals on black.
        dateText(w, h, "#111111", "<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>", 255);
        dateText(w, h, STEEL, "<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"255\"/>", 0);
        close("Group");
    }

    static void dateText(int w, int h, String color, String variant, int alpha) {
        open("PartText x=\"0\" y=\"2\" width=\"%d\" height=\"%d\" alpha=\"%d\"", w, h - 4, alpha);
        open("Text align=\"CENTER\"");
        open("Font family=\"SYNC_TO_DEVICE\" size=\"21\" weight=\"BOLD\" color=\"%s\"", color);
        line("<Template>%%s<Parameter expression=\"[DAY]\"/></Template>");
        close("Font");
        close("Text");
        line(variant);
        close("PartText");
    }

    // ---- Complications ----------------------------------------------------

    record ArcSlot(int id, String name, double start, double end, String color,
                   String provider, String providerType) {}

    static final List<ArcSlot> ARC_SLOTS = List.of(
            new ArcSlot(101, "slot_top_left", 280, 330, "#10B981", "WATCH_BATTERY", "RANGED_VALUE"),
            new ArcSlot(102, "slot_top_right", 30, 80, "#F59E0B", "SUNRISE_SUNSET", "SHORT_TEXT"),
            new ArcSlot(103, "slot_bottom_right", 100, 150, "#F43F5E", "HEART_RATE", "SHORT_TEXT"),
            new ArcSlot(104, "slot_bottom_left", 210, 260, "#E2E8F0", "STEP_COUNT", "SHORT_TEXT"));

    static void complications() {
        comment("Arc complications in the four quadrants");
        for (ArcSlot s : ARC_SLOTS) arcSlot(s);
        comment("6 o'clock long-text complication");
        longTextSlot();
    }

    static void arcSlot(ArcSlot s) {
        double mid = (s.start + s.end) / 2;
        String sweep = f(s.end - s.start);
        open("ComplicationSlot slotId=\"%d\" displayName=\"%s\" x=\"0\" y=\"0\" width=\"450\" height=\"450\" supportedTypes=\"RANGED_VALUE GOAL_PROGRESS SHORT_TEXT EMPTY\"", s.id, s.name);
        line("<DefaultProviderPolicy defaultSystemProvider=\"%s\" defaultSystemProviderType=\"%s\"/>", s.provider, s.providerType);
        line("<BoundingArc centerX=\"225\" centerY=\"225\" width=\"%s\" height=\"%s\" thickness=\"44\" startAngle=\"%s\" endAngle=\"%s\"/>",
                f(2 * COMP_R - 4), f(2 * COMP_R - 4), f(s.start), f(s.end));

        String ranged = "clamp(([COMPLICATION.RANGED_VALUE_VALUE] - [COMPLICATION.RANGED_VALUE_MIN]) / ([COMPLICATION.RANGED_VALUE_MAX] - [COMPLICATION.RANGED_VALUE_MIN]), 0, 1)";
        String goal = "clamp([COMPLICATION.GOAL_PROGRESS_VALUE] / [COMPLICATION.GOAL_PROGRESS_TARGET_VALUE], 0, 1)";

        for (String type : List.of("RANGED_VALUE", "GOAL_PROGRESS", "SHORT_TEXT")) {
            open("Complication type=\"%s\"", type);
            open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
            // The runtime draws strokes inside complications at roughly 0.4x the
            // declared thickness (seen on the Wear OS 6 emulator): 12 renders ~5 px.
            arc(COMP_R, s.start, s.end, "#2A313B", 12, "ROUND", null);
            if (!type.equals("SHORT_TEXT")) {
                String frac = type.equals("RANGED_VALUE") ? ranged : goal;
                arc(COMP_R, s.start, s.end, s.color, 12, "ROUND",
                        "<Transform target=\"endAngle\" value=\"" + f(s.start) + " + " + frac + " * " + sweep + "\"/>");
            }
            close("PartDraw");
            compText(mid, type.equals("SHORT_TEXT") ? STEEL : s.color);
            close("Complication");
        }
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"150\"/>");
        close("ComplicationSlot");
    }

    /** Small value label tucked just inside the arc's midpoint. */
    static void compText(double mid, String color) {
        double r = COMP_R - 24;
        int w = 60, h = 20;
        int x = (int) Math.round(dx(r, mid) - w / 2.0), y = (int) Math.round(dy(r, mid) - h / 2.0);
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        open("Text align=\"CENTER\" ellipsis=\"TRUE\"");
        open("Font family=\"SYNC_TO_DEVICE\" size=\"15\" weight=\"MEDIUM\" width=\"CONDENSED\" color=\"%s\"", color);
        line("<Template>%%s<Parameter expression=\"[COMPLICATION.TEXT]\"/></Template>");
        close("Font");
        close("Text");
        close("PartText");
    }

    static void longTextSlot() {
        int x = 160, y = 300, w = 130, h = 44;
        open("ComplicationSlot slotId=\"301\" displayName=\"slot_bottom\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" supportedTypes=\"LONG_TEXT SHORT_TEXT EMPTY\"", x, y, w, h);
        line("<DefaultProviderPolicy defaultSystemProvider=\"NEXT_EVENT\" defaultSystemProviderType=\"LONG_TEXT\"/>");
        line("<BoundingRoundBox x=\"0\" y=\"0\" width=\"%d\" height=\"%d\" cornerRadius=\"8\"/>", w, h);
        for (String type : List.of("LONG_TEXT", "SHORT_TEXT")) {
            open("Complication type=\"%s\"", type);
            open("PartDraw x=\"0\" y=\"0\" width=\"%d\" height=\"%d\"", w, h);
            open("RoundRectangle x=\"0.5\" y=\"0.5\" width=\"%d\" height=\"%d\" cornerRadiusX=\"8\" cornerRadiusY=\"8\"", w - 1, h - 1);
            line("<Stroke color=\"#334155\" thickness=\"1\"/>");
            line("<Fill color=\"#0A0D12\"/>");
            close("RoundRectangle");
            line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
            close("PartDraw");
            textLine(4, 3, w - 8, 20, 16, "MEDIUM", "#FFFFFF", "[COMPLICATION.TEXT]");
            textLine(4, 23, w - 8, 17, 12, "NORMAL", "#7DD3FC", "[COMPLICATION.TITLE]");
            close("Complication");
        }
        close("ComplicationSlot");
    }

    static void textLine(int x, int y, int w, int h, int size, String weight, String color, String expr) {
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        open("Text align=\"CENTER\" ellipsis=\"TRUE\"");
        open("Font family=\"SYNC_TO_DEVICE\" size=\"%d\" weight=\"%s\" color=\"%s\"", size, weight, color);
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
        label("LOCAL");
        open("AnalogClock x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        hand("HourHand", "hand_gmt", 22, 194, 0, 255, 200);
        close("AnalogClock");
        close("Group");
        close("ListOption");
        for (Airport a : AIRPORTS) {
            open("ListOption id=\"%s\"", a.code);
            open("Group name=\"airport_%s\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"", a.code);
            solarBezel(a, false);
            solarBezel(a, true);
            airportLabel(a);
            gmtHand(a);
            close("Group");
            close("ListOption");
        }
        close("ListConfiguration");
    }

    /**
     * The bezel is a rolling 12-hour window read against the GMT hand: the
     * hand always sits on "now" at the airport, and moving clockwise from it
     * the ring shows day colour for the hours that will be daylight there.
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
        String noonMin = "(720 - 4 * " + n(a.lon) + " - " + eqTime + " + [TIMEZONE_OFFSET_MINUTES_DST])";
        String sunrise = "((" + noonMin + " - " + halfDayMin + ") / 60)";
        String sunset = "((" + noonMin + " + " + halfDayMin + ") / 60)";
        String now = "([HOUR_0_23] + [MINUTE] / 60)";
        String end = "(" + now + " + 12)";

        double thick = ambient ? 3 : BEZEL_THICK;
        double r = ambient ? BEZEL_INNER + 2 : BEZEL_MID;
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\" alpha=\"%d\"", ambient ? 0 : 255);
        line("<Localization timeZone=\"%s\"/>", a.zone);
        arc(r, 0, 360, nightColor(ambient), thick, "BUTT", null);
        // Daylight intervals for today and tomorrow, clipped to the window. (Yesterday's
        // can only reach into the window if sunset is after midnight, which never
        // happens at these airports.)
        for (int day = 0; day <= 1; day++) {
            String shift = day == 0 ? "" : " + 24";
            String s = "clamp(" + sunrise + shift + ", " + now + ", " + end + ") * 30";
            String e = "clamp(" + sunset + shift + ", " + now + ", " + end + ") * 30";
            arc(r, 0, 0, "[CONFIGURATION.bezel.1]", thick, "BUTT",
                    "<Transform target=\"startAngle\" value=\"" + s + "\"/>\n"
                            + "<Transform target=\"endAngle\" value=\"" + e + "\"/>");
        }
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambient ? 255 : 0);
        close("PartDraw");
    }

    static String nightColor(boolean ambient) {
        return ambient ? "[CONFIGURATION.bezel.2]" : "[CONFIGURATION.bezel.0]";
    }

    /**
     * LOCAL's bezel: the watch has no location source in WFF, so daylight comes
     * from the hourly weather forecast's is-day flag for the wearer's location.
     * Each clock hour in the 12-hour window is one segment; with no weather data
     * the ring shows night colour only.
     */
    static void weatherBezel(boolean ambient) {
        String now = "([HOUR_0_23] + [MINUTE] / 60)";
        String end = "(" + now + " + 12)";
        double thick = ambient ? 3 : BEZEL_THICK;
        double r = ambient ? BEZEL_INNER + 2 : BEZEL_MID;
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\" alpha=\"%d\"", ambient ? 0 : 255);
        arc(r, 0, 360, nightColor(ambient), thick, "BUTT", null);
        for (int k = 0; k <= 12; k++) {
            String src = k == 0 ? "WEATHER" : "WEATHER.HOURS." + k;
            String isDay = "[" + src + ".IS_AVAILABLE] && [" + src + ".IS_DAY]";
            String s = "clamp(floor(" + now + ") + " + k + ", " + now + ", " + end + ") * 30";
            String e = "clamp(floor(" + now + ") + " + (k + 1) + ", " + now + ", " + end + ") * 30";
            arc(r, 0, 0, "[CONFIGURATION.bezel.1]", thick, "BUTT",
                    "<Transform target=\"startAngle\" value=\"" + xml(s) + "\"/>\n"
                            + "<Transform target=\"endAngle\" value=\"" + xml("(" + isDay + ") ? " + e + " : " + s) + "\"/>");
        }
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambient ? 255 : 0);
        close("PartDraw");
    }

    static void airportLabel(Airport a) {
        label(a.code + " %s%s%s%sH", offsetParams(destOffset(a)));
    }

    /**
     * Template parameters for "+7H" / "-3H" / "+5:30H": hours ahead of (or
     * behind) the wearer's local time. Not localised, so
     * [TIMEZONE_OFFSET_MINUTES_DST] is the wearer's offset.
     */
    static String[] offsetParams(String dest) {
        String diff = "(" + dest + " - [TIMEZONE_OFFSET_MINUTES_DST])";
        String whole = "abs(" + diff + ") % 60 == 0";
        return new String[] {
                diff + " > 0 ? \"+\" : (" + diff + " < 0 ? \"−\" : \"\")",
                "floor(abs(" + diff + ") / 60)",
                whole + " ? \"\" : \":\"",
                whole + " ? \"\" : abs(" + diff + ") % 60",
        };
    }

    /** The label above the dial centre, with an optional parameterised template. */
    static void label(String template, String... params) {
        int w = 130, h = 24, x = (int) (C - w / 2), y = 120;
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\"", x, y, w, h);
        open("Text align=\"CENTER\"");
        open("Font family=\"SYNC_TO_DEVICE\" size=\"17\" weight=\"MEDIUM\" width=\"CONDENSED\" letterSpacing=\"0.08\" color=\"%s\"", STEEL);
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
        // Red underline ties the label to the GMT hand.
        open("PartDraw x=\"%d\" y=\"%d\" width=\"28\" height=\"3\"", (int) (C - 14), y + h + 1);
        line("<Rectangle x=\"0\" y=\"0\" width=\"28\" height=\"2\"><Fill color=\"%s\"/></Rectangle>", GMT_RED);
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        close("PartDraw");
    }

    static void gmtHand(Airport a) {
        open("AnalogClock x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        line("<Localization timeZone=\"%s\"/>", a.zone);
        hand("HourHand", "hand_gmt", 22, 194, 0, 255, 200);
        close("AnalogClock");
    }

    // ---- Bezel markings (drawn above the coloured ring) --------------------

    static void bezelMarkings() {
        comment("Bezel: 12-hour numerals, half-hour dots and the lume pip at 12");
        open("Group name=\"bezel_markings\" x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        // Engraved inner edge.
        open("PartDraw x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        arc(BEZEL_INNER, 0, 360, "#05070A", 1.5, "BUTT", null);
        for (int h = 0; h < 12; h++) {
            double a = h * 30 + 15, d = 3.6;
            open("Ellipse x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\"", f(dx(BEZEL_MID, a) - d / 2), f(dy(BEZEL_MID, a) - d / 2), f(d), f(d));
            line("<Stroke color=\"#0B0E14\" thickness=\"1.2\"/>");
            line("<Fill color=\"#E8EAED\"/>");
            close("Ellipse");
        }
        close("PartDraw");
        // Numerals get a dark outline so they stay legible on light day colours
        // (e.g. Polar). The Font <Outline> decoration validates but the runtime
        // doesn't draw it, so the outline is built from offset dark copies.
        int[][] halo = {{-1, -1}, {0, -1}, {1, -1}, {-1, 0}, {1, 0}, {-1, 1}, {0, 1}, {1, 1},
                {-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int h = 1; h <= 11; h++) {
            for (int[] o : halo) numeral(h, o[0], o[1], "#0B0E14");
            numeral(h, 0, 0, "#F1F2F4");
        }
        open("PartImage x=\"214\" y=\"3\" width=\"22\" height=\"18\"");
        line("<Image resource=\"bezel_pip\"/>");
        close("PartImage");
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"120\"/>");
        close("Group");
    }

    static void numeral(int h, int ox, int oy, String color) {
        double a = h * 30;
        int w = 30, hh = 24;
        int x = (int) Math.round(dx(BEZEL_MID, a) - w / 2.0) + ox, y = (int) Math.round(dy(BEZEL_MID, a) - hh / 2.0) + oy;
        open("PartText x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" angle=\"%s\"", x, y, w, hh, f(a));
        open("Text align=\"CENTER\"");
        open("Font family=\"SYNC_TO_DEVICE\" size=\"19\" weight=\"SEMI_BOLD\" width=\"CONDENSED\" color=\"%s\"", color);
        line("<Template>%%s<Parameter expression=\"%d\"/></Template>", h);
        close("Font");
        close("Text");
        close("PartText");
    }

    // ---- Local time hands -------------------------------------------------

    static void localHands() {
        comment("Local time: hour, minute, sweeping seconds (outline hands in ambient)");
        open("AnalogClock x=\"0\" y=\"0\" width=\"450\" height=\"450\"");
        hand("HourHand", "hand_hour", 24, 106, 22, 255, 0);
        hand("HourHand", "hand_hour_ambient", 24, 106, 22, 0, 255);
        hand("MinuteHand", "hand_minute", 14, 178, 24, 255, 0);
        hand("MinuteHand", "hand_minute_ambient", 14, 178, 24, 0, 255);
        int sw = 14, sl = 186, st = 42;
        open("SecondHand resource=\"hand_second\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" pivotX=\"0.5\" pivotY=\"%s\"",
                (int) C - sw / 2, (int) C - sl, sw, sl + st, n((double) sl / (sl + st)));
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"0\"/>");
        line("<Sweep frequency=\"15\"/>");
        close("SecondHand");
        close("AnalogClock");
    }

    /** A hand image whose pivot is `len` px down from its tip; geometry matches HandArt.java. */
    static void hand(String tag, String res, int w, int len, int tail, int alpha, int ambientAlpha) {
        open("%s resource=\"%s\" x=\"%d\" y=\"%d\" width=\"%d\" height=\"%d\" pivotX=\"0.5\" pivotY=\"%s\" alpha=\"%d\"",
                tag, res, (int) C - w / 2, (int) C - len, w, len + tail, n((double) len / (len + tail)), alpha);
        line("<Variant mode=\"AMBIENT\" target=\"alpha\" value=\"%d\"/>", ambientAlpha);
        close(tag);
    }

    static void centerCap() {
        open("PartDraw x=\"215\" y=\"215\" width=\"20\" height=\"20\"");
        line("<Ellipse x=\"3\" y=\"3\" width=\"14\" height=\"14\"><Fill color=\"%s\"/></Ellipse>", STEEL);
        line("<Ellipse x=\"7.5\" y=\"7.5\" width=\"5\" height=\"5\"><Fill color=\"%s\"/></Ellipse>", GMT_RED);
        close("PartDraw");
    }

    // ---- Shape helpers ----------------------------------------------------

    static void arc(double r, double start, double end, String color, double thick, String cap, String extra) {
        open("Arc centerX=\"225\" centerY=\"225\" width=\"%s\" height=\"%s\" startAngle=\"%s\" endAngle=\"%s\"",
                f(2 * r), f(2 * r), f(start), f(end));
        line("<Stroke color=\"%s\" thickness=\"%s\" cap=\"%s\"/>", color, f(thick), cap);
        if (extra != null) for (String l : extra.split("\n")) line(l);
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
        str(s, "cfg_bezel", "Bezel colourway");
        str(s, "cfg_airport", "GMT airport");
        for (Colorway c : COLORWAYS) str(s, "bezel_" + c.id, c.name);
        str(s, "airport_local", "LOCAL · Your time zone");
        for (Airport a : AIRPORTS) str(s, "airport_" + a.code.toLowerCase(Locale.ROOT), a.code + " · " + a.city);
        str(s, "slot_top_left", "Top left arc");
        str(s, "slot_top_right", "Top right arc");
        str(s, "slot_bottom_right", "Bottom right arc");
        str(s, "slot_bottom_left", "Bottom left arc");
        str(s, "slot_bottom", "Bottom");
        s.append("</resources>\n");
        return s.toString();
    }

    static void str(StringBuilder s, String name, String value) {
        s.append("    <string name=\"").append(name).append("\">")
                .append(value.replace("&", "&amp;").replace("'", "\\'")).append("</string>\n");
    }

    static void write(String path, String content) throws Exception {
        File f = new File(path);
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), content, StandardCharsets.UTF_8);
    }
}
