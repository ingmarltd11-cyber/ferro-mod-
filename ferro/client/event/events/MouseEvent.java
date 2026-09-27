package dev.ferro.client.event.events;

import dev.ferro.client.event.CancellableEvent;

/**
 * Fired from the mouse handler for button presses and releases.
 */
public class MouseEvent extends CancellableEvent<MouseListener> {

    private final int button;
    private final int action;
    private final int modifiers;

    /**
     * @param button    GLFW mouse button index
     * @param action    {@code 0} release, {@code 1} press
     * @param modifiers modifier bitmask
     */
    public MouseEvent(int button, int action, int modifiers) {
        super(MouseListener.class);
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
    }

    /**
     * @return the GLFW mouse button index
     */
    public int getButton() {
        return button;
    }

    /**
     * @return {@code 0} release, {@code 1} press
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
     * @return {@code true} for a button press
     */
    public boolean isPress() {
        return action == 1;
    }

    @Override
    public void call(MouseListener listener) {
        listener.onMouse(this);
    }
}
