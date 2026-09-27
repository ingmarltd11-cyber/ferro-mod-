package dev.ferro.client.ui;

import dev.ferro.client.utils.RenderUtils;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The small vector glyphs of the interface.
 *
 * <p>Every icon is built from the same rounded rectangles the rest of the client draws with, so a glyph
 * costs a handful of quads, scales with its box and needs no texture atlas. That is what keeps FERRO self
 * contained: no resource pack, no PNG per state, and a theme switch recolours every icon for free because
 * icons only take a colour instead of shipping one.</p>
 */
public final class Icons {

    /** The available glyphs. */
    public enum Icon {
        /** A crown, used by the wordmark. */
        CROWN,
        /** Two crossed blades. */
        SWORD,
        /** A movement arrow. */
        RUN,
        /** An eye. */
        EYE,
        /** A single person. */
        PLAYER,
        /** A globe. */
        GLOBE,
        /** A four pointed star. */
        SPARK,
        /** A cog. */
        GEAR,
        /** Two sliders. */
        SLIDERS,
        /** A colour wheel. */
        PALETTE,
        /** A folder. */
        FOLDER,
        /** Two people. */
        PEOPLE,
        /** A keyboard. */
        KEYBOARD,
        /** A magnifier. */
        SEARCH,
        /** A bulleted list. */
        LIST,
        /** Four squares. */
        GRID,
        /** A right pointing chevron. */
        CHEVRON_RIGHT,
        /** A left pointing chevron with a tail. */
        BACK,
        /** A tick. */
        CHECK
    }

    private Icons() {
    }

    /**
     * Draws an icon.
     *
     * @param graphics the render context
     * @param icon     the glyph
     * @param x        the left edge of the bounding box
     * @param y        the top edge of the bounding box
     * @param size     the width and height of the bounding box, in pixels
     * @param colour   the ARGB colour
     */
    public static void draw(GuiGraphics graphics, Icon icon, double x, double y, double size, int colour) {
        switch (icon) {
            case CROWN -> crown(graphics, x, y, size, colour);
            case SWORD -> sword(graphics, x, y, size, colour);
            case RUN -> run(graphics, x, y, size, colour);
            case EYE -> eye(graphics, x, y, size, colour);
            case PLAYER -> player(graphics, x, y, size, colour);
            case GLOBE -> globe(graphics, x, y, size, colour);
            case SPARK -> spark(graphics, x, y, size, colour);
            case GEAR -> gear(graphics, x, y, size, colour);
            case SLIDERS -> sliders(graphics, x, y, size, colour);
            case PALETTE -> palette(graphics, x, y, size, colour);
            case FOLDER -> folder(graphics, x, y, size, colour);
            case PEOPLE -> people(graphics, x, y, size, colour);
            case KEYBOARD -> keyboard(graphics, x, y, size, colour);
            case SEARCH -> search(graphics, x, y, size, colour);
            case LIST -> list(graphics, x, y, size, colour);
            case GRID -> grid(graphics, x, y, size, colour);
            case CHEVRON_RIGHT -> chevronRight(graphics, x, y, size, colour);
            case BACK -> back(graphics, x, y, size, colour);
            case CHECK -> check(graphics, x, y, size, colour);
        }
    }

    // ------------------------------------------------------------------ glyphs

