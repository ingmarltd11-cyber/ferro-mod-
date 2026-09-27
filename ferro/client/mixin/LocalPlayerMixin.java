package dev.ferro.client.mixin;

import dev.ferro.client.event.events.PlayerTickEvent;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link PlayerTickEvent} at the head of the local player tick.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    /**
     * @param callback mixin callback
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void ferro$tick(CallbackInfo callback) {
        new PlayerTickEvent().post();
    }
}
