package net.devtech.attachment.settings;

import java.util.function.Consumer;
import java.util.function.Function;

import com.mojang.serialization.Codec;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.impl.serializer.ContextIdentifiedCodec;
import net.devtech.attachment.impl.serializer.ContextIdentifiedTrackedDataHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.util.Identifier;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;

/**
 * Supported settings for entity attachments
 */
public interface EntityAttachmentSetting extends AttachmentSetting {
	/**
	 * A setting that makes this attachment serialized in an entity's nbt data {@link Entity#writeNbt(NbtCompound)}
	 */
	static <T> Serializer<T> serializer(Identifier id, Codec<T> codec) {
		return new Serializer<>(id, entity -> codec);
	}
	
	/**
	 * A setting that makes this attachment synchronize to the clientside version of the entity {@link EntityTrackerEntry#sendPackets(Consumer)}
	 */
	static <T> NetSerializer<T> sync(Identifier id, TrackedDataHandler<T> codec) {
		return new NetSerializer<>(id, entity -> codec);
	}
	
	/**
	 * A setting that causes the attachment's data to be copied over to the new version of the player when they die.
	 * @see ServerPlayerEvents#COPY_FROM
	 */
	static DeathBehavior preserveRespawn() {
		return DeathBehavior.PRESERVE_ON_PLAYER_DEATH;
	}
	
	/**
	 * A setting that causes the attachment's data to be copied over to the new version of the player when they die only if <pre>/gamerule keepInventory</pre> is true.
	 * @see ServerPlayerEvents#COPY_FROM
	 */
	static DeathBehavior preserveRespawnKeepInventory() {
		return DeathBehavior.PRESERVE_ON_PLAYER_DEATH_WHEN_KEEP_INVENTORY;
	}
	
	record Serializer<T>(Identifier id, Function<Entity, Codec<T>> codecGetter)
			implements EntityAttachmentSetting, ContextIdentifiedCodec<Entity, T> {
		@Override
		public Identifier serializerId() {
			return this.id;
		}
		
		@Override
		public Codec<T> codec(Entity entity) {
			return this.codecGetter.apply(entity);
		}
	}
	
	record NetSerializer<T>(Identifier id, Function<Entity, TrackedDataHandler<T>> handlerGetter)
			implements EntityAttachmentSetting, ContextIdentifiedTrackedDataHandler<Entity, T> {
		@Override
		public Identifier networkId() {
			return this.id;
		}
		
		@Override
		public TrackedDataHandler<T> getTrackedHandler(Entity context) {
			return this.handlerGetter.apply(context);
		}
	}
	
	enum DeathBehavior implements EntityAttachmentSetting {
		DEFAULT,
		PRESERVE_ON_PLAYER_DEATH,
		PRESERVE_ON_PLAYER_DEATH_WHEN_KEEP_INVENTORY
	}
}
