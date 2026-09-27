package dev.ferro.client.event.events;

import dev.ferro.client.event.CancellableEvent;
import net.minecraft.world.entity.Entity;

/**
 * Fired at the head of {@code MultiPlayerGameMode#attack}. Cancelling it stops the vanilla attack,
 * which is how silent aura implementations swallow the client side swing/attack of their own.
 */
public class AttackEvent extends CancellableEvent<AttackListener> {

    private final Entity target;

    /**
     * @param target the entity that is about to be attacked
     */
    public AttackEvent(Entity target) {
        super(AttackListener.class);
        this.target = target;
    }

    /**
     * @return the entity that is about to be attacked
     */
    public Entity getTarget() {
        return target;
    }

    @Override
    public void call(AttackListener listener) {
        listener.onAttack(this);
    }
}