    /**
     * Draws the crown of the wordmark: a band with five spikes.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void crown(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x, y + u * 8.2D, size, u * 3.0D, colour, 1);
        double[] spikes = {u * 4.0D, u * 6.0D, u * 7.5D, u * 6.0D, u * 4.0D};
        for (int index = 0; index < spikes.length; index++) {
            double spikeX = x + u * (0.3D + index * 2.42D);
            double height = spikes[index];
            RenderUtils.rounded(graphics, spikeX, y + u * 8.2D - height, u * 1.4D, height, colour, 1);
        }
    }

    /**
     * Draws two crossed blades with their guards.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void sword(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double thickness = Math.max(1.0D, u * 1.5D);
        stroke(graphics, x + u * 1.2D, y + u * 1.2D, x + u * 10.8D, y + u * 10.8D, thickness, colour);
        stroke(graphics, x + u * 10.8D, y + u * 1.2D, x + u * 1.2D, y + u * 10.8D, thickness, colour);
        RenderUtils.rounded(graphics, x + u * 1.0D, y + u * 8.0D, u * 3.6D, thickness, colour, 1);
        RenderUtils.rounded(graphics, x + u * 7.4D, y + u * 8.0D, u * 3.6D, thickness, colour, 1);
    }

    /**
     * Draws the movement arrow.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void run(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x + u * 0.6D, y + u * 5.0D, u * 6.6D, u * 2.0D, colour, 1);
        for (int step = 0; step < 5; step++) {
            double height = u * (7.0D - step * 1.1D);
            RenderUtils.rounded(graphics, x + u * (6.6D + step), y + u * 6.0D - height / 2.0D, u, height, colour, 1);
        }
    }

    /**
     * Draws an eye: a ring with a filled pupil.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void eye(GuiGraphics graphics, double x, double y, double size, int colour) {
        double centreX = x + size / 2.0D;
        double centreY = y + size / 2.0D;
        ring(graphics, centreX, centreY, size * 0.46D, size * 0.31D, Math.max(1.0D, size * 0.10D), colour);
        ellipse(graphics, centreX, centreY, size * 0.15D, size * 0.15D, colour);
    }

    /**
     * Draws a person: a head and a pair of shoulders.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void player(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x + u * 3.4D, y + u * 0.6D, u * 5.2D, u * 5.2D, colour, 4);
        RenderUtils.rounded(graphics, x + u * 1.4D, y + u * 7.0D, u * 9.2D, u * 4.6D, colour, 5);
    }

    /**
     * Draws a globe: an outline, a meridian and the equator.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void globe(GuiGraphics graphics, double x, double y, double size, int colour) {
        double centreX = x + size / 2.0D;
        double centreY = y + size / 2.0D;
        ring(graphics, centreX, centreY, size * 0.46D, size * 0.46D, Math.max(1.0D, size * 0.10D), colour);
        ring(graphics, centreX, centreY, size * 0.21D, size * 0.46D, Math.max(1.0D, size * 0.08D), colour);
        RenderUtils.rect(graphics, centreX - size * 0.46D, centreY - 0.6D, size * 0.92D, 1.2D, colour);
    }

    /**
     * Draws a four pointed star.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void spark(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double centreX = x + size / 2.0D;
        double centreY = y + size / 2.0D;
        RenderUtils.rounded(graphics, centreX - u * 0.8D, y + u, u * 1.6D, u * 10.0D, colour, 1);
        RenderUtils.rounded(graphics, x + u, centreY - u * 0.8D, u * 10.0D, u * 1.6D, colour, 1);
        RenderUtils.rounded(graphics, centreX - u * 4.0D, centreY - u * 4.0D, u * 2.0D, u * 2.0D, colour, 1);
        RenderUtils.rounded(graphics, centreX + u * 2.0D, centreY - u * 4.0D, u * 2.0D, u * 2.0D, colour, 1);
        RenderUtils.rounded(graphics, centreX - u * 4.0D, centreY + u * 2.0D, u * 2.0D, u * 2.0D, colour, 1);
        RenderUtils.rounded(graphics, centreX + u * 2.0D, centreY + u * 2.0D, u * 2.0D, u * 2.0D, colour, 1);
    }

    /**
     * Draws a cog: a ring with four teeth.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void gear(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double centreX = x + size / 2.0D;
        double centreY = y + size / 2.0D;
        ring(graphics, centreX, centreY, size * 0.30D, size * 0.30D, u * 1.7D, colour);
        RenderUtils.rounded(graphics, centreX - u, y, u * 2.0D, u * 3.0D, colour, 1);
        RenderUtils.rounded(graphics, centreX - u, y + u * 9.0D, u * 2.0D, u * 3.0D, colour, 1);
        RenderUtils.rounded(graphics, x, centreY - u, u * 3.0D, u * 2.0D, colour, 1);
        RenderUtils.rounded(graphics, x + u * 9.0D, centreY - u, u * 3.0D, u * 2.0D, colour, 1);
    }

    /**
     * Draws two sliders with their handles.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void sliders(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x + u * 0.4D, y + u * 3.2D, u * 11.2D, u * 1.2D, colour, 1);
        RenderUtils.rounded(graphics, x + u * 6.6D, y + u * 1.8D, u * 2.2D, u * 4.0D, colour, 2);
        RenderUtils.rounded(graphics, x + u * 0.4D, y + u * 7.6D, u * 11.2D, u * 1.2D, colour, 1);
        RenderUtils.rounded(graphics, x + u * 3.2D, y + u * 6.2D, u * 2.2D, u * 4.0D, colour, 2);
    }

    /**
     * Draws a colour wheel with four blobs.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void palette(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double centreX = x + size / 2.0D;
        double centreY = y + size / 2.0D;
        ring(graphics, centreX, centreY, size * 0.45D, size * 0.45D, Math.max(1.0D, size * 0.11D), colour);
        ellipse(graphics, centreX - u * 2.6D, centreY - u * 1.4D, u * 1.2D, u * 1.2D, colour);
        ellipse(graphics, centreX + u * 1.6D, centreY - u * 2.6D, u * 1.2D, u * 1.2D, colour);
        ellipse(graphics, centreX + u * 2.6D, centreY + u * 1.6D, u * 1.2D, u * 1.2D, colour);
        ellipse(graphics, centreX - u * 0.6D, centreY + u * 2.8D, u * 1.2D, u * 1.2D, colour);
    }

    /**
     * Draws a folder with a tab.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void folder(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x + u * 0.4D, y + u * 1.4D, u * 5.2D, u * 2.6D, colour, 1);
        RenderUtils.rounded(graphics, x + u * 0.4D, y + u * 3.4D, u * 11.2D, u * 7.2D, colour, 2);
    }

    /**
     * Draws two people side by side.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void people(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        RenderUtils.rounded(graphics, x + u * 0.8D, y + u * 1.6D, u * 3.8D, u * 3.8D, colour, 4);
        RenderUtils.rounded(graphics, x + u * 6.6D, y + u * 2.6D, u * 3.4D, u * 3.4D, colour, 4);
        RenderUtils.rounded(graphics, x + u * 0.2D, y + u * 6.4D, u * 5.2D, u * 4.4D, colour, 4);
        RenderUtils.rounded(graphics, x + u * 6.2D, y + u * 7.2D, u * 4.6D, u * 4.0D, colour, 4);
    }

    /**
     * Draws a keyboard: three rows of keys.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void keyboard(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        for (int row = 0; row < 2; row++) {
            for (int key = 0; key < 4; key++) {
                RenderUtils.rounded(graphics, x + u * (0.8D + key * 2.7D), y + u * (1.8D + row * 3.1D), u * 1.9D,
                        u * 1.9D, colour, 1);
            }
        }
        RenderUtils.rounded(graphics, x + u * 2.2D, y + u * 8.2D, u * 7.6D, u * 1.9D, colour, 1);
    }

    /**
     * Draws a magnifier.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void search(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        ring(graphics, x + u * 5.0D, y + u * 5.0D, u * 4.0D, u * 4.0D, Math.max(1.0D, u * 1.4D), colour);
        stroke(graphics, x + u * 8.2D, y + u * 8.2D, x + u * 11.2D, y + u * 11.2D, Math.max(1.0D, u * 1.6D), colour);
    }

    /**
     * Draws a bulleted list.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void list(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        for (int row = 0; row < 3; row++) {
            double rowY = y + u * (1.4D + row * 3.6D);
            RenderUtils.rounded(graphics, x + u * 0.6D, rowY, u * 1.9D, u * 1.9D, colour, 1);
            RenderUtils.rounded(graphics, x + u * 3.8D, rowY + u * 0.4D, u * 7.6D, u * 1.1D, colour, 1);
        }
    }

    /**
     * Draws four squares.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void grid(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double[] offsets = {0.6D, 6.8D};
        for (double offsetX : offsets) {
            for (double offsetY : offsets) {
                RenderUtils.rounded(graphics, x + u * offsetX, y + u * offsetY, u * 4.6D, u * 4.6D, colour, 1);
            }
        }
    }

    /**
     * Draws a right pointing chevron.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void chevronRight(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double thickness = Math.max(1.0D, u * 1.5D);
        stroke(graphics, x + u * 4.2D, y + u * 2.0D, x + u * 8.0D, y + u * 6.0D, thickness, colour);
        stroke(graphics, x + u * 8.0D, y + u * 6.0D, x + u * 4.2D, y + u * 10.0D, thickness, colour);
    }

    /**
     * Draws a chevron pointing left, with a tail for the Back button.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void back(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double thickness = Math.max(1.0D, u * 1.5D);
        stroke(graphics, x + u * 5.4D, y + u * 2.0D, x + u * 1.6D, y + u * 6.0D, thickness, colour);
        stroke(graphics, x + u * 1.6D, y + u * 6.0D, x + u * 5.4D, y + u * 10.0D, thickness, colour);
        RenderUtils.rounded(graphics, x + u * 4.6D, y + u * 5.0D, u * 6.6D, u * 2.0D, colour, 1);
    }

    /**
     * Draws a tick.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param size     the box size
     * @param colour   the ARGB colour
     */
    private static void check(GuiGraphics graphics, double x, double y, double size, int colour) {
        double u = size / 12.0D;
        double thickness = Math.max(1.0D, u * 1.7D);
        stroke(graphics, x + u * 1.8D, y + u * 6.4D, x + u * 4.8D, y + u * 9.4D, thickness, colour);
        stroke(graphics, x + u * 4.8D, y + u * 9.4D, x + u * 10.2D, y + u * 2.8D, thickness, colour);
    }

