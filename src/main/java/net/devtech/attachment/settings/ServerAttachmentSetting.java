package net.devtech.attachment.settings;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.ServerRef;
import net.devtech.attachment.impl.serializer.ContextIdentifiedCodec;
import net.devtech.attachment.impl.serializer.ContextIdentifiedTrackedDataHandler;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

public interface ServerAttachmentSetting extends AttachmentSetting {
	/**
	 * A setting that makes this attachment serialized in a server's nbt data {@link MinecraftServer#getRunDirectory()}
	 */
	static <T> Serializer<T> serializer(Identifier id, Codec<T> codec) {
		return new Serializer<>(id, world -> codec);
	}
	
	/**
	 * A setting that makes this attachment synchronize to the clientside version of the world {@link ClientWorld}
	 */
	static <T> NetSerializer<T> sync(Identifier id, TrackedDataHandler<T> codec) {
		return new NetSerializer<>(id, world -> codec);
	}
	
	record Serializer<T>(Identifier id, Function<ServerRef, Codec<T>> codecGetter) implements ServerAttachmentSetting, ContextIdentifiedCodec<ServerRef, T> {
		@Override
		public Identifier serializerId() {
			return this.id;
		}
		
		@Override
		public Codec<T> codec(ServerRef entity) {
			return this.codecGetter.apply(entity);
		}
	}
	
	record NetSerializer<T>(Identifier id, Function<ServerRef, TrackedDataHandler<T>> handlerGetter) implements ServerAttachmentSetting, ContextIdentifiedTrackedDataHandler<ServerRef, T> {
		@Override
		public Identifier networkId() {
			return this.id;
		}
		
		@Override
		public TrackedDataHandler<T> getTrackedHandler(ServerRef context) {
			return this.handlerGetter.apply(context);
		}
	}
}
