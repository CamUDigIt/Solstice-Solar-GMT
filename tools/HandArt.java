import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Renders the hand and bezel-pip artwork for Solstice GMT as PNGs.
 *
 * Run from the project root:  java tools/HandArt.java
 *
 * Every hand is drawn pointing at 12 o'clock in a tight canvas. The pivot
 * (dial centre) is at (w/2, pivotY) in that canvas; WatchFace.java places
 * each image so that point lands on (225, 225) and rotates around it.
 * Geometry here is in watch-face units (450 x 450 canvas) and rendered at
 * SCALE x so it stays crisp on larger displays.
 */
public class HandArt {
    static final int SCALE = 1;      // 1 unit == 1 px at 450 px; WFF scales to the device
    static final int SS = 4;         // supersampling factor for antialiasing

    static final Color STEEL = new Color(0xE9, 0xEC, 0xF0);
    static final Color STEEL_EDGE = new Color(0x8A, 0x93, 0x9E);
    static final Color LUME = new Color(0xF4, 0xF1, 0xE4);
    static final Color GMT_RED = new Color(0xD7, 0x26, 0x26);

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "app/src/main/res/drawable-nodpi");
        out.mkdirs();

        // name, width, length (pivot->tip), tail (pivot->butt)
        hand(out, "hand_hour", 24, 106, 22, HandArt::drawHour);
        hand(out, "hand_minute", 14, 178, 24, HandArt::drawMinute);
        hand(out, "hand_second", 14, 186, 42, HandArt::drawSecond);
        hand(out, "hand_gmt", 22, 194, 0, HandArt::drawGmt);
        hand(out, "hand_hour_ambient", 24, 106, 22, (g, w, len, tail) -> outlineOnly(g, w, len, tail, HandArt::hourShape));
        hand(out, "hand_minute_ambient", 14, 178, 24, (g, w, len, tail) -> outlineOnly(g, w, len, tail, HandArt::minuteShape));

        // Modern style: straight sticks with flat ends. Geometry must match MODERN in WatchFace.java.
        hand(out, "modern_hand_hour", 14, 108, 18, HandArt::drawStickHour);
        hand(out, "modern_hand_minute", 10, 178, 20, HandArt::drawStickMinute);
        hand(out, "modern_hand_second", 10, 188, 40, HandArt::drawStickSecond);
        hand(out, "modern_hand_gmt", 12, 194, 0, HandArt::drawStickGmt);
        hand(out, "modern_hand_hour_ambient", 14, 108, 18, (g, w, len, tail) -> outlineOnly(g, w, len, tail, HandArt::stickShape));
        hand(out, "modern_hand_minute_ambient", 10, 178, 20, (g, w, len, tail) -> outlineOnly(g, w, len, tail, HandArt::stickShape));
        pip(out);
        placeholderPreview(out);
        System.out.println("Wrote hand artwork to " + out.getPath());
    }

    interface Painter { void paint(Graphics2D g, double w, double len, double tail); }

    /** Canvas: x in [0,w], y = 0 at tip, y = len at pivot, y = len+tail at butt. */
    static void hand(File dir, String name, int w, int len, int tail, Painter p) throws Exception {
        int h = len + tail;
        BufferedImage img = new BufferedImage(w * SCALE * SS, h * SCALE * SS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.scale(SCALE * SS, SCALE * SS);
        p.paint(g, w, len, tail);
        g.dispose();
        write(downsample(img, w * SCALE, h * SCALE), new File(dir, name + ".png"));
        System.out.printf("%-20s %3dx%-3d pivotY=%.4f%n", name, w, h, (double) len / h);
    }

    // ---- shapes -----------------------------------------------------------

    static Shape hourShape(double w, double len, double tail) {
        double c = w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(c, 2);                       // sword tip
        p.lineTo(c + w * 0.45, 22);
        p.lineTo(c + w * 0.40, len + tail - 2);
        p.lineTo(c - w * 0.40, len + tail - 2);
        p.lineTo(c - w * 0.45, 22);
        p.closePath();
        return p;
    }

    static Shape minuteShape(double w, double len, double tail) {
        double c = w / 2;
        Path2D p = new Path2D.Double();
        p.moveTo(c, 1);
        p.lineTo(c + w * 0.42, 26);
        p.lineTo(c + w * 0.36, len + tail - 2);
        p.lineTo(c - w * 0.36, len + tail - 2);
        p.lineTo(c - w * 0.42, 26);
        p.closePath();
        return p;
    }

    static void drawHour(Graphics2D g, double w, double len, double tail) {
        drawSword(g, hourShape(w, len, tail), w, 14, len - 26);
    }

    static void drawMinute(Graphics2D g, double w, double len, double tail) {
        drawSword(g, minuteShape(w, len, tail), w, 18, len - 30);
    }

    /** Polished steel sword with a lume insert from lumeTop to lumeBottom. */
    static void drawSword(Graphics2D g, Shape body, double w, double lumeTop, double lumeBottom) {
        double c = w / 2;
        g.setPaint(new GradientPaint((float) (c - w / 2), 0, STEEL_EDGE, (float) c, 0, STEEL, true));
        g.fill(body);
        g.setColor(STEEL_EDGE);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(body);
        // Centre ridge
        g.setColor(new Color(255, 255, 255, 140));
        g.draw(new Line2D.Double(c, 4, c, lumeBottom + 18));
        // Lume insert
        Path2D lume = new Path2D.Double();
        lume.moveTo(c, lumeTop);
        lume.lineTo(c + w * 0.26, lumeTop + 12);
        lume.lineTo(c + w * 0.22, lumeBottom);
        lume.lineTo(c - w * 0.22, lumeBottom);
        lume.lineTo(c - w * 0.26, lumeTop + 12);
        lume.closePath();
        g.setColor(LUME);
        g.fill(lume);
    }

    static void drawSecond(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new RoundRectangle2D.Double(c - 1.2, 4, 2.4, len + tail - 6, 2, 2));
        // Counterweight
        g.fill(new RoundRectangle2D.Double(c - 2.6, len + 14, 5.2, tail - 16, 3, 3));
        // Lume lollipop
        double ly = 46, r = 6.5;
        g.fill(new Ellipse2D.Double(c - r, ly - r, 2 * r, 2 * r));
        g.setColor(LUME);
        g.fill(new Ellipse2D.Double(c - r + 1.6, ly - r + 1.6, 2 * r - 3.2, 2 * r - 3.2));
    }

    static void drawGmt(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new Rectangle2D.Double(c - 1.4, 20, 2.8, len - 20));
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(c, 1);
        arrow.lineTo(c + w / 2 - 1, 26);
        arrow.lineTo(c - w / 2 + 1, 26);
        arrow.closePath();
        g.fill(arrow);
        Path2D inset = new Path2D.Double();
        inset.moveTo(c, 7);
        inset.lineTo(c + w / 2 - 5, 23);
        inset.lineTo(c - w / 2 + 5, 23);
        inset.closePath();
        g.setColor(LUME);
        g.fill(inset);
    }

    // ---- Modern stick hands ------------------------------------------------

    static Shape stickShape(double w, double len, double tail) {
        return new RoundRectangle2D.Double(1, 1, w - 2, len + tail - 2, 2, 2);
    }

    /** Brushed steel stick with a lume stripe from lumeTop to lumeBottom. */
    static void drawStick(Graphics2D g, double w, double len, double tail, double lumeW, double lumeTop, double lumeBottom) {
        g.setPaint(new GradientPaint(0, 0, STEEL_EDGE, (float) (w / 2), 0, STEEL, true));
        g.fill(stickShape(w, len, tail));
        g.setColor(STEEL_EDGE);
        g.setStroke(new BasicStroke(0.6f));
        g.draw(stickShape(w, len, tail));
        g.setColor(LUME);
        g.fill(new Rectangle2D.Double((w - lumeW) / 2, lumeTop, lumeW, lumeBottom - lumeTop));
    }

    static void drawStickHour(Graphics2D g, double w, double len, double tail) {
        drawStick(g, w, len, tail, 6, 6, len - 24);
    }

    static void drawStickMinute(Graphics2D g, double w, double len, double tail) {
        drawStick(g, w, len, tail, 4, 6, len - 28);
    }

    static void drawStickSecond(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new Rectangle2D.Double(c - 1.1, 2, 2.2, len + tail - 4));
        g.fill(new Rectangle2D.Double(c - 2.5, len + 12, 5, tail - 14));
    }

    static void drawStickGmt(Graphics2D g, double w, double len, double tail) {
        double c = w / 2;
        g.setColor(GMT_RED);
        g.fill(new Rectangle2D.Double(c - 1.4, 20, 2.8, len - 20));
        // Rectangular tip: red frame with a lume insert.
        g.fill(new Rectangle2D.Double(1, 1, w - 2, 22));
        g.setColor(LUME);
        g.fill(new Rectangle2D.Double(3.5, 3.5, w - 7, 17));
    }

    interface ShapeFn { Shape make(double w, double len, double tail); }

    /** Low-power outline version for ambient mode. */
    static void outlineOnly(Graphics2D g, double w, double len, double tail, ShapeFn fn) {
        g.setColor(new Color(0xCF, 0xD4, 0xDA));
        g.setStroke(new BasicStroke(1.4f));
        g.draw(fn.make(w, len, tail));
    }

    /** 12 o'clock bezel triangle: points toward the dial centre. */
    static void pip(File dir) throws Exception {
        int w = 22, h = 18;
        BufferedImage img = new BufferedImage(w * SS, h * SS, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(SS, SS);
        Path2D t = new Path2D.Double();
        t.moveTo(1, 1);
        t.lineTo(w - 1, 1);
        t.lineTo(w / 2.0, h - 1);
        t.closePath();
        g.setColor(new Color(0xB8, 0xBF, 0xC8));
        g.fill(t);
        Path2D in = new Path2D.Double();
        in.moveTo(4, 3);
        in.lineTo(w - 4, 3);
        in.lineTo(w / 2.0, h - 4.5);
        in.closePath();
        g.setColor(LUME);
        g.fill(in);
        g.dispose();
        write(downsample(img, w, h), new File(dir, "bezel_pip.png"));
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
        g.fill(new Ellipse2D.Double(27, 27, s - 54, s - 54));
        g.setColor(LUME);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 34));
        FontMetrics fm = g.getFontMetrics();
        String t = "SOLSTICE GMT";
        g.drawString(t, (s - fm.stringWidth(t)) / 2, s / 2 + 12);
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