    // ------------------------------------------------------------------ primitives

    /**
     * Draws a straight line by stepping small rounded squares along it. Diagonal strokes are what the
     * blades, chevrons and the magnifier are made of.
     *
     * @param graphics  the render context
     * @param x1        the start x
     * @param y1        the start y
     * @param x2        the end x
     * @param y2        the end y
     * @param thickness the stroke width
     * @param colour    the ARGB colour
     */
    private static void stroke(GuiGraphics graphics, double x1, double y1, double x2, double y2, double thickness,
                               int colour) {
        double deltaX = x2 - x1;
        double deltaY = y2 - y1;
        int steps = Math.max(1, (int) Math.ceil(Math.max(Math.abs(deltaX), Math.abs(deltaY))));
        double radius = thickness / 2.0D;
        for (int step = 0; step <= steps; step++) {
            double progress = (double) step / steps;
            RenderUtils.rounded(graphics, x1 + deltaX * progress - radius, y1 + deltaY * progress - radius,
                    thickness, thickness, colour, (int) Math.ceil(radius));
        }
    }

    /**
     * Draws a filled ellipse, one scanline per row.
     *
     * @param graphics  the render context
     * @param centreX   the centre x
     * @param centreY   the centre y
     * @param radiusX   the horizontal radius
     * @param radiusY   the vertical radius
     * @param colour    the ARGB colour
     */
    private static void ellipse(GuiGraphics graphics, double centreX, double centreY, double radiusX,
                                double radiusY, int colour) {
        int rows = Math.max(1, (int) Math.ceil(radiusY * 2.0D));
        for (int row = 0; row < rows; row++) {
            double half = halfWidth(radiusX, radiusY, row, rows);
            RenderUtils.rect(graphics, centreX - half, centreY - radiusY + row, Math.max(1.0D, half * 2.0D), 1.0D,
                    colour);
        }
    }

