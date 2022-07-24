package net.devtech.attachment.impl.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Called on autosave
 */
public interface ServerSaveCallback {
	Event<ServerSaveCallback> EVENT = EventFactory.createArrayBacked(ServerSaveCallback.class, callbacks -> (server, session, suppressLogs, flush, force, registryManager, saveProperties) -> {
		for(ServerSaveCallback callback : callbacks) {
			callback.onSave(server, session, suppressLogs, flush, force, registryManager, saveProperties);
		}
	});
	
	/**
	 * Invoked after worlds are saved
	 * @see MinecraftServer#save(boolean, boolean, boolean)
	 */
	void onSave(
			MinecraftServer server,
			LevelStorage.Session session,
			boolean suppressLogs,
			boolean flush,
			boolean force,
			DynamicRegistryManager registryManager,
			SaveProperties saveProperties);
}
