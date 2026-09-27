package dev.ferro.client.ui;

import dev.ferro.client.utils.Log;
import dev.ferro.client.utils.RenderUtils;

/**
 * The colour themes of the interface.
 *
 * <p>A theme is a set of nine colours, two of them accents. The second accent exists so a theme can carry
 * the red to orange sweep FERRO is built around: the wordmark, the switches, the sliders and the selected
 * sidebar row all draw a gradient between {@code accent} and {@code accent2}. Themes with a single identity
 * colour simply point both accents at it.</p>
 *
 * <p>Applying a theme writes the colours into {@link RenderUtils}, which every module and both layouts
 * read, so switching a theme recolours the whole client in one call without any module having to know that
 * themes exist.</p>
 */
public enum Theme {

    /** The default FERRO look: red with an orange sweep. */
    FERRO("Ferro",
            0xFF1A0000, 0xFF120000, 0xFF240404, 0xFF3D0000, 0xFFFF1A1A, 0xFFFF7A18, 0xFFFF4D4D, 0xFFFFFFFF,
            0xFFFF9999),
    /** Molten orange on burnt black. */
    MAGMA("Magma",
            0xFF180A02, 0xFF120804, 0xFF22120A, 0xFF3A1B0C, 0xFFFF5A1F, 0xFFFFB020, 0xFFFF8A3D, 0xFFFFF7ED,
            0xFFC0A08A),
    /** Deep crimson. */
    BLOOD("Blood",
            0xFF120307, 0xFF0D0205, 0xFF1C060C, 0xFF330A14, 0xFFE11D48, 0xFFFF5C7A, 0xFFF43F5E, 0xFFFDF2F8,
            0xFFB08090),
    /** Orange fading into pink. */
    SUNSET("Sunset",
            0xFF170A04, 0xFF1C0D05, 0xFF241009, 0xFF35180E, 0xFFF97316, 0xFFFB7185, 0xFFFB923C, 0xFFFFF7ED,
            0xFFC09A7E),
    /** Polished gold. */
    GOLD("Gold",
            0xFF141001, 0xFF191404, 0xFF221B06, 0xFF33280C, 0xFFF5A524, 0xFFFFD166, 0xFFFBBF24, 0xFFFFFBEB,
            0xFFBFA374),
    /** Rose pink. */
    ROSE("Rose",
            0xFF17040A, 0xFF1C060D, 0xFF250810, 0xFF350D18, 0xFFF43F5E, 0xFFFB7185, 0xFFFB7185, 0xFFFFF1F2,
            0xFFC08A97),
    /** Acid lime. */
    LIME("Lime",
            0xFF101404, 0xFF151B06, 0xFF1A2108, 0xFF2A3310, 0xFFA3E635, 0xFF4ADE80, 0xFFBEF264, 0xFFF7FEE7,
            0xFF9CAE6B),
    /** Emerald green. */
    EMERALD("Emerald",
            0xFF04140D, 0xFF061A11, 0xFF0A2117, 0xFF123524, 0xFF22C55E, 0xFF2DD4BF, 0xFF4ADE80, 0xFFF0FDF4,
            0xFF86B79A),
    /** Forest green, darker than emerald. */
    FOREST("Forest",
            0xFF06120A, 0xFF08170D, 0xFF0C2013, 0xFF14301D, 0xFF4ADE80, 0xFFA3E635, 0xFF86EFAC, 0xFFF0FDF4,
            0xFF8FB79E),
    /** Teal crossing into violet. */
    AURORA("Aurora",
            0xFF061216, 0xFF08181D, 0xFF0C2028, 0xFF123039, 0xFF2DD4BF, 0xFFA855F7, 0xFF5EEAD4, 0xFFF0FDFA,
            0xFF7FA8B0),
    /** Bright cyan. */
    CYAN("Cyan",
            0xFF04121A, 0xFF061722, 0xFF0A1E2A, 0xFF10333F, 0xFF22D3EE, 0xFF60A5FA, 0xFF67E8F9, 0xFFECFEFF,
            0xFF7FB6C4),
    /** Ice blue and white. */
    ARCTIC("Arctic",
            0xFF0A1218, 0xFF0E1A22, 0xFF12222C, 0xFF1B323D, 0xFF7DD3FC, 0xFFE0F2FE, 0xFFBAE6FD, 0xFFF8FAFC,
            0xFF9FB6C4),
    /** Cold blue. */
    MIDNIGHT("Midnight",
            0xFF060A14, 0xFF08101F, 0xFF0F1729, 0xFF17233D, 0xFF3B82F6, 0xFF60A5FA, 0xFF60A5FA, 0xFFF8FAFC,
            0xFF8394B0),
    /** The indigo of the reference screenshot, kept for people who liked it. */
    INDIGO("Indigo",
            0xFF0B0D16, 0xFF0E1120, 0xFF141728, 0xFF1E2337, 0xFF7C5CFF, 0xFF9A80FF, 0xFF9A80FF, 0xFFFFFFFF,
            0xFF8B90A8),
    /** Deep violet. */
    VIOLET("Violet",
            0xFF11041A, 0xFF160620, 0xFF1B0A28, 0xFF2A0F3D, 0xFFA855F7, 0xFFC084FC, 0xFFC084FC, 0xFFFAF5FF,
            0xFFA98CC0),
    /** Greyscale, for people who want the client out of the way. */
    MONO("Mono",
            0xFF0A0A0A, 0xFF0F0F0F, 0xFF141414, 0xFF242424, 0xFFE5E5E5, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFAFAFA,
            0xFF8A8A8A);

