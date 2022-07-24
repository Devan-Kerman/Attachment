package net.devtech.attachment.impl.asm.mixin.chunk;

import net.devtech.attachment.impl.init.AttachmentInit;
import net.devtech.attachment.impl.serializer.PacketSerializerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.world.chunk.WorldChunk;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin {
	@Shadow protected abstract void sendPacketToPlayersWatching(Packet<?> packet, boolean onlyOnWatchDistanceEdge);
	
	int devtech_attachment_sync = this.hashCode(); // mix it up a bit to prevent lag spikes
	
	@Inject(method = "flushUpdates", at = @At("HEAD"))
	public void syncChunk(WorldChunk chunk, CallbackInfo ci) {
		if(this.devtech_attachment_sync++ % 5 == 0) {
			PacketByteBuf buf = PacketSerializerList.CHUNK.writePacket(chunk, (c, write) -> {
				write.writeRegistryKey(chunk.getWorld().getRegistryKey());
				write.writeChunkPos(chunk.getPos());
			}, false);
			if(buf != null) {
				Packet<?> packet = ServerPlayNetworking.createS2CPacket(AttachmentInit.WORLD_SYNC, buf);
				this.sendPacketToPlayersWatching(packet, false);
			}
		}
	}
}
