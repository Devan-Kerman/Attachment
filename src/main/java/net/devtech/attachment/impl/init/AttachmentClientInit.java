package net.devtech.attachment.impl.init;

import java.util.concurrent.CompletableFuture;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import org.slf4j.Logger;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.World;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

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
				PacketSerializerList.ENTITY_LIST.readPacket(copy, buf1 -> {
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
		
		// world data sync
		ClientPlayNetworking.registerGlobalReceiver(AttachmentInit.WORLD_SYNC, (client, handler, buf, responseSender) -> {
			PacketByteBuf copy = new PacketByteBuf(buf.copy());
			client.executeSync(() -> {
				if(!handler.getConnection().isOpen()) {
					return;
				}
				PacketSerializerList.WORLD_LIST.readPacket(copy, buf1 -> {
					ClientWorld world = client.world;
					if(world == null) {
						LOGGER.warn("Player is not in world unable to process " + AttachmentInit.WORLD_SYNC);
						return null;
					}
					
					RegistryKey<World> key = world.getRegistryKey();
					Identifier identifier = buf1.readIdentifier();
					if(!key.getValue().equals(identifier)) {
						LOGGER.warn("Player is in world " + key.getValue() + " expected " + identifier + " cannot process " + AttachmentInit.WORLD_SYNC);
						return null;
					}
					
					return world;
				});
			});
		});
	}
}
