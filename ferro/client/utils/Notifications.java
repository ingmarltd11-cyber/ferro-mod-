package dev.ferro.client.utils;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A small queue of client side messages that the HUD can render as toasts.
 */
public final class Notifications {

    /** How long a notification stays visible, in milliseconds. */
    public static final long DEFAULT_DURATION = 3500L;

    /**
     * A single notification.
     *
     * @param message   the text
     * @param createdAt the creation timestamp
     * @param duration  how long it stays visible in milliseconds
     * @param colour    the accent colour
     */
    public record Note(String message, long createdAt, long duration, int colour) {

        /**
         * @return {@code true} when the note has expired
         */
        public boolean expired() {
            return System.currentTimeMillis() - createdAt > duration;
        }

        /**
         * @return the age of the note in milliseconds
         */
        public long age() {
            return System.currentTimeMillis() - createdAt;
        }
    }

    private static final List<Note> NOTES = new CopyOnWriteArrayList<>();

    private Notifications() {
    }

    /**
     * Adds a notification.
     *
     * @param message the text
     * @param colour  the accent colour
     */
    public static void add(String message, int colour) {
        NOTES.add(new Note(message, System.currentTimeMillis(), DEFAULT_DURATION, colour));
        while (NOTES.size() > 8) {
            NOTES.remove(0);
        }
    }

    /**
     * Adds an informational notification.
     *
     * @param message the text
     */
    public static void info(String message) {
        add(message, RenderUtils.ACCENT);
    }

    /**
     * Adds a notification that a module was toggled.
     *
     * @param module the module name
     * @param on     the new state
     */
    public static void toggle(String module, boolean on) {
        add(module + (on ? " enabled" : " disabled"), on ? RenderUtils.ACCENT : RenderUtils.TEXT_DISABLED);
    }

    /**
     * Adds a warning notification.
     *
     * @param message the text
     */
    public static void warn(String message) {
        add(message, 0xFFFFAA00);
    }

    /**
     * Adds an error notification.
     *
     * @param message the text
     */
    public static void error(String message) {
        add(message, 0xFFFF3333);
    }

    /**
     * @return the live notifications, oldest first
     */
    public static List<Note> getNotes() {
        NOTES.removeIf(Note::expired);
        return NOTES;
    }

    /**
     * Removes every notification.
     */
    public static void clear() {
        NOTES.clear();
    }
}
