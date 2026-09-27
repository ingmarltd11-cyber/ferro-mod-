package dev.ferro.client.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ferro.client.Argon;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

/**
 * Every drawing primitive FERRO uses, plus the world to screen projection.
 *
 * <p>FERRO renders world anchored visuals (ESP, tracers, nametags, breadcrumbs, trajectories) as a
 * projected 2D overlay on top of the {@link GuiGraphics} pipeline instead of touching
 * {@code RenderType} and {@code VertexConsumer}. That decision is deliberate: it keeps every
 * renderer working across Minecraft point releases, at the cost of not writing into the depth
 * buffer. Anything that needs real depth testing (chams, xray shading) is implemented as a mixin
 * instead of a renderer.</p>
 */
public final class RenderUtils {

    /**
     * The colours below are written by {@code Theme.apply}. They are mutable on purpose: every module and
     * both ClickGUI layouts read them directly, so a theme change is one method call instead of a change to
     * eighty modules.
     */
    /** Window background. */
    public static int BACKGROUND = 0xFF1A0000;
    /** Sidebar background. */
    public static int SIDEBAR = 0xFF120000;
    /** Card background. */
    public static int SURFACE = 0xFF240404;
    /** Border colour. */
    public static int FRAME = 0xFF3D0000;
    /** Accent colour, used for enabled modules, sliders and highlights. */
    public static int ACCENT = 0xFFFF1A1A;
    /** Secondary accent, the far end of every accent to accent gradient. */
    public static int ACCENT_2 = 0xFFFF7A18;
    /** Accent colour on hover. */
    public static int HOVER = 0xFFFF4D4D;
    /** Primary text colour. */
    public static int TEXT = 0xFFFFFFFF;
    /** Muted text colour. */
    public static int TEXT_DISABLED = 0xFFFF9999;
    /** Separator and border colour. */
    public static int OUTLINE = 0xFF5A0000;
    /** Panel colour with transparency, for HUD backgrounds. */
    public static int PANEL = 0xB01A0000;

    /**
     * Replaces every interface colour at once. Called by {@code Theme.apply}.
     *
     * @param background the window background
     * @param sidebar    the sidebar background
     * @param surface    the card background
     * @param border     the border colour
     * @param accent     the accent colour, the start of every gradient
     * @param accent2    the secondary accent colour, the end of every gradient
     * @param hover      the accent hover colour
     * @param text       the primary text colour
     * @param muted      the muted text colour
     */
    public static void setTheme(int background, int sidebar, int surface, int border, int accent, int accent2,
                                int hover, int text, int muted) {
        BACKGROUND = background;
        SIDEBAR = sidebar;
        SURFACE = surface;
        FRAME = border;
        OUTLINE = border;
        ACCENT = accent;
        ACCENT_2 = accent2;
        HOVER = hover;
        TEXT = text;
        TEXT_DISABLED = muted;
        PANEL = withAlpha(surface, 0xB0);
    }

    private RenderUtils() {
    }

    /**
     * @return the shared Minecraft instance
     */
    public static Minecraft mc() {
        return Minecraft.getInstance();
    }

    /**
     * @return the shared font renderer
     */
    public static Font font() {
        return Minecraft.getInstance().font;
    }

