package dev.ferro.client.event.events;

import dev.ferro.client.event.CancellableEvent;

/**
 * Fired from the keyboard handler for every key action. Cancelling stops vanilla key handling,
 * which is used by ClickGUI to swallow bind keys while a screen is open.
 */
public class KeyEvent extends CancellableEvent<KeyListener> {

    private final int key;
    private final int scancode;
    private final int action;
    private final int modifiers;

    /**
     * @param key       GLFW key code
     * @param scancode  platform scancode
     * @param action    {@code 0} release, {@code 1} press, {@code 2} repeat
     * @param modifiers modifier bitmask
     */
    public KeyEvent(int key, int scancode, int action, int modifiers) {
        super(KeyListener.class);
        this.key = key;
        this.scancode = scancode;
        this.action = action;
        this.modifiers = modifiers;
    }

    /**
     * @return the GLFW key code
     */
    public int getKey() {
        return key;
    }

    /**
     * @return the platform scancode
     */
    public int getScancode() {
        return scancode;
    }

    /**
     * @return {@code 0} release, {@code 1} press, {@code 2} repeat
     */
    public int getAction() {
        return action;
    }

    /**
     * @return the modifier bitmask
     */
    public int getModifiers() {
        return modifiers;
    }

    /**
     * @return {@code true} for the initial press
     */
    public boolean isPress() {
        return action == 1;
    }

    @Override
    public void call(KeyListener listener) {
        listener.onKey(this);
    }
}
