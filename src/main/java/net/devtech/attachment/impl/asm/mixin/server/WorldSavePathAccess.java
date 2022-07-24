package net.devtech.attachment.impl.asm.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.util.WorldSavePath;

@Mixin(WorldSavePath.class)
public interface WorldSavePathAccess {
	@Invoker("<init>")
	static WorldSavePath createWorldSavePath(String relativePath) {
		throw new UnsupportedOperationException();
	}
}
