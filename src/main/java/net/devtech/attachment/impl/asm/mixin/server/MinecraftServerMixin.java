package net.devtech.attachment.impl.asm.mixin.server;

import net.devtech.attachment.impl.event.ServerSaveCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistryManager;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
	@Shadow @Final protected LevelStorage.Session session;
	@Shadow public abstract DynamicRegistryManager.Immutable getRegistryManager();
	@Shadow @Final protected SaveProperties saveProperties;
	
	public Object[] devtech_attach;
	
	@Inject(method = "save", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getOverworld()Lnet/minecraft/server/world/ServerWorld;"))
	public void init(boolean suppressLogs, boolean flush, boolean force, CallbackInfoReturnable<Boolean> cir) {
		ServerSaveCallback.EVENT.invoker().onSave(
				(MinecraftServer) (Object) this,
				this.session,
				suppressLogs,
				flush,
				force,
				this.getRegistryManager(),
				this.saveProperties
		);
	}
}
