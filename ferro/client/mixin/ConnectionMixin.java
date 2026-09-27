
package dev.ferro.client.mixin;

import dev.ferro.client.utils.PacketHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The only network hook in FERRO.
 *
 * <p>Both directions are handed to {@link PacketHandler} and nothing else happens here: all the policy,
 * the hold queues and the re-entrancy guard live in the handler, so there is exactly one place that
 * decides what reaches the server.</p>
 */
@Mixin(Connection.class)
public class ConnectionMixin {

    /**
     * @param packet   the outgoing packet
     * @param callback mixin callback
     */
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void ferro$send(Packet<?> packet, CallbackInfo callback) {
        if (!PacketHandler.dispatchSend(packet)) {
            callback.cancel();
        }
    }

    /**
     * @param context  the netty context
     * @param packet   the incoming packet
     * @param callback mixin callback
     */
    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void ferro$channelRead(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        if (!PacketHandler.dispatchReceive(packet)) {
            callback.cancel();
        }
    }
}
