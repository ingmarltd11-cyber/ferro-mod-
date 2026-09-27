
package dev.ferro.client.module.modules.client;

import dev.ferro.client.Argon;
import dev.ferro.client.module.Category;
import dev.ferro.client.module.Module;
import dev.ferro.client.module.setting.ModeSetting;
import dev.ferro.client.module.setting.StringSetting;
import dev.ferro.client.utils.ChatUtils;
import dev.ferro.client.utils.Notifications;

/**
 * Manages the friends list. Combat and render modules skip every player on it.
 */
public class Friends extends Module {

    private final ModeSetting action = new ModeSetting("Action", "Add", "Add", "Remove", "List");
    private final StringSetting playerName = new StringSetting("Player", "Notch", 16);

    /**
     * Creates the module.
     */
    public Friends() {
        super("Friends", Category.CLIENT, "Manage the friends list.");
        action.onChange(mode -> {
            if (isEnabled()) {
                perform(mode);
            }
        });
        addSettings(action, playerName);
    }

    /**
     * Runs an action against the friend list.
     *
     * @param mode one of {@code Add}, {@code Remove} or {@code List}
     */
    private void perform(String mode) {
        Argon client = Argon.get();
        if (client == null) {
            return;
        }
        switch (mode) {
            case "Add" -> {
                client.getFriendManager().addFriend(playerName.get());
                Notifications.info("Added " + playerName.get());
            }
            case "Remove" -> {
                client.getFriendManager().removeFriend(playerName.get());
                Notifications.info("Removed " + playerName.get());
            }
            case "List" -> ChatUtils.displayPrefixed("friends: " + String.join(", ", client.getFriendManager().getFriends()));
            default -> {
            }
        }
    }
}
