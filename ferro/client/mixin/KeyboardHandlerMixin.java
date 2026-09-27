package dev.ferro.client.mixin;

import dev.ferro.client.event.events.KeyEvent;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link KeyEvent} for every key action of the game window.
 *
 * <p>Minecraft 1.21.11 passes the key data as a {@code net.minecraft.client.input.KeyEvent} record and
 * keeps the GLFW action as a separate int, which is why this handler takes both.</p>
 */
@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    /**
     * @param window    the GLFW window handle
     * @param action    the GLFW action, {@code 0} release, {@code 1} press, {@code 2} repeat
     * @param keyEvent  the vanilla key event record
     * @param callback  mixin callback
     */
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void ferro$keyPress(long window, int action, net.minecraft.client.input.KeyEvent keyEvent,
                                CallbackInfo callback) {
        if (window != Minecraft.getInstance().getWindow().handle()) {
            return;
        }
        KeyEvent event = new KeyEvent(keyEvent.key(), keyEvent.scancode(), action, keyEvent.modifiers());
        event.post();
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}
