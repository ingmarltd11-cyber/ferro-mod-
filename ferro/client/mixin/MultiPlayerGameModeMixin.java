
package dev.ferro.client.mixin;

import dev.ferro.client.event.events.AttackEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires {@link AttackEvent} before the vanilla attack runs, and lets listeners cancel it.
 */
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

    /**
     * @param player   the attacking player
     * @param target   the entity being attacked
     * @param callback mixin callback
     */
    @Inject(method = "attack", at = @At("HEAD"), cancellable = true)
    private void ferro$attack(Player player, Entity target, CallbackInfo callback) {
        AttackEvent event = new AttackEvent(target);
        event.post();
        if (event.isCancelled()) {
            callback.cancel();
        }
    }
}
