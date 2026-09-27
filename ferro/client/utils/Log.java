package dev.ferro.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin logging facade. FERRO never prints to {@code System.out}; every diagnostic goes through
 * SLF4J so it ends up in the normal Minecraft log file with a consistent prefix.
 */
public final class Log {

    private static final Logger LOGGER = LoggerFactory.getLogger("FERRO");

    private Log() {
    }

    /**
     * @param message {@code {}}-formatted message
     * @param args    format arguments
     */
    public static void info(String message, Object... args) {
        LOGGER.info(message, args);
    }

    /**
     * @param message {@code {}}-formatted message
     * @param args    format arguments
     */
    public static void warn(String message, Object... args) {
        LOGGER.warn(message, args);
    }

    /**
     * @param message {@code {}}-formatted message
     * @param args    format arguments
     */
    public static void error(String message, Object... args) {
        LOGGER.error(message, args);
    }

    /**
     * @param message   {@code {}}-formatted message
     * @param throwable the exception to attach
     */
    public static void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }

    /**
     * @param message {@code {}}-formatted message
     * @param args    format arguments
     */
    public static void debug(String message, Object... args) {
        LOGGER.debug(message, args);
    }
}
