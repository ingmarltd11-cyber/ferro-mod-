
package dev.ferro.client.module.modules.client;

import dev.ferro.client.Argon;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.ChatUtils;
import dev.ferro.client.utils.Log;
import dev.ferro.client.utils.Notifications;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Saves, loads and lists FERRO config profiles.
 *
 * <p>Changing the {@code Action} setting performs the action immediately, which is how a client
 * without a button widget offers commands. Profiles live in {@code config/ferro/profiles}.</p>
 */
public class Config extends Module {

    private final ModeSetting action = new ModeSetting("Action", "Save", "Save", "Load", "List");
    private final StringSetting profile = new StringSetting("Profile", "default", 32);

    /**
     * Creates the module.
     */
    public Config() {
        super("Config", Category.CLIENT, "Manage FERRO config profiles.");
        action.onChange(mode -> {
            if (isEnabled()) {
                perform(mode);
            }
        });
        addSettings(action, profile);
    }

    /**
     * @return the directory that holds the profiles
     */
    public static Path profileDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("ferro/profiles");
    }

    /**
     * @param name the profile name
     * @return the file for that profile
     */
    public static Path profileFile(String name) {
        return profileDirectory().resolve(name + ".json");
    }

    /**
     * Runs an action.
     *
     * @param mode one of {@code Save}, {@code Load} or {@code List}
     */
    private void perform(String mode) {
        switch (mode) {
            case "Save" -> save();
            case "Load" -> load();
            case "List" -> list();
            default -> {
            }
        }
    }

    /**
     * Copies the live config into the profile with the given name.
     *
     * @param name the profile name
     */
    public static void saveProfile(String name) {
        if (name == null || name.isBlank()) {
            Notifications.error("Profile name is empty");
            return;
        }
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        client.getModuleManager().save();
        try {
            Files.createDirectories(profileDirectory());
            Files.copy(dev.ferro.client.module.ModuleManager.configPath(), profileFile(name),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Notifications.info("Saved profile " + name);
        } catch (IOException exception) {
            Log.error("Failed to save profile " + name, exception);
            Notifications.error("Could not save profile");
        }
    }

    /**
     * Copies the named profile back over the live config and reloads it.
     *
     * @param name the profile name
     */
    public static void loadProfile(String name) {
        if (name == null || name.isBlank()) {
            Notifications.error("Profile name is empty");
            return;
        }
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        Path file = profileFile(name);
        if (!Files.exists(file)) {
            Notifications.error("Profile " + name + " does not exist");
            return;
        }
        try {
            Files.copy(file, dev.ferro.client.module.ModuleManager.configPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            client.getModuleManager().disableAll();
            client.getModuleManager().load();
            Notifications.info("Loaded profile " + name);
        } catch (IOException exception) {
            Log.error("Failed to load profile " + name, exception);
            Notifications.error("Could not load profile");
        }
    }

    /**
     * Copies the live config into the currently selected profile.
     */
    public void save() {
        saveProfile(profile.get());
    }

    /**
     * Copies the currently selected profile back over the live config and reloads it.
     */
    public void load() {
        loadProfile(profile.get());
    }

    /**
     * @return the names of every saved profile, sorted alphabetically
     */
    public static List<String> listProfiles() {
        try {
            Files.createDirectories(profileDirectory());
            try (Stream<Path> stream = Files.list(profileDirectory())) {
                return stream.filter(path -> path.getFileName().toString().endsWith(".json"))
                        .map(path -> path.getFileName().toString().replace(".json", ""))
                        .sorted()
                        .toList();
            }
        } catch (IOException exception) {
            Log.error("Failed to list profiles", exception);
            return List.of();
        }
    }

    /**
     * Lists every profile in chat and as notifications.
     */
    public void list() {
        List<String> names = listProfiles();
        if (names.isEmpty()) {
            ChatUtils.displayPrefixed("no profiles yet");
            return;
        }
        ChatUtils.displayPrefixed("profiles: " + String.join(", ", names));
    }
}
