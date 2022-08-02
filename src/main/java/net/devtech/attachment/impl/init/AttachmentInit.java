package net.devtech.attachment.impl.init;

import static net.devtech.attachment.impl.init.AttachmentClientInit.LOGGER;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;

import io.netty.buffer.Unpooled;
import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.ServerRef;
import net.devtech.attachment.impl.EnumAttributeList;
import net.devtech.attachment.impl.asm.mixin.server.MinecraftServerAccess;
import net.devtech.attachment.impl.asm.mixin.server.WorldSavePathAccess;
import net.devtech.attachment.impl.event.ServerSaveCallback;
import net.devtech.attachment.impl.serializer.CodecSerializerList;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import net.devtech.attachment.impl.world.WorldAttachmentsPersistentState;
import net.devtech.attachment.settings.EntityAttachmentSetting;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class AttachmentInit implements ModInitializer {
	public static final WorldSavePath WORLD_SAVE_PATH = WorldSavePathAccess.createWorldSavePath("devtech_attachment");
	
	public static final Identifier NETWORK_ID_SYNC = new Identifier("devtech:netids");
	public static final Identifier ENTITY_SYNC = new Identifier("devtech:entity_sync");
	public static final Identifier WORLD_SYNC = new Identifier("devtech:world_sync");
	public static final Identifier CHUNK_SYNC = new Identifier("devtech:chunk_sync");
	public static final Identifier SERVER_SYNC = new Identifier("devtech:server_sync");
	
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
				for(var entry : EnumAttributeList.PLAYER_DEATH_ENTITY.entries) {
					this.devtech_copyAttachment(oldPlayer, entry);
				}
				if(newPlayer.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || oldPlayer.isSpectator()) {
					for(var entry : EnumAttributeList.PLAYER_DEATH_ENTITY_KEEP_INVENTORY.entries) {
						this.devtech_copyAttachment(oldPlayer, entry);
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
		ServerTickEvents.END_WORLD_TICK.register(world -> {
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
		
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if((server.getTicks()+6) % 20 == 0) {
				Packet<?> packet = createServerSyncPacket(server, false);
				if(packet != null) {
					server.getPlayerManager().sendToAll(packet);
				}
			}
		});
		
		ServerSaveCallback.EVENT.register((server, session, suppressLogs, flush, force, registryManager, saveProperties) -> {
			Path directory = session.getDirectory(WORLD_SAVE_PATH);
			try {
				Files.createDirectories(directory);
			} catch(IOException e) {
				throw new RuntimeException(e);
			}
			
			NbtElement write = CodecSerializerList.SERVER.write(ServerRef.of(server));
			Path temp = directory.resolve("tmp.dat"), main = directory.resolve("devtech_attach.dat");
			if(write != null) {
				NbtCompound compound = new NbtCompound();
				compound.put("contents", write);
				try (OutputStream oos = Files.newOutputStream(temp)) {
					NbtIo.writeCompressed(compound, oos);
					Files.move(temp, main, StandardCopyOption.REPLACE_EXISTING); // if success, write to file
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			} else {
				try {
					Files.deleteIfExists(main);
					Files.deleteIfExists(temp);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

			}
		});
		
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			LevelStorage.Session session = ((MinecraftServerAccess) server).getSession();
			Path directory = session.getDirectory(WORLD_SAVE_PATH);
			Path main = directory.resolve("devtech_attach.dat");
			if(Files.exists(main)) {
				try(InputStream input = Files.newInputStream(main)) {
					NbtCompound compound = NbtIo.readCompressed(input);
					NbtElement contents = compound.get("contents");
					Objects.requireNonNull(contents, "contents must be non-null!");
					CodecSerializerList.SERVER.read(ServerRef.of(server), contents);
				} catch(IOException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}
	
	public static Packet<?> createWorldSyncPacket(World world, boolean force) {
		PacketByteBuf buf = PacketSerializerList.WORLD.writePacket(world, (view, idBuf) -> {
			idBuf.writeRegistryKey(world.getRegistryKey());
		}, force);
		if(buf == null) {
			return null;
		}
		return ServerPlayNetworking.createS2CPacket(AttachmentInit.WORLD_SYNC, buf);
	}
	
	public static Packet<?> createServerSyncPacket(MinecraftServer server, boolean force) {
		PacketByteBuf buf = PacketSerializerList.SERVER.writePacket(ServerRef.of(server), (view, idBuf) -> {}, force);
		if(buf == null) {
			return null;
		}
		return ServerPlayNetworking.createS2CPacket(AttachmentInit.SERVER_SYNC, buf);
	}
	
	private <T> void devtech_copyAttachment(Entity original, Attachment<Entity, T> attachment) {
		attachment.setValue((Entity) (Object) this, attachment.getValue(original));
	}
}
