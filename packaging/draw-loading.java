/*
 * Draws resource/loading.gif, the busy indicator jdbgen shows on the glass pane
 * of a window while a background task runs (UIUtils.loading).
 *
 * Like resource/icon.png this is a generated artifact rather than a hand made
 * bitmap, so it can be reproduced byte for byte and tweaked by editing numbers
 * instead of pixels.
 *
 * Design: a rotating arc -- a round capped comet sweeping a bit under a third
 * of the circle, thinning and lightening from the indigo of the application
 * icon (#4C6EF5) at the head to its cyan accent (#7FE7FF) at the tail, so the
 * tail reads as fading out. The two ends carry the image on opposite
 * backgrounds: the indigo head stands out on a light theme, the cyan tail on a
 * dark one.
 *
 * The animation is a plain constant speed rotation, clockwise, one turn per
 * FRAMES frames, looping forever.
 *
 * A GIF only has one bit of transparency, so nothing can be drawn half
 * transparent over the user interface underneath. Every frame is therefore
 * rendered supersampled with real antialiasing, box filtered down to the final
 * size, and then flattened: coverage below half a pixel becomes fully
 * transparent, and the rest is composited over mid grey (#808080). Mid grey is
 * the one blend partner whose fringe stays inconspicuous on both a light and a
 * dark background.
 *
 * Run it with the single file source launcher (JDK 11+), from the repository
 * root:
 *
 *     java packaging/draw-loading.java [output.gif]
 *
 * The optional argument overrides the default output path, resource/loading.gif.
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.w3c.dom.Node;

public class DrawLoading {

    /** Edge of the square canvas, in pixels. */
    private static final int SIZE = 64;
    /** Supersampling factor. Frames are drawn this much bigger, then averaged. */
    private static final int SS = 8;

    /** Frames in one full turn. */
    private static final int FRAMES = 24;
    /** Frame delay in hundredths of a second: 4 is 40ms, so a turn takes 960ms. */
    private static final int DELAY_CS = 4;

    /** Radius of the centre line of the arc. */
    private static final double RADIUS = 23.0;
    /** Stroke width at the head, 13% of the 64 pixel diameter. */
    private static final double WIDTH = 8.4;
    /** Length of the arc, in degrees: a bit under a third of the circle. */
    private static final double SWEEP = 110.0;
    /** Sub arcs the sweep is cut into, to fake a gradient and a taper. */
    private static final int SEGMENTS = 72;

    private static final Color TAIL = new Color(0x7FE7FF);
    private static final Color HEAD = new Color(0x4C6EF5);

    /** The colour the antialiased edge is blended with, see the file header. */
    private static final int MATTE = 0x808080;
    /** Coverage at or above this (of 255) survives the flattening. */
    private static final int ALPHA_CUT = 128;

    public static void main(String[] args) throws Exception {
        File target = new File(args.length > 0 ? args[0] : "resource/loading.gif");

        List<BufferedImage> frames = new ArrayList<>(FRAMES);
        for (int i = 0; i < FRAMES; i++) {
            frames.add(flatten(shrink(render(i))));
        }

        IndexColorModel palette = palette(frames);
        List<BufferedImage> indexed = new ArrayList<>(FRAMES);
        for (BufferedImage frame : frames) {
            indexed.add(index(frame, palette));
        }

        File parent = target.getAbsoluteFile().getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        write(indexed, target);

        System.out.printf("Wrote    %s (%dx%d, %d frames, %dms each, %d colours, %d bytes)%n",
                target.getAbsolutePath(), SIZE, SIZE, FRAMES, DELAY_CS * 10,
                palette.getMapSize(), target.length());
    }

    // ------------------------------------------------------------------ drawing

    /** One supersampled ARGB frame, the arc rotated by index/FRAMES of a turn. */
    private static BufferedImage render(int index) {
        int big = SIZE * SS;
        BufferedImage image = new BufferedImage(big, big, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            // Arc2D counts degrees counterclockwise, so subtracting turns the
            // comet clockwise, the direction a spinner is expected to run in.
            double head = 90.0 - 360.0 * index / FRAMES;
            double r = RADIUS * SS;
            double diameter = 2 * r;
            double x = big / 2.0 - r;

            // Tail first, so the brighter head and its round cap end up on top.
            for (int s = 0; s < SEGMENTS; s++) {
                double t0 = (double) s / SEGMENTS;
                double t1 = (double) (s + 1) / SEGMENTS;
                double a0 = head - SWEEP * (1 - t0);
                double a1 = head - SWEEP * (1 - t1);

                double mid = (t0 + t1) / 2;
                g.setPaint(mix(TAIL, HEAD, mid));
                g.setStroke(new BasicStroke((float) (WIDTH * SS * taper(mid)),
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Arc2D.Double(x, x, diameter, diameter, a0, a1 - a0, Arc2D.OPEN));
            }
        } finally {
            g.dispose();
        }
        return image;
    }

    /** Stroke width along the arc: thinnest at the tail tip, full at the head. */
    private static double taper(double t) {
        return 0.55 + 0.45 * Math.sqrt(t);
    }

    private static Color mix(Color from, Color to, double t) {
        return new Color(
                (int) Math.round(from.getRed() + (to.getRed() - from.getRed()) * t),
                (int) Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t),
                (int) Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t));
    }

    // ------------------------------------------------------------- downsampling

    /** Box filters the supersampled frame down to SIZE, averaging in premultiplied alpha. */
    private static BufferedImage shrink(BufferedImage big) {
        BufferedImage small = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        int cells = SS * SS;
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                long sa = 0, sr = 0, sg = 0, sb = 0;
                for (int dy = 0; dy < SS; dy++) {
                    for (int dx = 0; dx < SS; dx++) {
                        int argb = big.getRGB(x * SS + dx, y * SS + dy);
                        int a = (argb >>> 24);
                        sa += a;
                        sr += a * ((argb >> 16) & 0xFF);
                        sg += a * ((argb >> 8) & 0xFF);
                        sb += a * (argb & 0xFF);
                    }
                }
                int argb = 0;
                if (sa > 0) {
                    int a = (int) Math.round((double) sa / cells);
                    argb = (a << 24)
                            | ((int) Math.round((double) sr / sa) << 16)
                            | ((int) Math.round((double) sg / sa) << 8)
                            | (int) Math.round((double) sb / sa);
                }
                small.setRGB(x, y, argb);
            }
        }
        return small;
    }

    /** Cuts the alpha channel down to one bit, blending what is left over mid grey. */
    private static BufferedImage flatten(BufferedImage source) {
        BufferedImage flat = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int argb = source.getRGB(x, y);
                int a = argb >>> 24;
                if (a < ALPHA_CUT) {
                    flat.setRGB(x, y, 0);
                    continue;
                }
                double f = a / 255.0;
                int rgb = 0;
                for (int shift = 16; shift >= 0; shift -= 8) {
                    int c = (argb >> shift) & 0xFF;
                    int m = (MATTE >> shift) & 0xFF;
                    rgb |= (int) Math.round(c * f + m * (1 - f)) << shift;
                }
                flat.setRGB(x, y, 0xFF000000 | rgb);
            }
        }
        return flat;
    }

    // ------------------------------------------------------------------ palette

    /**
     * One palette for every frame, index 0 transparent. The colours are bucketed
     * by dropping low bits, using the fewest bits that fit the whole animation
     * into 255 opaque entries -- a small table keeps the GIF's codes short.
     */
    private static IndexColorModel palette(List<BufferedImage> frames) {
        int drop = 0;
        Map<Integer, Integer> buckets;
        while (true) {
            buckets = new LinkedHashMap<>();
            for (BufferedImage frame : frames) {
                for (int y = 0; y < SIZE; y++) {
                    for (int x = 0; x < SIZE; x++) {
                        int argb = frame.getRGB(x, y);
                        if ((argb >>> 24) == 0) {
                            continue;
                        }
                        buckets.putIfAbsent(bucket(argb, drop), null);
                    }
                }
            }
            if (buckets.size() <= 255) {
                break;
            }
            drop++;
        }

        int size = 1;
        while (size < buckets.size() + 1) {
            size <<= 1;
        }
        byte[] r = new byte[size];
        byte[] g = new byte[size];
        byte[] b = new byte[size];
        int i = 1;
        for (Integer key : buckets.keySet()) {
            buckets.put(key, i);
            int rgb = unbucket(key, drop);
            r[i] = (byte) (rgb >> 16);
            g[i] = (byte) (rgb >> 8);
            b[i] = (byte) rgb;
            i++;
        }
        indexOf = buckets;
        colourDrop = drop;
        return new IndexColorModel(8, size, r, g, b, 0);
    }

    /** Filled in by palette(), read by index(): the bucket to palette entry map. */
    private static Map<Integer, Integer> indexOf;
    private static int colourDrop;

    private static int bucket(int argb, int drop) {
        return ((argb >> (16 + drop)) & (0xFF >> drop)) << 16
                | ((argb >> (8 + drop)) & (0xFF >> drop)) << 8
                | ((argb >> drop) & (0xFF >> drop));
    }

    private static int unbucket(int key, int drop) {
        int half = drop == 0 ? 0 : 1 << (drop - 1);
        int rgb = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int c = Math.min(255, (((key >> shift) & 0xFF) << drop) + half);
            rgb |= c << shift;
        }
        return rgb;
    }

    private static BufferedImage index(BufferedImage frame, IndexColorModel palette) {
        BufferedImage out = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_INDEXED, palette);
        WritableRaster raster = out.getRaster();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                int argb = frame.getRGB(x, y);
                int value = (argb >>> 24) == 0 ? 0 : indexOf.get(bucket(argb, colourDrop));
                raster.setSample(x, y, 0, value);
            }
        }
        return out;
    }

    // ------------------------------------------------------------- gif encoding

    private static void write(List<BufferedImage> frames, File target) throws Exception {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IllegalStateException("no GIF image writer available");
        }
        ImageWriter writer = writers.next();
        // An image output stream on a File writes in place instead of truncating,
        // so a shorter GIF would keep the tail of the previous one.
        target.delete();
        try (ImageOutputStream out = ImageIO.createImageOutputStream(target)) {
            writer.setOutput(out);
            writer.prepareWriteSequence(null);
            ImageWriteParam params = writer.getDefaultWriteParam();
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = frames.get(i);
                IIOMetadata meta = writer.getDefaultImageMetadata(
                        new ImageTypeSpecifier(frame), params);
                writer.writeToSequence(new IIOImage(frame, null, describe(meta, i == 0)), params);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
    }

    /** Frame delay, one bit transparency, and on the first frame the loop marker. */
    private static IIOMetadata describe(IIOMetadata meta, boolean first) throws Exception {
        String format = meta.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(format);

        IIOMetadataNode control = child(root, "GraphicControlExtension");
        // The frames do not paint over each other: each one has to start from a
        // clear canvas, or the arc would smear into a full ring.
        control.setAttribute("disposalMethod", "restoreToBackgroundColor");
        control.setAttribute("userInputFlag", "FALSE");
        control.setAttribute("transparentColorFlag", "TRUE");
        control.setAttribute("transparentColorIndex", "0");
        control.setAttribute("delayTime", Integer.toString(DELAY_CS));

        if (first) {
            IIOMetadataNode extensions = child(root, "ApplicationExtensions");
            IIOMetadataNode netscape = new IIOMetadataNode("ApplicationExtension");
            netscape.setAttribute("applicationID", "NETSCAPE");
            netscape.setAttribute("authenticationCode", "2.0");
            netscape.setUserObject(new byte[] { 1, 0, 0 }); // sub block 1, repeat forever
            extensions.appendChild(netscape);
        }

        meta.setFromTree(format, root);
        return meta;
    }

    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        for (int i = 0; i < parent.getLength(); i++) {
            Node node = parent.item(i);
            if (node.getNodeName().equalsIgnoreCase(name)) {
                return (IIOMetadataNode) node;
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(name);
        parent.appendChild(node);
        return node;
    }
}
