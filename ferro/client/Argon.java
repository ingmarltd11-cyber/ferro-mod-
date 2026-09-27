
package dev.ferro.client;

import dev.ferro.client.event.EventManager;
import dev.ferro.client.event.events.KeyEvent;
import dev.ferro.client.event.events.KeyListener;
import dev.ferro.client.event.events.MouseEvent;
import dev.ferro.client.event.events.MouseListener;
import dev.ferro.client.event.events.TickEvent;
import dev.ferro.client.module.ModuleManager;
import dev.ferro.client.utils.FriendManager;
import dev.ferro.client.utils.Log;
import dev.ferro.client.module.modules.client.ClickGui;
import dev.ferro.client.ui.Theme;
import dev.ferro.client.utils.PacketHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/**
 * FERRO's entry point and service locator.
 *
 * <p>This class owns the global services: the {@link EventManager}, the {@link ModuleManager}, the
 * {@link FriendManager} and the packet pipeline. It is registered as a {@code client} entrypoint in
 * {@code fabric.mod.json}, so {@link #onInitializeClient()} runs after Minecraft has been constructed and
 * before the first world is loaded.</p>
 */
public final class Argon implements ClientModInitializer, KeyListener, MouseListener {

    /** Mod id, also used for the config folder and the logger name. */
    public static final String MOD_ID = "ferro";

    /** Human readable client name. */
    public static final String NAME = "FERRO";

    /** Client version, kept in sync with {@code gradle.properties} manually. */
    public static final String VERSION = "1.0.0";

    private static Argon instance;

    private final EventManager eventManager = EventManager.getInstance();
    private final ModuleManager moduleManager = new ModuleManager();
    private final FriendManager friendManager = new FriendManager();

    private int tickCounter;

    /**
     * @return the singleton client instance, never {@code null} after client init
     */
    public static Argon get() {
        return instance;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        PacketHandler.validateEnvironment();
        moduleManager.load();
        friendManager.load();
        ClickGui clickGui = moduleManager.get(ClickGui.class);
        if (clickGui != null) {
            Theme.byName(clickGui.getThemeName()).apply();
        }
        eventManager.add(KeyEvent.class, this);
        eventManager.add(MouseEvent.class, this);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            new TickEvent(++tickCounter).post();
            PacketHandler.tick();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            moduleManager.save();
            friendManager.save();
            PacketHandler.shutdown();
            Log.info("FERRO shutting down: {}", PacketHandler.summary());
        });
        Log.info("{} {} initialised with {} modules", NAME, VERSION, moduleManager.size());
    }

    @Override
    public void onKey(KeyEvent event) {
        if (event.isPress()) {
            moduleManager.onKey(event.getKey());
            return;
        }
        if (event.getAction() == 0) {
            moduleManager.onKeyRelease(event.getKey());
        }
    }

    @Override
    public void onMouse(MouseEvent event) {
        if (event.isPress()) {
            moduleManager.onMouse(event.getButton());
            return;
        }
        if (event.getAction() == 0) {
            moduleManager.onMouseRelease(event.getButton());
        }
    }

    /**
     * @return the global event bus
     */
    public EventManager getEventManager() {
        return eventManager;
    }

    /**
     * @return the module registry
     */
    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    /**
     * @return the friends and enemies registry
     */
    public FriendManager getFriendManager() {
        return friendManager;
    }
}
