package net.devtech.attachment.mixin.item;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.nbt.NbtCompound;

@Mixin(NbtCompound.class)
public class NbtCompoundMixin {
	public Object[] devtech_attach;
}
