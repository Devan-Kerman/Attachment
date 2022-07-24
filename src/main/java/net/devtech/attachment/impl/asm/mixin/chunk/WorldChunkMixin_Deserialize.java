package net.devtech.attachment.impl.asm.mixin.chunk;

import net.devtech.attachment.impl.asm.access.ProtoChunkAccess;
import net.devtech.attachment.impl.serializer.CodecSerializerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.NbtElement;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public class WorldChunkMixin_Deserialize {
	public Object[] devtech_attach;
	@Inject(method = "<init>(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/ProtoChunk;Lnet/minecraft/world/chunk/WorldChunk$EntityLoader;)V", at = @At("RETURN"))
	public void init(ServerWorld world, ProtoChunk protoChunk, WorldChunk.EntityLoader entityLoader, CallbackInfo ci) {
		ProtoChunkAccess chunk = (ProtoChunkAccess) protoChunk;
		NbtElement element = chunk.devtech_attachment_getNbt();
		if(element != null) {
			CodecSerializerList.CHUNK.read((WorldChunk) (Object) this, element);
		}
	}
}
