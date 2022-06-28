package net.devtech.attachment.mixin.world;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.World;

@Mixin(World.class)
public class WorldMixin {
	public Object[] devtech_attach;
}
