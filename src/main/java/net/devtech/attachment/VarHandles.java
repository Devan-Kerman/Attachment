package net.devtech.attachment;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Use VarHandles to access the fields for atomic support and for when I'm too lazy to use an accessor interface.
 */
@ApiStatus.Internal
class VarHandles {
	public static final VarHandle ENTITY, WORLD, NBT, WORLD_CHUNK, SERVER;
	
	/**
	 * null when on server
	 */
	@Nullable
	public static final VarHandle CLIENT_PLAYER_ENTITY_SERVER_DATA;
	
	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			ENTITY = handle(lookup, Entity.class);
			WORLD = handle(lookup, World.class);
			NBT = handle(lookup, NbtCompound.class);
			WORLD_CHUNK = handle(lookup, WorldChunk.class);
			SERVER = handle(lookup, MinecraftServer.class);
			if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
				CLIENT_PLAYER_ENTITY_SERVER_DATA = handle(lookup, ClientPlayerEntity.class);
			} else {
				CLIENT_PLAYER_ENTITY_SERVER_DATA = null;
			}
		} catch(NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static VarHandle handle(MethodHandles.Lookup lookup, Class<?> cls) throws NoSuchFieldException, IllegalAccessException {
		return lookup.findVarHandle(cls, "devtech_attach", Object[].class);
	}
}
