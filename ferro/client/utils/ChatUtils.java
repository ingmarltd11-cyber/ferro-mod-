package dev.ferro.client.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Chat and command helpers.
 */
public final class ChatUtils {

    private ChatUtils() {
    }

    /**
     * Sends a chat message to the server.
     *
     * @param message the message
     */
    public static void sendChat(String message) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null && !message.isBlank()) {
            connection.sendChat(message);
        }
    }

    /**
     * Sends a command to the server.
     *
     * @param command the command, with or without a leading slash
     */
    public static void sendCommand(String command) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        String cleaned = command.startsWith("/") ? command.substring(1) : command;
        if (!cleaned.isBlank()) {
            connection.sendCommand(cleaned);
        }
    }

    /**
     * Shows a message in the local chat window without sending anything to the server.
     *
     * @param message the message
     */
    public static void display(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(message), false);
        }
    }

    /**
     * Shows a message prefixed with the client name.
     *
     * @param message the message
     */
    public static void displayPrefixed(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal("\u00a7cFERRO \u00a77\u00bb \u00a7f" + message), false);
        }
    }
}
