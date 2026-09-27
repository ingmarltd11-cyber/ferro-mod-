package dev.ferro.client.event.events;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ferro.client.event.Event;
import net.minecraft.world.phys.Vec3;

/**
 * Fired once per frame for world anchored overlay geometry.
 *
 * <p>FERRO deliberately renders all world space visuals (ESP, tracers, nametags, breadcrumbs,
 * trajectories) through a projected 2D overlay instead of touching the {@code RenderType} /
 * {@code VertexConsumer} pipeline. That keeps every renderer working across Minecraft point
 * releases without depending on renames such as {@code MultiBufferSource}/{@code BufferSource} or
 * {@code LevelRenderer}/{@code ShapeRenderer} helpers.</p>
 *
 * <p>Use {@code RenderUtils.worldToScreen} with {@link #getCameraPos()} to project a world
 * position onto the screen, then draw with {@code GuiGraphics}.</p>
 */
public class Render3DEvent extends Event<Render3DListener> {

    private final PoseStack poseStack;
    private final Vec3 cameraPos;
    private final float partialTick;

    /**
     * @param poseStack   a fresh pose stack for the overlay pass
     * @param cameraPos   the interpolated camera position of the current frame
     * @param partialTick frame delta in ticks
     */
    public Render3DEvent(PoseStack poseStack, Vec3 cameraPos, float partialTick) {
        super(Render3DListener.class);
        this.poseStack = poseStack;
        this.cameraPos = cameraPos;
        this.partialTick = partialTick;
    }

    /**
     * @return a fresh pose stack for the overlay pass
     */
    public PoseStack getPoseStack() {
        return poseStack;
    }

    /**
     * @return the interpolated camera position
     */
    public Vec3 getCameraPos() {
        return cameraPos;
    }

    /**
     * @return frame delta in ticks
     */
    public float getPartialTick() {
        return partialTick;
    }

    @Override
    public void call(Render3DListener listener) {
        listener.onRender3D(this);
    }
}
