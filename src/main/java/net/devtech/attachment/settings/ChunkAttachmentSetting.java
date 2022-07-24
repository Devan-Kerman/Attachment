package net.devtech.attachment.settings;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.impl.serializer.ContextIdentifiedCodec;
import net.devtech.attachment.impl.serializer.ContextIdentifiedTrackedDataHandler;

import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.WorldChunk;

public interface ChunkAttachmentSetting extends AttachmentSetting {
	/**
	 * A setting that makes this attachment serialized in a worldchunks's nbt data {@link ChunkSerializer#serialize(ServerWorld, Chunk)}
	 */
	static <T> Serializer<T> serializer(Identifier id, Codec<T> codec) {
		return new Serializer<>(id, Chunk -> codec);
	}
	
	/**
	 * A setting that makes this attachment synchronize to the clientside version of the Chunk {@link WorldChunk}
	 */
	static <T> NetSerializer<T> sync(Identifier id, TrackedDataHandler<T> codec) {
		return new NetSerializer<>(id, Chunk -> codec);
	}
	
	record Serializer<T>(Identifier id, Function<WorldChunk, Codec<T>> codecGetter) implements ChunkAttachmentSetting, ContextIdentifiedCodec<WorldChunk, T> {
		@Override
		public Identifier serializerId() {
			return this.id;
		}
		
		@Override
		public Codec<T> codec(WorldChunk entity) {
			return this.codecGetter.apply(entity);
		}
	}
	
	record NetSerializer<T>(Identifier id, Function<WorldChunk, TrackedDataHandler<T>> handlerGetter) implements ChunkAttachmentSetting, ContextIdentifiedTrackedDataHandler<WorldChunk, T> {
		@Override
		public Identifier networkId() {
			return this.id;
		}
		
		@Override
		public TrackedDataHandler<T> getTrackedHandler(WorldChunk context) {
			return this.handlerGetter.apply(context);
		}
	}
}
