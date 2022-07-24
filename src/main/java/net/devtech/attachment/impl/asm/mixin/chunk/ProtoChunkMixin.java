package net.devtech.attachment.impl.asm.mixin.chunk;

import net.devtech.attachment.impl.asm.access.ProtoChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.nbt.NbtElement;
import net.minecraft.world.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements ProtoChunkAccess {
	@Unique NbtElement devtech_attachment_nbt;
	
	@Override
	public NbtElement devtech_attachment_getNbt() {
		return this.devtech_attachment_nbt;
	}
	
	@Override
	public void devtech_attachment_setNbt(NbtElement compound) {
		this.devtech_attachment_nbt = compound;
	}
}
