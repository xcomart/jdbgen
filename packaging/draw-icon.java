/*
 * Draws resource/icon.png, the jdbgen application icon.
 *
 * The icon is a generated artifact rather than a hand-painted bitmap, so that
 * it can be reproduced byte for byte and tweaked by editing numbers instead of
 * pixels. resource/icon.ico is in turn derived from the PNG this writes, by
 * packaging/make-icon.ps1.
 *
 * Design: a database cylinder wrapped in a pair of code angle brackets --
 * "< database >" -- on a rounded square with a diagonal indigo gradient.
 * Everything is solid shapes with no hairlines, so the silhouette survives the
 * downscale to the 16 pixel frame of the .ico.
 *
 * Run it with the single file source launcher (JDK 11+), from the repository
 * root:
 *
 *     java packaging/draw-icon.java [output.png]
 *
 * The optional argument overrides the default output path, resource/icon.png.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class DrawIcon {

    /** Edge of the square canvas. The .ico frames are scaled down from it. */
    private static final int SIZE = 512;

    /** Transparent margin around the rounded square. */
    private static final double MARGIN = 26;
    /** Corner radius of the rounded square, as a fraction of its edge. */
    private static final double CORNER_RATIO = 0.235;

    private static final Color BG_TOP    = new Color(0x4C6EF5);
    private static final Color BG_MIDDLE = new Color(0x3B5BDB);
    private static final Color BG_BOTTOM = new Color(0x1E2A78);
    private static final Color CYLINDER  = Color.WHITE;
    private static final Color BRACKET   = new Color(0x7FE7FF);

    // Cylinder geometry. cy* values are ellipse centres, not shape extents.
    private static final double CX = SIZE / 2.0;
    private static final double CY = SIZE / 2.0;
    private static final double RX = 66;   // half width
    private static final double RY = 23;   // half height of the end ellipses
    private static final double DISC = 48; // straight side of one disc
    private static final double GAP = 32;  // height of the gap between discs
    /** Centre of the top ellipse, placed so the whole cylinder is centred. */
    private static final double TOP = CY - DISC - GAP / 2;

    // Angle brackets. The arms are kept close to 45 degrees: a steeper "<" turns
    // into a plain vertical bar once the icon is scaled down to 16 pixels.
    private static final double BRACKET_TIP = 88;    // x of the left tip
    private static final double BRACKET_RUN = 64;    // horizontal reach of an arm
    private static final double BRACKET_RISE = 74;   // vertical reach of an arm
    private static final double BRACKET_WIDTH = 44;  // stroke width

    public static void main(String[] args) throws Exception {
        File target = new File(args.length > 0 ? args[0] : "resource/icon.png");

        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            paintBackground(g);
            paintCylinder(g);
            paintBrackets(g);
        } finally {
            g.dispose();
        }

        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        ImageIO.write(image, "png", target);
        System.out.printf("Wrote    %s (%dx%d)%n", target.getAbsolutePath(), SIZE, SIZE);
    }

    /** The rounded square and its top left to bottom right gradient. */
    private static void paintBackground(Graphics2D g) {
        double edge = SIZE - 2 * MARGIN;
        RoundRectangle2D plate = new RoundRectangle2D.Double(
                MARGIN, MARGIN, edge, edge, edge * CORNER_RATIO * 2, edge * CORNER_RATIO * 2);

        g.setPaint(new LinearGradientPaint(
                new Point2D.Double(MARGIN, MARGIN),
                new Point2D.Double(SIZE - MARGIN, SIZE - MARGIN),
                new float[] { 0f, 0.5f, 1f },
                new Color[] { BG_TOP, BG_MIDDLE, BG_BOTTOM },
                MultipleGradientPaint.CycleMethod.NO_CYCLE));
        g.fill(plate);
    }

    /** Two stacked discs, cut out of a single cylinder so the seam stays clean. */
    private static void paintCylinder(Graphics2D g) {
        double bottom = TOP + 2 * DISC + GAP;

        Area cylinder = cylinder(TOP, bottom - TOP);
        cylinder.subtract(seam(TOP + DISC));

        g.setPaint(CYLINDER);
        g.fill(cylinder);
    }

    /** A capsule with elliptical ends: the top ellipse, the sides and the base. */
    private static Area cylinder(double topCentre, double height) {
        Area area = new Area(new Ellipse2D.Double(CX - RX, topCentre - RY, 2 * RX, 2 * RY));
        area.add(new Area(new Rectangle2D.Double(CX - RX, topCentre, 2 * RX, height)));
        area.add(new Area(new Ellipse2D.Double(CX - RX, topCentre + height - RY, 2 * RX, 2 * RY)));
        return area;
    }

    /**
     * The band removed between two discs. Both of its edges follow the same
     * downward curve as the cylinder's base, so the discs keep their shape
     * instead of being sliced by a straight line.
     */
    private static Area seam(double top) {
        double wide = RX + 4; // overshoot sideways; the excess falls outside the cylinder
        Area band = new Area(new Rectangle2D.Double(CX - wide, top, 2 * wide, GAP));
        band.add(new Area(new Ellipse2D.Double(CX - wide, top + GAP - RY, 2 * wide, 2 * RY)));
        band.subtract(new Area(new Ellipse2D.Double(CX - RX, top - RY, 2 * RX, 2 * RY)));
        return band;
    }

    /** The "<" and ">" flanking the cylinder. */
    private static void paintBrackets(Graphics2D g) {
        g.setPaint(BRACKET);
        g.setStroke(new BasicStroke((float) BRACKET_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(bracket(BRACKET_TIP, CY, BRACKET_RUN));
        g.draw(bracket(SIZE - BRACKET_TIP, CY, -BRACKET_RUN));
    }

    private static Path2D bracket(double tipX, double tipY, double run) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(tipX + run, tipY - BRACKET_RISE);
        path.lineTo(tipX, tipY);
        path.lineTo(tipX + run, tipY + BRACKET_RISE);
        return path;
    }
}