    private final String displayName;
    private final int background;
    private final int sidebar;
    private final int surface;
    private final int border;
    private final int accent;
    private final int accent2;
    private final int hover;
    private final int text;
    private final int muted;

    Theme(String displayName, int background, int sidebar, int surface, int border, int accent, int accent2,
          int hover, int text, int muted) {
        this.displayName = displayName;
        this.background = background;
        this.sidebar = sidebar;
        this.surface = surface;
        this.border = border;
        this.accent = accent;
        this.accent2 = accent2;
        this.hover = hover;
        this.text = text;
        this.muted = muted;
    }

    /**
     * @return the name shown in the theme picker and stored in the config
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the window background colour
     */
    public int getBackground() {
        return background;
    }

    /**
     * @return the sidebar colour
     */
    public int getSidebar() {
        return sidebar;
    }

    /**
     * @return the card colour
     */
    public int getSurface() {
        return surface;
    }

    /**
     * @return the border colour
     */
    public int getBorder() {
        return border;
    }

    /**
     * @return the primary accent colour, the start of every gradient
     */
    public int getAccent() {
        return accent;
    }

    /**
     * @return the secondary accent colour, the end of every gradient
     */
    public int getAccent2() {
        return accent2;
    }

    /**
     * @return the accent colour used on hover
     */
    public int getHover() {
        return hover;
    }

    /**
     * @return the primary text colour
     */
    public int getText() {
        return text;
    }

    /**
     * @return the muted text colour
     */
    public int getMuted() {
        return muted;
    }

    /**
     * Applies this theme to the render utilities.
     */
    public void apply() {
        RenderUtils.setTheme(background, sidebar, surface, border, accent, accent2, hover, text, muted);
    }

    /**
     * @return the display names of every theme, in declaration order
     */
    public static String[] names() {
        Theme[] values = values();
        String[] names = new String[values.length];
        for (int index = 0; index < values.length; index++) {
            names[index] = values[index].displayName;
        }
        return names;
    }

    /**
     * @param name the display name
     * @return the matching theme, or {@link #FERRO} when the name is unknown
     */
    public static Theme byName(String name) {
        for (Theme theme : values()) {
            if (theme.displayName.equalsIgnoreCase(name)) {
                return theme;
            }
        }
        Log.warn("Unknown theme '{}', falling back to FERRO", name);
        return FERRO;
    }
}
