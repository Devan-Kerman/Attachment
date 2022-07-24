package net.devtech.attachment.impl.asm.mixin.chunk;

import net.devtech.attachment.Attachments;
import net.devtech.attachment.impl.asm.access.ProtoChunkAccess;
import net.devtech.attachment.impl.serializer.CodecSerializerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.ChunkSerializer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.poi.PointOfInterestStorage;

@Mixin(ChunkSerializer.class)
public class ChunkSerializerMixin {
	@Inject(method = "deserialize", at = @At("RETURN"))
	private static void deserialize(
			ServerWorld world, PointOfInterestStorage poiStorage, ChunkPos chunkPos, NbtCompound nbt, CallbackInfoReturnable<ProtoChunk> cir) {
		NbtElement attachment = nbt.get("devtech_attachment");
		if(attachment != null) {
			ProtoChunk value = cir.getReturnValue();
			if(value instanceof ReadOnlyChunkAccess access) {
				WorldChunk wrapped = access.getWrapped();
				CodecSerializerList.CHUNK.read(wrapped, attachment);
			} else {
				((ProtoChunkAccess) value).devtech_attachment_setNbt(attachment);
			}
		}
	}
	
	@Inject(method = "serialize", at = @At("RETURN"))
	private static void serialize(ServerWorld world, Chunk chunk, CallbackInfoReturnable<NbtCompound> cir) {
		WorldChunk from = Attachments.from(chunk);
		if(from != null) {
			NbtElement write = CodecSerializerList.CHUNK.write(chunk);
			if(write != null) {
				cir.getReturnValue().put("devtech_attachment", write);
			}
		}
	}
}
