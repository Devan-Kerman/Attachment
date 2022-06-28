package net.devtech.attachment.impl.init;

import static net.devtech.attachment.impl.init.AttachmentClientInit.LOGGER;

import java.util.Map;

import com.mojang.serialization.Codec;
import io.netty.buffer.Unpooled;
import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.impl.EnumAttributeList;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import net.devtech.attachment.impl.world.WorldAttachmentsPersistentState;
import net.devtech.attachment.settings.EntityAttachmentSetting;
import net.devtech.attachment.settings.NbtAttachmentSetting;
import net.devtech.attachment.settings.WorldAttachmentSetting;
import org.spongepowered.asm.mixin.injection.At;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class AttachmentInit implements ModInitializer {
	public static final Identifier NETWORK_ID_SYNC = new Identifier("devtech:netids");
	public static final Identifier ENTITY_SYNC = new Identifier("devtech:entity_sync");
	public static final Identifier WORLD_SYNC = new Identifier("devtech:world_sync");
	
	@Override
	public void onInitialize() {
		// network id sync
		ServerLoginConnectionEvents.QUERY_START.register((handler, server, sender, synchronizer) -> {
			PacketSerializerList.locked = true;
			PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
			Map<Identifier, PacketSerializerList<?>> lists = PacketSerializerList.SERIALIZER_LISTS;
			buf.writeInt(lists.size());
			for(var entry : lists.entrySet()) {
				buf.writeIdentifier(entry.getKey());
				PacketSerializerList<?> value = entry.getValue();
				buf.writeInt(value.serverId);
				buf.writeInt(value.entries.size());
				int index = 0;
				for(PacketSerializerList.Entry<?, ?> attachmentEntry : value.entries) {
					buf.writeIdentifier(attachmentEntry.serializer().networkId());
					buf.writeInt(index++);
				}
			}
			sender.sendPacket(AttachmentInit.NETWORK_ID_SYNC, buf, synchronizer::waitFor);
		});
		
		ServerLoginNetworking.registerGlobalReceiver(NETWORK_ID_SYNC, (server, handler, understood, buf, synchronizer, responseSender) -> {
			if(!understood) {
				LOGGER.warn("An error occurred when synchronizing network ids with the client " + handler);
			}
		});
		
		// copy entity data for players
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
			if(!alive) {
				for(EnumAttributeList.Entry<Entity, ?> entry : EnumAttributeList.PLAYER_DEATH_ENTITY.entries) {
					this.devtech_copyAttachment(oldPlayer, entry.attachment());
				}
				if(newPlayer.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator()) {
					for(EnumAttributeList.Entry<Entity, ?> entry : EnumAttributeList.PLAYER_DEATH_ENTITY_KEEP_INVENTORY.entries) {
						this.devtech_copyAttachment(oldPlayer, entry.attachment());
					}
				}
			} else {
				for(AttachmentProvider.AttachmentPair<Entity, EntityAttachmentSetting> entry : Attachments.ENTITY.getAttachments()) {
					this.devtech_copyAttachment(oldPlayer, entry.attachment());
				}
			}
		});
		
		// save world data
		ServerWorldEvents.LOAD.register((server, world) -> {
			world.getPersistentStateManager().getOrCreate(compound -> new WorldAttachmentsPersistentState(world, compound),
					() -> new WorldAttachmentsPersistentState(world),
					"devtech:world_attach"
			);
		});
		
		// sync on player join
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			World world = handler.player.world;
			if(world != null) {
				Packet<?> packet = createWorldSyncPacket(world, true);
				if(packet != null) {
					sender.sendPacket(packet);
				}
			}
		});
		
		// sync when player changes worlds
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			Packet<?> packet = createWorldSyncPacket(player.world, true);
			if(packet != null) {
				ServerPlayNetworkHandler handler = player.networkHandler;
				handler.sendPacket(packet);
			}
		});
		
		// sync every once in awhile
		ServerTickEvents.START_WORLD_TICK.register(world -> {
			// slight offset to prevent us from syncing all data at the same time everyone else does their once-per-second tasks
			if((world.getTime()+3) % 20 == 0) {
				Packet<?> packet = createWorldSyncPacket(world, false);
				if(packet != null) {
					for(ServerPlayerEntity player : world.getPlayers()) {
						player.networkHandler.sendPacket(packet);
					}
				}
			}
		});
	}
	
	public static Packet<?> createWorldSyncPacket(World world, boolean force) {
		PacketByteBuf buf = PacketSerializerList.WORLD_LIST.writePacket(world, (view, idBuf) -> {
			idBuf.writeIdentifier(world.getRegistryKey().getValue());
		}, force);
		if(buf == null) {
			return null;
		}
		return ServerPlayNetworking.createS2CPacket(AttachmentInit.WORLD_SYNC, buf);
	}
	
	private <T> void devtech_copyAttachment(Entity original, Attachment<Entity, T> attachment) {
		attachment.setValue((Entity) (Object) this, attachment.getValue(original));
	}
}
