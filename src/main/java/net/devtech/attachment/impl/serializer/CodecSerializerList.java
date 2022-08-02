package net.devtech.attachment.impl.serializer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.RecordBuilder;
import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.ServerRef;
import org.apache.commons.lang3.SerializationException;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.Identifier;
import net.minecraft.world.BlockRenderView;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Maintains a list of attachments that have a {@link ContextIdentifiedCodec} for serialization purposes.
 */
public final class CodecSerializerList<O> {
	public static final CodecSerializerList<Entity> ENTITY = new CodecSerializerList<>(Attachments.ENTITY);
	public static final CodecSerializerList<BlockRenderView> WORLD = new CodecSerializerList<>(Attachments.WORLD);
	public static final CodecSerializerList<Chunk> CHUNK = new CodecSerializerList<>(Attachments.CHUNK);
	public static final CodecSerializerList<NbtCompound> NBT = new CodecSerializerList<>(Attachments.NBT);
	public static final CodecSerializerList<ServerRef> SERVER = new CodecSerializerList<>(Attachments.SERVER);
	
	public final List<Entry<O, ?>> entries = new Vector<>();
	
	public record Entry<O, T>(Attachment<O, T> attachment, ContextIdentifiedCodec<O, T> serializer) {}
	
	public CodecSerializerList(AttachmentProvider<O, ?> provider) {
		Set<Identifier> identifiers = Collections.newSetFromMap(new ConcurrentHashMap<>());
		provider.registerAndRunListener((attachment, behavior) -> {
			boolean added = false;
			for(AttachmentSetting setting : behavior) {
				if(setting instanceof ContextIdentifiedCodec c) {
					if(added) {
						throw new IllegalArgumentException("Cannot have multiple serializers for single attachment!");
					}
					if(!identifiers.add(c.serializerId())) {
						throw new IllegalArgumentException("Duplicate serializers with id " + c.serializerId());
					}
					//noinspection unchecked,rawtypes
					this.entries.add(new Entry<>(attachment, c));
					added = true;
				}
			}
		});
	}
	
	public NbtElement write(O context) {
		return this.write(NbtOps.INSTANCE, context);
	}
	
	public void read(O context, NbtElement element) {
		this.read(NbtOps.INSTANCE, context, element);
	}
	
	/**
	 * @return if everything was null and there was nothing to serialize
	 */
	public <T> T write(DynamicOps<T> ops, O context) {
		RecordBuilder<T> builder = ops.mapBuilder();
		boolean write = false;
		for(Entry<O, ?> entry : this.entries) {
			write |= this.write(builder, context, entry);
		}
		if(write) {
			DataResult<T> build = builder.build(ops.empty());
			return build.getOrThrow(false, s -> {throw new SerializationException(s);});
		} else {
			return null;
		}
	}
	
	public <T> void read(DynamicOps<T> ops, O context, T map) {
		if(map == null) {
			return;
		}
		
		for(Entry<O, ?> entry : this.entries) {
			this.read(ops, map, context, entry);
		}
	}
	
	private <T, E> void read(DynamicOps<E> ops, E map, O context, Entry<O, T> entry) {
		ContextIdentifiedCodec<O, T> serializer = entry.serializer;
		String key = serializer.serializerId().toString();
		ops.get(map, key).get().ifLeft(serialized -> {
			Codec<T> codec = serializer.codec(context);
			DataResult<Pair<T, E>> decode = codec.decode(ops, serialized);
			decode.get().ifLeft(pair -> {
				T first = pair.getFirst();
				entry.attachment.setValue(context, first);
			});
		});
	}
	
	public <T, E> Pair<String, E> writeEntry(DynamicOps<E> ops, O context, Entry<O, T> entry) {
		ContextIdentifiedCodec<O, T> serializer = entry.serializer();
		Codec<T> codec = serializer.codec(context);
		T value = entry.attachment.getValue(context);
		String identifier = serializer.serializerId().toString();
		if(value != null || serializer.serializesNulls()) {
			// throw hard on write
			DataResult<E> result = codec.encodeStart(ops, value);
			var either = result.get();
			//either.ifRight(partial -> {
			//	throw new SerializationException("Unable to serialize " + context + " because " + partial.message());
			//});
			return Pair.of(identifier, either.left().orElse(null));
		} else {
			return Pair.of(identifier, null);
		}
	}
	
	public <T, E> boolean write(RecordBuilder<E> map, O context, Entry<O, T> entry) {
		Pair<String, E> pair = this.writeEntry(map.ops(), context, entry);
		if(pair.getSecond() != null) {
			map.add(pair.getFirst(), pair.getSecond());
			return true;
		} else {
			return false;
		}
	}
}
