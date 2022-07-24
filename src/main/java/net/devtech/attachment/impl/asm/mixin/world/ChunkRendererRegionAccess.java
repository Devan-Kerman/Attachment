package net.devtech.attachment.impl.asm.mixin.world;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.render.chunk.ChunkRendererRegion;
import net.minecraft.world.World;

/**
 * ChunkRenderRegion has to be unwrapped because if we store the data on the ChunkRenderRegion object we won't be able to access information in the ClientWorld from it
 */
@Mixin(ChunkRendererRegion.class)
public interface ChunkRendererRegionAccess {
	@Accessor
	World getWorld();
}
