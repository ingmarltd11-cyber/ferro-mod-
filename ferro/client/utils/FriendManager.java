package dev.ferro.client.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.player.Player;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of friends and enemies. Combat and render modules consult this before acting on a
 * player, so it is deliberately cheap and lock free.
 */
public final class FriendManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Set<String> friends = ConcurrentHashMap.newKeySet();
    private final Set<String> enemies = ConcurrentHashMap.newKeySet();

    /**
     * @return the config file path
     */
    public static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("ferro/social.json");
    }

    /**
     * Adds a friend.
     *
     * @param name the player name
     */
    public void addFriend(String name) {
        if (name != null && !name.isBlank()) {
            friends.add(name);
        }
    }

    /**
     * Removes a friend.
     *
     * @param name the player name
     */
    public void removeFriend(String name) {
        friends.remove(name);
    }

    /**
     * Adds an enemy.
     *
     * @param name the player name
     */
    public void addEnemy(String name) {
        if (name != null && !name.isBlank()) {
            enemies.add(name);
        }
    }

    /**
     * Removes an enemy.
     *
     * @param name the player name
     */
    public void removeEnemy(String name) {
        enemies.remove(name);
    }

    /**
     * @param name the player name
     * @return {@code true} when the player is a friend
     */
    public boolean isFriend(String name) {
        return name != null && friends.contains(name);
    }

    /**
     * @param player the player
     * @return {@code true} when the player is a friend
     */
    public boolean isFriend(Player player) {
        return player != null && isFriend(player.getName().getString());
    }

    /**
     * @param name the player name
     * @return {@code true} when the player is an enemy
     */
    public boolean isEnemy(String name) {
        return name != null && enemies.contains(name);
    }

    /**
     * @param player the player
     * @return {@code true} when the player is an enemy
     */
    public boolean isEnemy(Player player) {
        return player != null && isEnemy(player.getName().getString());
    }

    /**
     * @return an unmodifiable view of the friend names
     */
    public Set<String> getFriends() {
        return Collections.unmodifiableSet(friends);
    }

    /**
     * @return an unmodifiable view of the enemy names
     */
    public Set<String> getEnemies() {
        return Collections.unmodifiableSet(enemies);
    }

    /**
     * Persists both lists to disk.
     */
    public void save() {
        JsonObject root = new JsonObject();
        JsonArray friendArray = new JsonArray();
        friends.forEach(friendArray::add);
        JsonArray enemyArray = new JsonArray();
        enemies.forEach(enemyArray::add);
        root.add("friends", friendArray);
        root.add("enemies", enemyArray);
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Log.error("Failed to save friends and enemies", exception);
        }
    }

    /**
     * Reads both lists from disk. Missing files are ignored.
     */
    public void load() {
        if (!Files.exists(path())) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(path(), StandardCharsets.UTF_8)).getAsJsonObject();
            friends.clear();
            enemies.clear();
            if (root.has("friends")) {
                root.getAsJsonArray("friends").forEach(element -> friends.add(element.getAsString()));
            }
            if (root.has("enemies")) {
                root.getAsJsonArray("enemies").forEach(element -> enemies.add(element.getAsString()));
            }
        } catch (Throwable throwable) {
            Log.error("Failed to load friends and enemies", throwable);
        }
    }
}