    /**
     * @return the width of the scaled GUI area in pixels
     */
    public static int screenWidth() {
        return Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /**
     * @return the height of the scaled GUI area in pixels
     */
    public static int screenHeight() {
        return Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    // ------------------------------------------------------------------ colours

    /**
     * @param red   red channel, {@code 0..255}
     * @param green green channel, {@code 0..255}
     * @param blue  blue channel, {@code 0..255}
     * @return an opaque ARGB colour
     */
    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | (red << 16) | (green << 8) | blue;
    }

    /**
     * @param red   red channel, {@code 0..255}
     * @param green green channel, {@code 0..255}
     * @param blue  blue channel, {@code 0..255}
     * @param alpha alpha channel, {@code 0..255}
     * @return an ARGB colour
     */
    public static int rgba(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * @param colour an ARGB colour
     * @param alpha  the new alpha value, {@code 0..255}
     * @return the colour with a replaced alpha channel
     */
    public static int withAlpha(int colour, int alpha) {
        return (colour & 0x00FFFFFF) | (MathUtils.clamp(alpha, 0, 255) << 24);
    }

    /**
     * @param colour the colour
     * @return the alpha channel of the colour
     */
    public static int alphaOf(int colour) {
        return (colour >>> 24) & 0xFF;
    }

    /**
     * @param from  the first colour
     * @param to    the second colour
     * @param delta interpolation factor
     * @return the interpolated colour
     */
    public static int lerpColour(int from, int to, float delta) {
        float clamped = MathUtils.clamp(delta, 0.0F, 1.0F);
        int a = (int) MathUtils.lerp(alphaOf(from), alphaOf(to), clamped);
        int r = (int) MathUtils.lerp((from >> 16) & 0xFF, (to >> 16) & 0xFF, clamped);
        int g = (int) MathUtils.lerp((from >> 8) & 0xFF, (to >> 8) & 0xFF, clamped);
        int b = (int) MathUtils.lerp(from & 0xFF, to & 0xFF, clamped);
        return rgba(r, g, b, a);
    }

    /**
     * @param offset    hue offset in degrees
     * @param saturation saturation, {@code 0..1}
     * @param brightness brightness, {@code 0..1}
     * @return a fully saturated ARGB colour for the current time plus offset
     */
    public static int rainbow(float offset, float saturation, float brightness) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000.0F) * 360.0F + offset;
        return hsb(hue % 360.0F, saturation, brightness);
    }

    public static int hsb(float hue, float saturation, float brightness) {
        int rgb = java.awt.Color.HSBtoRGB((hue % 360.0F) / 360.0F, MathUtils.clamp(saturation, 0.0F, 1.0F),
                MathUtils.clamp(brightness, 0.0F, 1.0F));
        return 0xFF000000 | (rgb & 0xFFFFFF);
    }

    /**
     * @param health the current health
     * @param max    the maximum health
     * @return a colour between red and green for that health ratio
     */
    public static int healthColour(float health, float max) {
        float ratio = max <= 0.0F ? 0.0F : MathUtils.clamp(health / max, 0.0F, 1.0F);
        int red = (int) (255 * (1.0F - ratio));
        int green = (int) (255 * ratio);
        return rgba(red, green, 40, 255);
    }

    // ------------------------------------------------------------------ 2D primitives

    /**
     * Draws a filled rectangle.
     *
     * @param graphics the render context
     * @param x1       the left edge
     * @param y1       the top edge
     * @param x2       the right edge
     * @param y2       the bottom edge
     * @param colour   the ARGB colour
     */
    public static void fill(GuiGraphics graphics, double x1, double y1, double x2, double y2, int colour) {
        graphics.fill((int) Math.floor(x1), (int) Math.floor(y1), (int) Math.ceil(x2), (int) Math.ceil(y2), colour);
    }

    /**
     * Draws a filled rectangle with a top left origin and a size.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param colour   the ARGB colour
     */
    public static void rect(GuiGraphics graphics, double x, double y, double width, double height, int colour) {
        fill(graphics, x, y, x + width, y + height, colour);
    }

    /**
     * Draws a vertical gradient rectangle.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param top      the colour at the top
     * @param bottom   the colour at the bottom
     */
    public static void gradient(GuiGraphics graphics, double x, double y, double width, double height, int top, int bottom) {
        graphics.fillGradient((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width), (int) Math.ceil(y + height),
                top, bottom);
    }

    /**
     * Draws a horizontal gradient rectangle in one pixel columns.
     *
     * <p>Used for the narrow accents of the interface, where a handful of columns is cheaper than any
     * cleverer approach: the wordmark underline, the switches and the sliders.</p>
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param left     the colour at the left edge
     * @param right    the colour at the right edge
     */
    public static void gradientH(GuiGraphics graphics, double x, double y, double width, double height, int left,
                                 int right) {
        int columns = Math.max(1, (int) Math.ceil(width));
        for (int column = 0; column < columns; column++) {
            float delta = (column + 0.5F) / columns;
            rect(graphics, x + column, y, 1.0D, height, lerpColour(left, right, delta));
        }
    }

