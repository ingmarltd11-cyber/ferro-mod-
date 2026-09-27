package dev.ferro.client.module;

/**
 * Module categories. Every category owns one draggable frame in the ClickGUI, in declaration order.
 */
public enum Category {

    /** Combat and PvP modules. */
    COMBAT("Combat"),
    /** Movement modules. */
    MOVEMENT("Movement"),
    /** Visual modules. */
    RENDER("Render"),
    /** Player automation modules. */
    PLAYER("Player"),
    /** World interaction modules. */
    WORLD("World"),
    /** Miscellaneous modules. */
    MISC("Misc"),
    /** Client management modules. */
    CLIENT("Client");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @return the label shown in the ClickGUI
     */
    public String getDisplayName() {
        return displayName;
    }
}
