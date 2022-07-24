package net.devtech.attachment.impl.asm.mixin.server;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.client.network.ClientPlayerEntity;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
	public Object[] devtech_attach;
}
