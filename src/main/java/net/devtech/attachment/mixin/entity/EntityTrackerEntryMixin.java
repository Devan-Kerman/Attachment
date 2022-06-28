package net.devtech.attachment.mixin.entity;

import java.util.function.Consumer;

import net.devtech.attachment.impl.init.AttachmentInit;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.EntityTrackerEntry;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@Mixin(EntityTrackerEntry.class)
public abstract class EntityTrackerEntryMixin {
	@Shadow
	protected abstract void sendSyncPacket(Packet<?> packet);
	
	@Shadow @Final private Entity entity;
	
	@Inject(method = "sendPackets", at = @At("HEAD"))
	public void send(Consumer<Packet<?>> sender, CallbackInfo ci) {
		Packet<?> packet = this.createSyncPacket(true);
		if(packet != null) {
			sender.accept(packet);
		}
	}
	
	@Inject(method = "syncEntityData", at = @At("HEAD"))
	public void sync(CallbackInfo ci) {
		Packet<?> packet = this.createSyncPacket(false);
		if(packet != null) {
			this.sendSyncPacket(packet);
		}
	}
	
	@Unique
	@Nullable
	private Packet<?> createSyncPacket(boolean force) {
		PacketByteBuf buf = PacketSerializerList.ENTITY_LIST.writePacket(
				this.entity,
				(entity1, idBuf) -> idBuf.writeInt(entity1.getId()),
				force
		);
		if(buf == null) {
			return null;
		}
		return ServerPlayNetworking.createS2CPacket(
				AttachmentInit.ENTITY_SYNC, buf
		);
	}
}
