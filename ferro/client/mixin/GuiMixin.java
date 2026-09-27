package dev.ferro.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ferro.client.event.events.Render2DEvent;
import dev.ferro.client.event.events.Render3DEvent;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fires the two render events once per frame, after the vanilla HUD has drawn so that overlays sit
 * on top of it.
 */
@Mixin(Gui.class)
public class GuiMixin {

    /**
     * @param graphics     the GUI render context
     * @param deltaTracker frame delta information
     * @param callback     mixin callback
     */
    @Inject(method = "render", at = @At("RETURN"))
    private void ferro$render(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo callback) {
        new Render2DEvent(graphics, deltaTracker).post();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().position();
        new Render3DEvent(new PoseStack(), cameraPos, partialTick).post();
    }
}
