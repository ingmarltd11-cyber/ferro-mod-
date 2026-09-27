package dev.ferro.client.mixin;

import dev.ferro.client.event.events.MouseEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link MouseEvent} for every mouse button action of the game window.
 */
@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    /**
     * @param window    the GLFW window handle
     * @param buttonInfo the vanilla mouse button record
     * @param action    the GLFW action
     * @param callback  mixin callback
     */
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void ferro$onButton(long window, net.minecraft.client.input.MouseButtonInfo buttonInfo, int action,
                                CallbackInfo callback) {
        if (window != Minecraft.getInstance().getWindow().handle()) {
            return;
        }
        MouseEvent event = new MouseEvent(buttonInfo.button(), action, buttonInfo.modifiers());
        event.post();
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}
