package net.devtech.attachment.impl.serializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.mojang.logging.LogUtils;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.ServerRef;
import net.devtech.attachment.impl.DirtyableAttachment;
import net.devtech.attachment.impl.init.AttachmentInit;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.chunk.Chunk;

public class PacketSerializerList<O> {
	public static final Logger LOGGER = LogUtils.getLogger();
	private static final AtomicInteger ID = new AtomicInteger();
	public static final Map<Identifier, PacketSerializerList<?>> SERIALIZER_LISTS = new ConcurrentHashMap<>();
	public static final PacketSerializerList<Entity> ENTITY = new PacketSerializerList<>(Attachments.ENTITY, AttachmentInit.ENTITY_SYNC);
	public static final PacketSerializerList<BlockRenderView> WORLD = new PacketSerializerList<>(Attachments.WORLD, AttachmentInit.WORLD_SYNC);
	public static final PacketSerializerList<Chunk> CHUNK = new PacketSerializerList<>(Attachments.CHUNK, AttachmentInit.CHUNK_SYNC);
	public static final PacketSerializerList<ServerRef> SERVER = new PacketSerializerList<>(Attachments.SERVER, AttachmentInit.SERVER_SYNC);
	
	public static Int2ObjectMap<PacketSerializerList<?>> clientIds;
	
	public static volatile boolean locked;
	
	public final List<Entry<O, ?>> entries = new Vector<>();
	public Int2IntMap idToIndexMap;
	
	final Identifier id;
	public final int serverId = ID.getAndIncrement();
	
	public record Entry<O, T>(Attachment<O, T> attachment, ContextIdentifiedTrackedDataHandler<O, T> serializer) {}
	
	public PacketSerializerList(AttachmentProvider<O, ?> provider, Identifier id) {
		this.id = id;
		Set<Identifier> identifiers = Collections.newSetFromMap(new ConcurrentHashMap<>());
		provider.registerAndRunListener((attachment, behavior) -> {
			boolean added = false;
			for(AttachmentSetting setting : behavior) {
				if(setting instanceof ContextIdentifiedTrackedDataHandler c) {
					if(added) {
						throw new IllegalArgumentException("Cannot have multiple serializers for single attachment!");
					}
					if(!identifiers.add(c.networkId())) {
						throw new IllegalArgumentException("Duplicate serializers with id " + c.networkId());
					}
					if(locked) {
						throw new IllegalStateException("cannot register networked attachments after player join!");
					}
					
					this.entries.add(new Entry<>(attachment, c));
					added = true;
				}
			}
		});
		SERIALIZER_LISTS.put(id, this);
	}
	
	public int getIndexOf(Identifier id) {
		int index = 0;
		for(Entry<O, ?> entry : this.entries) {
			if(entry.serializer.networkId().equals(id)) {
				return index;
			}
			index++;
		}
		return -1;
	}
	
	public void readPacket(PacketByteBuf buf, Function<PacketByteBuf, O> readIdentifier) {
		Int2IntMap map = this.idToIndexMap;
		if(map == null) {
			LOGGER.warn("Attachment Registry Synchronization Failed! Unable to process " + AttachmentInit.ENTITY_SYNC);
			return;
		}
		
		O object = readIdentifier.apply(buf);
		if(object == null) {
			return;
		}
		
		int size = buf.readInt();
		for(int i = 0; i < size; i++) {
			int id = buf.readInt();
			if(id == -1) {
				int index = buf.readInt();
				Attachment<O, ?> attachment = this.entries.get(index).attachment();
				attachment.setValue(object, null);
				if(attachment instanceof DirtyableAttachment a) {
					a.consumeNetworkDirtiness(object);
				}
			} else {
				int index = map.get(id);
				this.sync(this.entries.get(index), object, buf);
			}
		}
	}
	
	@Nullable
	public PacketByteBuf writePacket(O object, BiConsumer<O, PacketByteBuf> identifier, boolean force) {
		List<DirtyEntry<O>> dirty = new ArrayList<>();
		int index = 0;
		for(PacketSerializerList.Entry<O, ?> entry : this.entries) {
			if(entry.attachment() instanceof DirtyableAttachment d) {
				if(d.consumeNetworkDirtiness(object) || force) {
					dirty.add(new DirtyEntry<>(entry, index));
				}
			} else {
				dirty.add(new DirtyEntry<>(entry, index));
			}
			index++;
		}
		
		if(dirty.isEmpty()) {
			return null;
		}
		
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		identifier.accept(object, buf);
		buf.writeInt(dirty.size());
		boolean write = false;
		for(DirtyEntry<O> entry : dirty) {
			write |= this.write(entry.list(), object, buf, entry.index());
		}
		if(!write) {
			return null;
		}
		return buf;
	}
	
	private <T> boolean write(PacketSerializerList.Entry<O, T> entry, O entity, PacketByteBuf buf, int index) {
		T value = entry.attachment().getValue(entity);
		if(value != null || entry.serializer().serializeNulls()) {
			buf.writeInt(index);
			entry.serializer().getTrackedHandler(entity).write(buf, value);
			return true;
		} else {
			buf.writeInt(-1);
			buf.writeInt(index);
			return false;
		}
	}
	
	private <T> void sync(PacketSerializerList.Entry<O, T> entry, O entity, PacketByteBuf buf) {
		ContextIdentifiedTrackedDataHandler<O, T> serializer = entry.serializer();
		T read = serializer.getTrackedHandler(entity).read(buf);
		Attachment<O, T> attachment = entry.attachment();
		attachment.setValue(entity, read);
		if(attachment instanceof DirtyableAttachment a) {
			a.consumeNetworkDirtiness(entity);
		}
	}
	
	public record DirtyEntry<T>(PacketSerializerList.Entry<T, ?> list, int index) {}
}