    /**
     * Draws the outline of an ellipse by filling only the left and right edges of every scanline.
     *
     * @param graphics  the render context
     * @param centreX   the centre x
     * @param centreY   the centre y
     * @param radiusX   the horizontal radius
     * @param radiusY   the vertical radius
     * @param thickness the line width
     * @param colour    the ARGB colour
     */
    private static void ring(GuiGraphics graphics, double centreX, double centreY, double radiusX, double radiusY,
                             double thickness, int colour) {
        int rows = Math.max(1, (int) Math.ceil(radiusY * 2.0D));
        for (int row = 0; row < rows; row++) {
            double half = halfWidth(radiusX, radiusY, row, rows);
            double y = centreY - radiusY + row;
            if (half <= thickness) {
                RenderUtils.rect(graphics, centreX - half, y, Math.max(1.0D, half * 2.0D), 1.0D, colour);
                continue;
            }
            RenderUtils.rect(graphics, centreX - half, y, thickness, 1.0D, colour);
            RenderUtils.rect(graphics, centreX + half - thickness, y, thickness, 1.0D, colour);
        }
    }

    /**
     * @param radiusX the horizontal radius
     * @param radiusY the vertical radius
     * @param row     the scanline index
     * @param rows    the amount of scanlines
     * @return half the width of the ellipse on that scanline
     */
    private static double halfWidth(double radiusX, double radiusY, int row, int rows) {
        double normalised = (row + 0.5D) / rows * 2.0D - 1.0D;
        return radiusX * Math.sqrt(Math.max(0.0D, 1.0D - normalised * normalised));
    }
}
