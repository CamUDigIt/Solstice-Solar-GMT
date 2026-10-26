import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Renders the hand and marker artwork for Solstice GMT as PNGs.
 *
 * Run from the project root:  java tools/HandArt.java
 *
 * Every hand is drawn pointing at 12 o'clock. Hand geometry is (w, len, tail):
 * width, pivot-to-tip and pivot-to-butt, in watch-face units (450 x 450 face).
 * Each canvas has PAD units of margin on every side for the soft dark edge, so
 * the image is (w + 2*PAD) x (len + tail + 2*PAD) with the pivot at
 * (w/2 + PAD, len + PAD). WatchFace.java uses the same numbers to place them.
 */
public class HandArt {
    static final int SS = 4;         // supersampling factor for antialiasing
    static final int PAD = 2;        // margin for the dark edge (must match WatchFace.HAND_PAD)

    static final Color STEEL = new Color(0xE9, 0xEC, 0xF0);
    static final Color STEEL_EDGE = new Color(0x8A, 0x93, 0x9E);
    static final Color LUME = new Color(0xF4, 0xF1, 0xE4);
    static final Color FLAT_WHITE = new Color(0xF4, 0xF4, 0xF2);
    static final Color GMT_RED = new Color(0xD7, 0x26, 0x26);
    static final Color OUTLINE = new Color(0xCF, 0xD4, 0xDA);

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "app/src/main/res/drawable-nodpi");
        out.mkdirs();

        // Pilot (Rolex GMT-Master II inspired). Geometry must match PILOT in WatchFace.java.
        hand(out, "hand_hour", 26, 104, 20, true, HandArt::drawMercedes);
        hand(out, "hand_minute", 14, 176, 22, true, HandArt::drawMinute);
        hand(out, "hand_second", 14, 182, 40, true, HandArt::drawSecond);
        hand(out, "hand_gmt", 30, 186, 0, true, HandArt::drawGmt);
        hand(out, "hand_hour_ambient", 26, 104, 20, false, (g, w, len, tail) -> outline(g, mercedesShape(w, len, tail)));
        hand(out, "hand_minute_ambient", 14, 176, 22, false, (g, w, len, tail) -> outline(g, minuteShape(w, len, tail)));

        // Flat (Apple Watch GMT inspired). Geometry must match FLAT in WatchFace.java.
        hand(out, "flat_hand_hour", 12, 100, 12, true, (g, w, len, tail) -> flatStick(g, w, len, tail, 22));
        hand(out, "flat_hand_minute", 10, 174, 12, true, (g, w, len, tail) -> flatStick(g, w, len, tail, 22));
        hand(out, "flat_hand_second", 10, 182, 36, true, HandArt::drawFlatSecond);
        hand(out, "flat_hand_gmt", 10, 186, 0, true, HandArt::drawFlatGmt);
        hand(out, "flat_hand_hour_ambient", 12, 100, 12, false, (g, w, len, tail) -> outline(g, stickShape(w, len, tail, 22)));
        hand(out, "flat_hand_minute_ambient", 10, 174, 12, false, (g, w, len, tail) -> outline(g, stickShape(w, len, tail, 22)));

        // Pixel (Pixel Watch inspired). Geometry must match PIXEL in WatchFace.java.
        hand(out, "pixel_hand_hour", 18, 100, 16, true, (g, w, len, tail) -> capsule(g, w, len, tail));
        hand(out, "pixel_hand_minute", 14, 172, 16, true, (g, w, len, tail) -> capsule(g, w, len, tail));
        hand(out, "pixel_hand_second", 10, 182, 30, true, HandArt::drawPixelSecond);
        hand(out, "pixel_hand_gmt", 14, 160, 0, true, HandArt::drawPixelGmt);
        hand(out, "pixel_hand_hour_ambient", 18, 100, 16, false, (g, w, len, tail) -> outline(g, capsuleShape(w, len, tail)));
        hand(out, "pixel_hand_minute_ambient", 14, 172, 16, false, (g, w, len, tail) -> outline(g, capsuleShape(w, len, tail)));

        triangle(out, "bezel_pip", 26, 22, new Color(0xB8, 0xBF, 0xC8), LUME, 3);
        triangle(out, "pilot_marker_12", 20, 18, STEEL, LUME, 2.2);
        triangle(out, "bezel_triangle", 12, 10, new Color(0x0B, 0x0E, 0x14), new Color(0xF1, 0xF2, 0xF4), 1.4);
        placeholderPreview(out);
        System.out.println("Wrote hand artwork to " + out.getPath());
    }

    interface Painter { void paint(Graphics2D g, double w, double len, double tail); }

    /** Draws a hand into its padded canvas; `edge` adds the soft dark edge. */
    static void hand(File dir, String name, int w, int len, int tail, boolean edge, Painter p) throws Exception {
        int cw = w + 2 * PAD, ch = len + tail + 2 * PAD;
        BufferedImage art = new BufferedImage(cw * SS, ch * SS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = art.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(SS, SS);
        g.translate(PAD, PAD);
        p.paint(g, w, len, tail);
        g.dispose();

        BufferedImage img = art;
        if (edge) {
            // Dark silhouette drawn at small offsets underneath = a soft dark edge.
            BufferedImage shadow = silhouette(art, new Color(0, 0, 0, 150));
            img = new BufferedImage(art.getWidth(), art.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D c = img.createGraphics();
            int r = (int) Math.round(1.3 * SS);
            for (int dx = -r; dx <= r; dx += 2) {
                for (int dy = -r; dy <= r; dy += 2) {
                    if (dx * dx + dy * dy <= r * r) c.drawImage(shadow, dx, dy, null);
                }
            }
            c.drawImage(art, 0, 0, null);
            c.dispose();
        }
        write(downsample(img, cw, ch), new File(dir, name + ".png"));
        System.out.printf("%-26s %3dx%-3d pivotY=%.4f%n", name, cw, ch, (double) (len + PAD) / ch);
    }

    static BufferedImage silhouette(BufferedImage src, Color c) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        int rgb = c.getRGB() & 0xFFFFFF;
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int a = (src.getRGB(x, y) >>> 24) * c.getAlpha() / 255;
                if (a > 0) out.setRGB(x, y, (a << 24) | rgb);
            }
        }
        return out;
    }

    // ---- Pilot ------------------------------------------------------------

    static final double MERC_CY = 30, MERC_R = 11;

    /** Broad hour hand with a pointed tip and the open "Mercedes" circle near it. */
    static Shape mercedesShape(double w, double len, double tail) {
        double c = w / 2;
        Area a = new Area();
        Path2D tip = new Path2D.Double();
        tip.moveTo(c, 0);
        tip.lineTo(c + 6, MERC_CY - MERC_R + 3);
        tip.lineTo(c - 6, MERC_CY - MERC_R + 3);
        tip.closePath();
        a.add(new Area(tip));
        a.add(new Area(new Ellipse2D.Double(c - MERC_R, MERC_CY - MERC_R, 2 * MERC_R, 2 * MERC_R)));
        Path2D shaft = new Path2D.Double();
        shaft.moveTo(c - 4.5, MERC_CY + MERC_R - 2);
        shaft.lineTo(c + 4.5, MERC_CY + MERC_R - 2);
        shaft.lineTo(c + 5.5, len + tail);
        shaft.lineTo(c - 5.5, len + tail);
        shaft.closePath();
        a.add(new Area(shaft));
        return a;
    }

    static void drawMercedes(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        Shape body = mercedesShape(w, len, tail);
        g.setPaint(new GradientPaint(0, 0, STEEL_EDGE, (float) c, 0, STEEL, true));
        g.fill(body);
        g.setColor(STEEL_EDGE);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(body);
        g.setColor(LUME);
        // Lume in the tip, the circle (behind the three spokes) and the shaft.
        Path2D tip = new Path2D.Double();
        tip.moveTo(c, 5);
        tip.lineTo(c + 3.2, MERC_CY - MERC_R + 1);
        tip.lineTo(c - 3.2, MERC_CY - MERC_R + 1);
        tip.closePath();
        g.fill(tip);
        double ir = MERC_R - 2.4;
        g.fill(new Ellipse2D.Double(c - ir, MERC_CY - ir, 2 * ir, 2 * ir));
        g.fill(new Rectangle2D.Double(c - 2.5, MERC_CY + MERC_R + 3, 5, len - MERC_CY - MERC_R - 18));
        g.setColor(STEEL);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
        for (double deg : new double[] {90, 210, 330}) {
            double rad = Math.toRadians(deg);
            g.draw(new Line2D.Double(c, MERC_CY, c + ir * Math.cos(rad), MERC_CY + ir * Math.sin(rad)));
        }
    }

    static Shape minuteShape(double w, double len, double tail) {
        double c = w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(c, 0);
        p.lineTo(c + w * 0.45, 26);
        p.lineTo(c + w * 0.38, len + tail);
        p.lineTo(c - w * 0.38, len + tail);
        p.lineTo(c - w * 0.45, 26);
        p.closePath();
        return p;
    }

    static void drawMinute(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        Shape body = minuteShape(w, len, tail);
        g.setPaint(new GradientPaint(0, 0, STEEL_EDGE, (float) c, 0, STEEL, true));
        g.fill(body);
        g.setColor(STEEL_EDGE);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(body);
        Path2D lume = new Path2D.Double();
        lume.moveTo(c, 18);
        lume.lineTo(c + w * 0.24, 30);
        lume.lineTo(c + w * 0.2, len - 30);
        lume.lineTo(c - w * 0.2, len - 30);
        lume.lineTo(c - w * 0.24, 30);
        lume.closePath();
        g.setColor(LUME);
        g.fill(lume);
    }

    static void drawSecond(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new RoundRectangle2D.Double(c - 1.2, 2, 2.4, len + tail - 4, 2, 2));
        g.fill(new RoundRectangle2D.Double(c - 2.6, len + 14, 5.2, tail - 16, 3, 3));
        double ly = 44, r = 6.5;
        g.fill(new Ellipse2D.Double(c - r, ly - r, 2 * r, 2 * r));
    }

    /** Thicker stem with a large outlined triangle tip. */
    static void drawGmt(Graphics2D g, double w, double len, double tail) {
        double c = w / 2, th = 32;
        g.setColor(GMT_RED);
        g.fill(new Rectangle2D.Double(c - 2, th - 2, 4, len - th + 2));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(c, 0);
        arrow.lineTo(w, th);
        arrow.lineTo(0, th);
        arrow.closePath();
        g.fill(arrow);
    }

    // ---- Flat -------------------------------------------------------------

    /** Rounded stick with a thin neck of `neck` units next to the pivot. */
    static Shape stickShape(double w, double len, double tail, double neck) {
        double c = w / 2, nw = 3.2;
        Area a = new Area(new RoundRectangle2D.Double(0, 0, w, len - neck, w, w));
        a.add(new Area(new Rectangle2D.Double(c - nw / 2, len - neck - w / 2, nw, neck + w / 2)));
        a.add(new Area(new RoundRectangle2D.Double(c - nw / 2, len - nw / 2, nw, tail + nw / 2, nw, nw)));
        return a;
    }

    static void flatStick(Graphics2D g, double w, double len, double tail, double neck) {
        g.setColor(FLAT_WHITE);
        g.fill(stickShape(w, len, tail, neck));
    }

    static void drawFlatSecond(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new RoundRectangle2D.Double(c - 1.1, 0, 2.2, len + tail, 2.2, 2.2));
        g.fill(new RoundRectangle2D.Double(c - 2.4, len + 10, 4.8, tail - 10, 4.8, 4.8));
    }

    static void drawFlatGmt(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new Rectangle2D.Double(c - 1.8, 20, 3.6, len - 20));
        g.fill(new RoundRectangle2D.Double(c - 4, 0, 8, 26, 8, 8));
    }

    // ---- Pixel ------------------------------------------------------------

    /** Solid capsule with fully rounded ends, running through the pivot. */
    static Shape capsuleShape(double w, double len, double tail) {
        return new RoundRectangle2D.Double(0, 0, w, len + tail, w, w);
    }

    static void capsule(Graphics2D g, double w, double len, double tail) {
        g.setColor(FLAT_WHITE);
        g.fill(capsuleShape(w, len, tail));
    }

    /** Thin rounded line, drawn white so WatchFace can tint it with the colourway. */
    static void drawPixelSecond(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(c - 1.3, 0, 2.6, len + tail, 2.6, 2.6));
    }

    /** Thin stem ending in a round dot, drawn white so WatchFace can tint it with the colourway. */
    static void drawPixelGmt(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(c - 2, w / 2, 4, len - w / 2, 4, 4));
        g.fill(new Ellipse2D.Double(0, 0, w, w));
    }

    // ---- Shared -----------------------------------------------------------

    /** Low-power outline for ambient mode. */
    static void outline(Graphics2D g, Shape s) {
        g.setColor(OUTLINE);
        g.setStroke(new BasicStroke(1.4f));
        g.draw(s);
    }

    /** Downward-pointing triangle (points at the dial centre when placed at 12). */
    static void triangle(File dir, String name, int w, int h, Color rim, Color fill, double inset) throws Exception {
        BufferedImage img = new BufferedImage(w * SS, h * SS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(SS, SS);
        Path2D t = new Path2D.Double();
        t.moveTo(0.5, 0.5);
        t.lineTo(w - 0.5, 0.5);
        t.lineTo(w / 2.0, h - 0.5);
        t.closePath();
        g.setColor(rim);
        g.fill(t);
        Path2D in = new Path2D.Double();
        in.moveTo(inset * 1.6, inset);
        in.lineTo(w - inset * 1.6, inset);
        in.lineTo(w / 2.0, h - inset * 1.9);
        in.closePath();
        g.setColor(fill);
        g.fill(in);
        g.dispose();
        write(downsample(img, w, h), new File(dir, name + ".png"));
    }

    /** Stand-in until preview.png is replaced with a real on-watch screenshot. */
    static void placeholderPreview(File dir) throws Exception {
        File f = new File(dir, "preview.png");
        if (f.exists()) return;
        int s = 450;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x1D4ED8));
        g.fill(new Ellipse2D.Double(0, 0, s, s));
        g.setColor(new Color(0xC81E1E));
        g.fill(new Arc2D.Double(0, 0, s, s, 90, -180, Arc2D.PIE));
        g.setColor(new Color(0x0B0E14));
        g.fill(new Ellipse2D.Double(36, 36, s - 72, s - 72));
        g.dispose();
        write(img, f);
    }

    // ---- io ---------------------------------------------------------------

    static BufferedImage downsample(BufferedImage src, int w, int h) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING), 0, 0, null);
        g.dispose();
        return dst;
    }

    static void write(BufferedImage img, File f) throws Exception {
        ImageIO.write(img, "png", f);
    }
}
