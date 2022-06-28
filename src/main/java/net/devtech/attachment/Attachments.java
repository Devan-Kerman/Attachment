package net.devtech.attachment;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import net.devtech.attachment.impl.item.NbtMap;
import net.devtech.attachment.mixin.world.ChunkRendererRegionAccess;
import net.devtech.attachment.settings.EntityAttachmentSetting;
import net.devtech.attachment.settings.NbtAttachmentSetting;
import net.devtech.attachment.settings.WorldAttachmentSetting;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.api.transfer.v1.storage.TransferVariant;

/**
 * Included/Provided attachments
 */
@SuppressWarnings("UnstableApiUsage")
public class Attachments {
	/**
	 * Attach custom data to an entity, allows for synchronization & serialization, as well as custom behavior on player respawn
	 */
	public static final AttachmentProvider.Atomic<Entity, EntityAttachmentSetting> ENTITY = AttachmentProvider.atomic(VarHandles.ENTITY);
	
	/**
	 * Attach custom data to a world, such information can be accessed from
	 * {@link FabricBakedModel#emitBlockQuads(BlockRenderView, BlockState, BlockPos, Supplier, RenderContext)}
	 * and anywhere else you have a {@link World}
	 */
	public static final AttachmentProvider.Atomic<BlockRenderView, WorldAttachmentSetting> WORLD = AttachmentProvider.atomic(view -> {
		World world = from(view);
		return (Object[]) VarHandles.WORLD.getVolatile(world);
	}, (obj, expected, set) -> {
		World world = from(obj);
		return VarHandles.WORLD.compareAndSet(world, expected, set);
	});
	
	/**
	 * Attach custom data to a nbt compound, allows for auto-serialization
	 * @see ItemStack#getOrCreateNbt()
	 * @see TransferVariant#copyNbt()
	 */
	public static final AttachmentProvider<NbtCompound, NbtAttachmentSetting> NBT = AttachmentProvider.simple(compound -> {
		NbtMap.initRead(compound);
		return (Object[]) VarHandles.NBT.get(compound);
	}, (compound, objects) -> {
		NbtMap.initWrite(compound);
		VarHandles.NBT.set(compound, objects);
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
		if(view instanceof World w) {
			if(!allowWorldCompatibility && WARNED_CLASSES.add(view.getClass())) {
				LOGGER.warn("Unknown World Type "
				            + view.getClass()
				            + " world attachments may not work! To silence this error call "
				            + Attachments.class
				            + "#enableWorldCompat or use -Dattach.enable.world_compat=true");
			}
			
			world = w;
		}
		
		if(view instanceof ServerWorldAccess a) {
			world = a.toServerWorld();
		} else if(view instanceof ChunkRendererRegionAccess w) {
			world = w.getWorld();
		}
		
		Objects.requireNonNull(world, "Unable to find World for " + view.getClass());
		return world;
	}
}
