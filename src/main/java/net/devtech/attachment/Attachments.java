package net.devtech.attachment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import net.devtech.attachment.impl.item.NbtMap;
import net.devtech.attachment.impl.asm.mixin.chunk.ReadOnlyChunkAccess;
import net.devtech.attachment.impl.asm.mixin.world.ChunkRendererRegionAccess;
import net.devtech.attachment.settings.ChunkAttachmentSetting;
import net.devtech.attachment.settings.EntityAttachmentSetting;
import net.devtech.attachment.settings.NbtAttachmentSetting;
import net.devtech.attachment.settings.ServerAttachmentSetting;
import net.devtech.attachment.settings.WorldAttachmentSetting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

/**
 * Included/Provided attachments
 */
@SuppressWarnings ("UnstableApiUsage")
public class Attachments {
	/**
	 * Attach custom data to an entity, allows for synchronization & serialization, as well as custom behavior on
	 * player
	 * respawn
	 */
	public static final AttachmentProvider.Atomic<Entity, EntityAttachmentSetting> ENTITY = AttachmentProvider.atomic(
		VarHandles.ENTITY);
	public static final AttachmentProvider.Atomic<Chunk, ChunkAttachmentSetting> CHUNK = AttachmentProvider.atomic(
		chunk -> {
			WorldChunk from = from(chunk);
			return (Object[]) VarHandles.WORLD_CHUNK.getVolatile(from);
		}, (obj, expected, set) -> {
			WorldChunk world = from(obj);
			return VarHandles.WORLD_CHUNK.compareAndSet(world, expected, set);
		});

	public static final AttachmentProvider.Atomic<ServerRef, ServerAttachmentSetting> SERVER =
		AttachmentProvider.atomic(
		server -> {
			if (server instanceof ServerRef.Server s) {
				return (Object[]) VarHandles.SERVER.getVolatile(s.reference());
			} else {
				return (Object[]) VarHandles.CLIENT_PLAYER_ENTITY_SERVER_DATA.getVolatile(server.getRef());
			}
		}, (obj, expected, set) -> {
			if (obj instanceof ServerRef.Server s) {
				return VarHandles.SERVER.compareAndSet(s.reference(), expected, set);
			} else {
				return VarHandles.CLIENT_PLAYER_ENTITY_SERVER_DATA.compareAndSet(obj.getRef(), expected, set);
			}
		});

	/**
	 * Attach custom data to a world, such information can be accessed from
	 * {@link FabricBakedModel#emitBlockQuads(BlockRenderView, BlockState, BlockPos, Supplier, RenderContext)} and
	 * anywhere else you have a {@link World}
	 */
	public static final AttachmentProvider.Atomic<BlockRenderView, WorldAttachmentSetting> WORLD =
		AttachmentProvider.atomic(
		view -> {
			World world = from(view);
			return (Object[]) VarHandles.WORLD.getVolatile(world);
		}, (obj, expected, set) -> {
			World world = from(obj);
			return VarHandles.WORLD.compareAndSet(world, expected, set);
		});

	/**
	 * Attach custom data to a nbt compound, allows for auto-serialization
	 *
	 * @see ItemStack#getOrCreateNbt()
	 * @see TransferVariant#copyNbt()
	 */
	public static final AttachmentProvider.Atomic<NbtCompound, NbtAttachmentSetting> NBT = AttachmentProvider.atomic(
		compound -> {
			synchronized (compound) {
				NbtMap.initRead(compound);
			}
			return (Object[]) VarHandles.NBT.get(compound);
		}, (compound, expected, set) -> {
			synchronized (compound) {
				NbtMap.initWrite(compound);
			}
			return VarHandles.NBT.compareAndSet(compound, expected, set);
		});

	private static final Set<Class<?>> WARNED_CLASSES = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private static final Logger LOGGER = LogUtils.getLogger();
	private static boolean allowWorldCompatibility = Boolean.getBoolean("attach.enable.world_compat");

	static {
		WARNED_CLASSES.add(ServerWorld.class);
		WARNED_CLASSES.add(ClientWorld.class);
	}

	public static void enableWorldCompat() {
		allowWorldCompatibility = true;
	}

	@NotNull
	public static World from(BlockRenderView view) {
		Objects.requireNonNull(view, "view is null");

		World world = null;
		if (view instanceof World w) {
			if (!allowWorldCompatibility && WARNED_CLASSES.add(view.getClass())) {
				LOGGER.warn("Unknown World Type " +
				            view.getClass() +
				            " world attachments may not work! To silence this error call " +
				            Attachments.class +
				            "#enableWorldCompat or use -Dattach.enable.world_compat=true");
			}

			world = w;
		}

		if (view instanceof ServerWorldAccess a) {
			world = a.toServerWorld();
		} else if (view instanceof ChunkRendererRegionAccess w) {
			world = w.getWorld();
		}

		Objects.requireNonNull(world, "Unable to find World for " + view.getClass());
		return world;
	}

	public static WorldChunk from(Chunk chunk) {
		if (chunk instanceof WorldChunk worldChunk) {
			return worldChunk;
		} else if (chunk instanceof ReadOnlyChunkAccess access) {
			return access.getWrapped();
		} else {
			return null;
		}
	}

}