    /**
     * Draws a rounded rectangle filled with a horizontal gradient.
     *
     * <p>The shape is sliced into thin columns; every column is inset by the corner radius of the rounded
     * rectangle, so the pill stays perfectly round while its colour slides from one accent to the other.
     * This is what the switches use.</p>
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param left     the colour at the left edge
     * @param right    the colour at the right edge
     * @param radius   the corner radius in pixels
     */
    public static void roundedGradientH(GuiGraphics graphics, double x, double y, double width, double height,
                                        int left, int right, int radius) {
        int r = Math.max(0, Math.min(radius, (int) Math.min(width, height) / 2));
        double slice = 2.0D;
        for (double offset = 0.0D; offset < width; offset += slice) {
            double columnWidth = Math.min(slice, width - offset);
            double centre = offset + columnWidth / 2.0D;
            double distance = Math.min(centre, width - centre);
            double inset = r == 0 || distance >= r ? 0.0D : r - Math.sqrt(Math.max(0.0D, (double) r * r - (r - distance) * (r - distance)));
            float delta = (float) (centre / Math.max(1.0D, width));
            rect(graphics, x + offset, y + inset, columnWidth, height - inset * 2.0D, lerpColour(left, right, delta));
        }
    }

    /**
     * Draws a rectangle with rounded corners.
     *
     * <p>Corner rounding is faked with row insets instead of a shader, which is exact for the small radii an
     * interface uses and costs nothing.</p>
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param colour   the ARGB colour
     * @param radius   the corner radius in pixels
     */
    public static void rounded(GuiGraphics graphics, double x, double y, double width, double height, int colour,
                               int radius) {
        int r = Math.max(0, Math.min(radius, (int) Math.min(width, height) / 2));
        if (r == 0) {
            rect(graphics, x, y, width, height, colour);
            return;
        }
        rect(graphics, x, y + r, width, height - r * 2.0D, colour);
        // The corner rows follow a real circle instead of a straight chamfer, which is what makes a large
        // radius look round rather than stepped.
        for (int row = 0; row < r; row++) {
            double dy = r - row - 0.5D;
            double inset = r - Math.sqrt(Math.max(0.0D, (double) r * r - dy * dy));
            rect(graphics, x + inset, y + row, width - inset * 2.0D, 1.0D, colour);
            rect(graphics, x + inset, y + height - row - 1.0D, width - inset * 2.0D, 1.0D, colour);
        }
    }

    /**
     * Draws a soft drop shadow behind a rounded rectangle.
     *
     * <p>Several rounded rectangles are stacked, each a little larger and weaker than the previous, which is
     * what gives a flat interface a sense of depth without a shader.</p>
     *
     * @param graphics the render context
     * @param x        the left edge of the shape
     * @param y        the top edge of the shape
     * @param width    the width of the shape
     * @param height   the height of the shape
     * @param radius   the corner radius of the shape
     * @param layers   how many shadow layers to draw
     * @param alpha    the alpha of the innermost layer, {@code 0..255}
     */
    public static void shadow(GuiGraphics graphics, double x, double y, double width, double height, int radius,
                              int layers, int alpha) {
        for (int layer = layers; layer >= 1; layer--) {
            double spread = layer * 1.5D;
            rounded(graphics, x - spread, y - spread + layer * 1.1D, width + spread * 2.0D,
                    height + spread * 2.0D, withAlpha(0x000000, Math.max(3, alpha / (layer + 1))),
                    radius + (int) Math.round(spread));
        }
    }

    /**
     * Draws a rounded rectangle with a border.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param fill     the fill colour
     * @param border   the border colour
     * @param radius   the corner radius in pixels
     */
    public static void roundedPanel(GuiGraphics graphics, double x, double y, double width, double height,
                                    int fill, int border, int radius) {
        rounded(graphics, x, y, width, height, border, radius);
        rounded(graphics, x + 1.0D, y + 1.0D, width - 2.0D, height - 2.0D, fill, Math.max(0, radius - 1));
    }

