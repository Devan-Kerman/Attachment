package net.devtech.attachment.settings;

import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.impl.RequiresDirty;
import net.devtech.attachment.impl.serializer.ContextIdentifiedCodec;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

public interface NbtAttachmentSetting extends AttachmentSetting {
	static <T> Serializer<T> serializer(Identifier id, Codec<T> codec) {
		return new Serializer<>(id, NbtCompound -> codec);
	}
	
	record Serializer<T>(Identifier id, Function<NbtCompound, Codec<T>> codecGetter) implements NbtAttachmentSetting, ContextIdentifiedCodec<NbtCompound, T>, RequiresDirty {
		@Override
		public Identifier serializerId() {
			return this.id;
		}
		
		@Override
		public Codec<T> codec(NbtCompound entity) {
			return this.codecGetter.apply(entity);
		}
	}
}
