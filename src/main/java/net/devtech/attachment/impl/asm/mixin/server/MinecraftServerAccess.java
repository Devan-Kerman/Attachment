package net.devtech.attachment.impl.asm.mixin.server;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccess {
	@Accessor
	LevelStorage.Session getSession();
}
