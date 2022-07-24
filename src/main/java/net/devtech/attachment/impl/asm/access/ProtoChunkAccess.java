package net.devtech.attachment.impl.asm.access;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

public interface ProtoChunkAccess {
	NbtElement devtech_attachment_getNbt();
	void devtech_attachment_setNbt(NbtElement compound);
}
