package net.devtech.attachment.impl.init;

import java.util.concurrent.CompletableFuture;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.devtech.attachment.ServerRef;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import org.slf4j.Logger;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class AttachmentClientInit implements ClientModInitializer {
	public static final Logger LOGGER = LogUtils.getLogger();
	@Override
	public void onInitializeClient() {
		// network id sync
		ClientLoginNetworking.registerGlobalReceiver(AttachmentInit.NETWORK_ID_SYNC, (client, handler, buf, listenerAdder) -> {
			try {
				PacketSerializerList.locked = true;
				Int2ObjectMap<PacketSerializerList<?>> lists = new Int2ObjectOpenHashMap<>();
				int size = buf.readInt();
				for(int i = 0; i < size; i++) {
					Identifier id = buf.readIdentifier();
					int netId = buf.readInt();
					PacketSerializerList<?> list = PacketSerializerList.SERIALIZER_LISTS.get(id);
					if(list != null) {
						lists.put(netId, list);
					} else {
						LOGGER.warn("No packet serializer found for id " + id);
					}
					
					Int2IntMap map = new Int2IntOpenHashMap();
					map.defaultReturnValue(-1);
					int len = buf.readInt();
					for(int j = 0; j < len; j++) {
						Identifier attachmentId = buf.readIdentifier();
						int attachmentNetId = buf.readInt();
						if(list != null) {
							map.put(attachmentNetId, list.getIndexOf(attachmentId));
						}
					}
					
					if(list != null) {
						list.idToIndexMap = map;
					}
				}
				PacketSerializerList.clientIds = lists;
				return CompletableFuture.completedFuture(new PacketByteBuf(Unpooled.EMPTY_BUFFER));
			} catch(Exception e) {
				e.printStackTrace();
				return CompletableFuture.completedFuture(null);
			}
		});
		
		// entity data sync
		ClientPlayNetworking.registerGlobalReceiver(AttachmentInit.ENTITY_SYNC, (client, handler, buf, responseSender) -> {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			client.executeSync(() -> {
				if(!handler.getConnection().isOpen()) {
					return;
				}
				PacketSerializerList.ENTITY.readPacket(copy, buf1 -> {
					int entityId = copy.readInt();
					ClientWorld world = client.world;
					if(world == null) {
						LOGGER.warn("Player is not in world unable to process " + AttachmentInit.ENTITY_SYNC);
						return null;
					}
					
					Entity entity = world.getEntityById(entityId);
					if(entity == null) {
						LOGGER.warn("Unable to find entity with id " + entityId + " unable to process " + AttachmentInit.ENTITY_SYNC);
						return null;
					}
					
					return entity;
				});
			});
		});
		
		// chunk data sync
		ClientPlayNetworking.registerGlobalReceiver(AttachmentInit.CHUNK_SYNC, (client, handler, buf, responseSender) -> {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			client.executeSync(() -> {
				if(!handler.getConnection().isOpen()) {
					return;
				}
				PacketSerializerList.CHUNK.readPacket(copy, buf1 -> {
					ClientWorld world = client.world;
					if(world == null) {
						LOGGER.warn("Player is not in world unable to process " + AttachmentInit.CHUNK_SYNC);
						return null;
					}
					
					RegistryKey<World> key = world.getRegistryKey();
					RegistryKey<World> identifier = buf1.readRegistryKey(Registry.WORLD_KEY);
					if(key != identifier) {
						LOGGER.warn("Player is in world " + key.getValue() + " expected " + identifier + " cannot process " + AttachmentInit.CHUNK_SYNC);
						return null;
					}
					
					ChunkPos pos = buf1.readChunkPos();
					WorldChunk chunk = world.getChunk(pos.x, pos.z);
					if(chunk == null) {
						LOGGER.warn("Unable to find chunk @ " + pos + " unable to process " + AttachmentInit.CHUNK_SYNC);
						return null;
					}
					
					return chunk;
				});
			});
		});
		
		// world data sync
		ClientPlayNetworking.registerGlobalReceiver(AttachmentInit.WORLD_SYNC, (client, handler, buf, responseSender) -> {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			client.executeSync(() -> {
				if(!handler.getConnection().isOpen()) {
					return;
				}
				PacketSerializerList.WORLD.readPacket(copy, buf1 -> {
					ClientWorld world = client.world;
					if(world == null) {
						LOGGER.warn("Player is not in world unable to process " + AttachmentInit.WORLD_SYNC);
						return null;
					}
					
					RegistryKey<World> key = world.getRegistryKey();
					RegistryKey<World> identifier = buf1.readRegistryKey(Registry.WORLD_KEY);
					if(key != identifier) {
						LOGGER.warn("Player is in world " + key.getValue() + " expected " + identifier + " cannot process " + AttachmentInit.WORLD_SYNC);
						return null;
					}
					
					return world;
				});
			});
		});
		
		// server data sync
		ClientPlayNetworking.registerGlobalReceiver(AttachmentInit.SERVER_SYNC, (client, handler, buf, responseSender) -> {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			client.executeSync(() -> {
				if(!handler.getConnection().isOpen()) {
					return;
				}
				PacketSerializerList.SERVER.readPacket(copy, buf1 -> {
					ClientPlayerEntity player = client.player;
					if(player == null) {
						LOGGER.warn("Player is not in world unable to process " + AttachmentInit.SERVER_SYNC);
						return null;
					}
					return ServerRef.clientConnectedTo();
				});
			});
		});
		
	}
}