    /**
     * Draws the outline of a rectangle.
     *
     * @param graphics  the render context
     * @param x         the left edge
     * @param y         the top edge
     * @param width     the width
     * @param height    the height
     * @param colour    the ARGB colour
     * @param thickness the line thickness in pixels
     */
    public static void outline(GuiGraphics graphics, double x, double y, double width, double height, int colour,
                               double thickness) {
        double size = Math.max(1.0D, thickness);
        fill(graphics, x, y, x + width, y + size, colour);
        fill(graphics, x, y + height - size, x + width, y + height, colour);
        fill(graphics, x, y + size, x + size, y + height - size, colour);
        fill(graphics, x + width - size, y + size, x + width, y + height - size, colour);
    }

    /**
     * Draws a filled rectangle with a border.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     * @param fill     the fill colour
     * @param border   the border colour
     */
    public static void panel(GuiGraphics graphics, double x, double y, double width, double height, int fill, int border) {
        rect(graphics, x, y, width, height, fill);
        outline(graphics, x, y, width, height, border, 1.0D);
    }

    /**
     * Draws a line between two points by stepping through 1x1 rectangles. Used for tracers, breadcrumbs
     * and trajectories.
     *
     * @param graphics the render context
     * @param x1       the start x
     * @param y1       the start y
     * @param x2       the end x
     * @param y2       the end y
     * @param colour   the ARGB colour
     * @param width    the line thickness in pixels
     */
    public static void line(GuiGraphics graphics, double x1, double y1, double x2, double y2, int colour, double width) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 0.5D) {
            return;
        }
        double step = 0.5D;
        double thickness = Math.max(1.0D, width);
        for (double travelled = 0.0D; travelled <= length; travelled += step) {
            double factor = travelled / length;
            double x = x1 + dx * factor;
            double y = y1 + dy * factor;
            fill(graphics, x - thickness / 2.0D, y - thickness / 2.0D, x + thickness / 2.0D, y + thickness / 2.0D, colour);
        }
    }

    /**
     * Clips all following draws to a rectangle.
     *
     * @param graphics the render context
     * @param x        the left edge
     * @param y        the top edge
     * @param width    the width
     * @param height   the height
     */
    public static void scissor(GuiGraphics graphics, double x, double y, double width, double height) {
        graphics.enableScissor((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(x + width), (int) Math.ceil(y + height));
    }

    /**
     * Removes the active clip region.
     *
     * @param graphics the render context
     */
    public static void resetScissor(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    // ------------------------------------------------------------------ text

    /**
     * Draws a string with a drop shadow.
     *
     * @param graphics the render context
     * @param text     the text
     * @param x        the x position
     * @param y        the y position
     * @param colour   the ARGB colour
     */
    public static void text(GuiGraphics graphics, String text, double x, double y, int colour) {
        graphics.drawString(font(), sequence(text), (int) Math.floor(x), (int) Math.floor(y), colour, true);
    }

    /** Font id of the bundled UI font, defined by {@code assets/ferro/font/ui.json}. */
    private static final Identifier UI_FONT = Identifier.fromNamespaceAndPath("ferro", "ui");

    /** Styled strings, so a label that is drawn every frame is not built from scratch every frame. */
    private static final java.util.concurrent.ConcurrentHashMap<String, FormattedCharSequence> TEXT_CACHE =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** How many strings the cache keeps before it is dropped. */
    private static final int TEXT_CACHE_LIMIT = 1024;

    /**
     * Wraps a string in the bundled font so the whole interface is drawn in it instead of the vanilla bitmap
     * font. Colours, shadows and clipping stay vanilla, because the game renders the runs itself: that is
     * also why this needs no glyph atlas of our own.
     *
     * @param text the text
     * @return the styled sequence to draw or measure
     */
    public static FormattedCharSequence sequence(String text) {
        FormattedCharSequence cached = TEXT_CACHE.get(text);
        if (cached != null) {
            return cached;
        }
        if (TEXT_CACHE.size() > TEXT_CACHE_LIMIT) {
            TEXT_CACHE.clear();
        }
        FormattedCharSequence created = Component.literal(text)
                .withStyle(Style.EMPTY.withFont(new FontDescription.Resource(UI_FONT)))
                .getVisualOrderText();
        TEXT_CACHE.put(text, created);
        return created;
    }

    /**
     * Draws a string.
     *
     * @param graphics the render context
     * @param text     the text
     * @param x        the x position
     * @param y        the y position
     * @param colour   the ARGB colour
     * @param shadow   whether to draw a drop shadow
     */
    public static void text(GuiGraphics graphics, String text, double x, double y, int colour, boolean shadow) {
        graphics.drawString(font(), sequence(text), (int) Math.floor(x), (int) Math.floor(y), colour, shadow);
    }

    /**
     * Draws a string centred around a point.
     *
     * @param graphics the render context
     * @param text     the text
     * @param centerX  the x position of the centre
     * @param y        the y position
     * @param colour   the ARGB colour
     */
    public static void centeredText(GuiGraphics graphics, String text, double centerX, double y, int colour) {
        graphics.drawString(font(), sequence(text), (int) Math.floor(centerX - textWidth(text) / 2.0D),
                (int) Math.floor(y), colour, true);
    }

    /**
     * @param text the text
     * @return the rendered width of the text
     */
    public static int textWidth(String text) {
        return font().width(sequence(text));
    }

    /**
     * @return the height of a single line of text
     */
    public static int textHeight() {
        return font().lineHeight;
    }

    // ------------------------------------------------------------------ projection

    /**
     * A projected screen position.
     *
     * @param x        the x position in scaled GUI pixels
     * @param y        the y position in scaled GUI pixels
     * @param onScreen whether the point is in front of the camera and inside the frustum
     */
    public record ScreenPos(float x, float y, boolean onScreen) {
    }

    /**
     * A projected screen rectangle.
     *
     * @param minX the left edge
     * @param minY the top edge
     * @param maxX the right edge
     * @param maxY the bottom edge
     */
    public record ScreenBox(float minX, float minY, float maxX, float maxY) {

        /**
         * @return the width of the box
         */
        public float width() {
            return maxX - minX;
        }

        /**
         * @return the height of the box
         */
        public float height() {
            return maxY - minY;
        }

        /**
         * @return the centre x position
         */
        public float centerX() {
            return (minX + maxX) / 2.0F;
        }

        /**
         * @return the centre y position
         */
        public float centerY() {
            return (minY + maxY) / 2.0F;
        }
    }

    /**
     * Projects a world position onto the screen.
     *
     * @param world the world position
     * @return the projected position, or {@code null} when the point is behind the camera
     */
    public static ScreenPos project(Vec3 world) {
        Minecraft minecraft = mc();
        if (world == null || minecraft.level == null || minecraft.player == null) {
            return null;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (!camera.isInitialized()) {
            return null;
        }
        Vec3 cameraPos = camera.position();
        Quaternionf rotation = new Quaternionf(camera.rotation()).conjugate();
        Matrix4f view = new Matrix4f().rotation(rotation);
        Matrix4f projection = minecraft.gameRenderer.getProjectionMatrix(minecraft.options.fov().get().floatValue());
        Matrix4f combined = new Matrix4f(projection).mul(view);
        Vector4f point = new Vector4f((float) (world.x - cameraPos.x), (float) (world.y - cameraPos.y),
                (float) (world.z - cameraPos.z), 1.0F);
        combined.transform(point);
        if (point.w <= 0.001F) {
            return null;
        }
        float ndcX = point.x / point.w;
        float ndcY = point.y / point.w;
        float x = (ndcX * 0.5F + 0.5F) * minecraft.getWindow().getGuiScaledWidth();
        float y = (0.5F - ndcY * 0.5F) * minecraft.getWindow().getGuiScaledHeight();
        boolean onScreen = ndcX >= -1.4F && ndcX <= 1.4F && ndcY >= -1.4F && ndcY <= 1.4F;
        return new ScreenPos(x, y, onScreen);
    }

    /**
     * Interpolates the position of an entity between the previous and the current tick.
     *
     * @param entity      the entity
     * @param partialTick the frame delta in ticks
     * @return the interpolated position
     */
    public static Vec3 interpolated(Entity entity, float partialTick) {
        return new Vec3(MathUtils.lerp(entity.xOld, entity.getX(), partialTick),
                MathUtils.lerp(entity.yOld, entity.getY(), partialTick),
                MathUtils.lerp(entity.zOld, entity.getZ(), partialTick));
    }

    /**
     * Interpolates the bounding box of an entity.
     *
     * @param entity      the entity
     * @param partialTick the frame delta in ticks
     * @return the interpolated bounding box
     */
    public static AABB interpolatedBox(Entity entity, float partialTick) {
        AABB box = entity.getBoundingBox();
        double dx = MathUtils.lerp(entity.xOld, entity.getX(), partialTick) - entity.getX();
        double dy = MathUtils.lerp(entity.yOld, entity.getY(), partialTick) - entity.getY();
        double dz = MathUtils.lerp(entity.zOld, entity.getZ(), partialTick) - entity.getZ();
        return box.move(dx, dy, dz);
    }

    /**
     * Projects a bounding box to the smallest screen rectangle that contains it.
     *
     * @param worldBox the world space box
     * @return the projected box, or {@code null} when it is behind the camera
     */
    public static ScreenBox projectBox(AABB worldBox) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        int visible = 0;
        for (int corner = 0; corner < 8; corner++) {
            double x = (corner & 1) == 0 ? worldBox.minX : worldBox.maxX;
            double y = (corner & 2) == 0 ? worldBox.minY : worldBox.maxY;
            double z = (corner & 4) == 0 ? worldBox.minZ : worldBox.maxZ;
            ScreenPos projected = project(new Vec3(x, y, z));
            if (projected == null) {
                continue;
            }
            visible++;
            minX = Math.min(minX, projected.x());
            minY = Math.min(minY, projected.y());
            maxX = Math.max(maxX, projected.x());
            maxY = Math.max(maxY, projected.y());
        }
        if (visible == 0) {
            return null;
        }
        return new ScreenBox(minX, minY, maxX, maxY);
    }

    // ------------------------------------------------------------------ boxes and overlays

    /**
     * Draws a filled screen space rectangle for an entity style box.
     *
     * @param graphics the render context
     * @param box      the projected box
     * @param colour   the ARGB colour
     */
    public static void drawFilledBox(GuiGraphics graphics, ScreenBox box, int colour) {
        rect(graphics, box.minX(), box.minY(), box.width(), box.height(), colour);
    }

    /**
     * Draws the outline of a screen space box.
     *
     * @param graphics  the render context
     * @param box       the projected box
     * @param colour    the ARGB colour
     * @param thickness the line thickness
     */
    public static void drawOutlineBox(GuiGraphics graphics, ScreenBox box, int colour, double thickness) {
        outline(graphics, box.minX(), box.minY(), box.width(), box.height(), colour, thickness);
    }

    /**
     * Draws only the four corners of a screen space box.
     *
     * @param graphics  the render context
     * @param box       the projected box
     * @param colour    the ARGB colour
     * @param thickness the line thickness
     */
    public static void drawCorners(GuiGraphics graphics, ScreenBox box, int colour, double thickness) {
        double length = Math.max(2.0D, Math.min(box.width(), box.height()) * 0.25D);
        double size = Math.max(1.0D, thickness);
        fill(graphics, box.minX(), box.minY(), box.minX() + length, box.minY() + size, colour);
        fill(graphics, box.minX(), box.minY(), box.minX() + size, box.minY() + length, colour);
        fill(graphics, box.maxX() - length, box.minY(), box.maxX(), box.minY() + size, colour);
        fill(graphics, box.maxX() - size, box.minY(), box.maxX(), box.minY() + length, colour);
        fill(graphics, box.minX(), box.maxY() - size, box.minX() + length, box.maxY(), colour);
        fill(graphics, box.minX(), box.maxY() - length, box.minX() + size, box.maxY(), colour);
        fill(graphics, box.maxX() - length, box.maxY() - size, box.maxX(), box.maxY(), colour);
        fill(graphics, box.maxX() - size, box.maxY() - length, box.maxX(), box.maxY(), colour);
    }

    /**
     * Draws the twelve edges of a world box as projected lines.
     *
     * @param graphics  the render context
     * @param worldBox  the world space box
     * @param colour    the ARGB colour
     * @param thickness the line thickness
     */
    public static void drawWorldBox(GuiGraphics graphics, AABB worldBox, int colour, double thickness) {
        Vec3[] corners = new Vec3[8];
        for (int corner = 0; corner < 8; corner++) {
            corners[corner] = new Vec3((corner & 1) == 0 ? worldBox.minX : worldBox.maxX,
                    (corner & 2) == 0 ? worldBox.minY : worldBox.maxY,
                    (corner & 4) == 0 ? worldBox.minZ : worldBox.maxZ);
        }
        int[][] edges = {
                {0, 1}, {2, 3}, {4, 5}, {6, 7},
                {0, 2}, {1, 3}, {4, 6}, {5, 7},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };
        for (int[] edge : edges) {
            ScreenPos from = project(corners[edge[0]]);
            ScreenPos to = project(corners[edge[1]]);
            if (from == null || to == null) {
                continue;
            }
            line(graphics, from.x(), from.y(), to.x(), to.y(), colour, thickness);
        }
    }

    /**
     * Draws a tracer from the bottom centre of the screen to a world position.
     *
     * @param graphics  the render context
     * @param world     the target position
     * @param colour    the ARGB colour
     * @param thickness the line thickness
     */
    public static void drawTracer(GuiGraphics graphics, Vec3 world, int colour, double thickness) {
        ScreenPos target = project(world);
        if (target == null) {
            return;
        }
        line(graphics, screenWidth() / 2.0D, screenHeight() - 1.0D, target.x(), target.y(), colour, thickness);
    }

    /**
     * Draws a health bar next to a projected box.
     *
     * @param graphics the render context
     * @param box      the projected box
     * @param health   the current health
     * @param max      the maximum health
     */
    public static void drawHealthBar(GuiGraphics graphics, ScreenBox box, float health, float max) {
        double height = Math.max(4.0D, box.height());
        double left = box.minX() - 3.0D;
        fill(graphics, left - 1.0D, box.minY() - 1.0D, left + 2.0D, box.minY() + height + 1.0D, 0xFF000000);
        double ratio = max <= 0.0F ? 0.0F : MathUtils.clamp(health / max, 0.0F, 1.0F);
        double filled = height * ratio;
        fill(graphics, left, box.minY() + height - filled, left + 1.0D, box.minY() + height,
                healthColour(health, max));
    }

    // ------------------------------------------------------------------ misc

    /**
     * @return the interpolated camera position of the current frame
     */
    public static Vec3 cameraPos() {
        return mc().gameRenderer.getMainCamera().position();
    }

    /**
     * @return the frame delta in ticks
     */
    public static float partialTick() {
        return mc().getDeltaTracker().getGameTimeDeltaPartialTick(false);
    }

    /**
     * @return the local player, or {@code null}
     */
    public static net.minecraft.client.player.LocalPlayer player() {
        return mc().player;
    }

    /**
     * @return {@code true} when FERRO is loaded and the given module is enabled
     */
    public static boolean isEnabled(Class<? extends dev.ferro.client.module.Module> type) {
        Argon client = Argon.get();
        if (client == null) {
            return false;
        }
        dev.ferro.client.module.Module module = client.getModuleManager().get(type);
        return module != null && module.isEnabled();
    }

    /**
     * @return a fresh pose stack, used by modules that still want matrix maths
     */
    public static PoseStack poseStack() {
        return new PoseStack();
    }
}
