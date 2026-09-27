package dev.ferro.client.event.events;

import dev.ferro.client.event.Listener;

/**
 * Called right before the player attacks an entity.
 */
public interface AttackListener extends Listener {

    /**
     * @param event the event instance
     */
    void onAttack(AttackEvent event);
}
