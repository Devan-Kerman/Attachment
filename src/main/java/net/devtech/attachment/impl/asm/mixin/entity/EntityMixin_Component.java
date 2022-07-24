package net.devtech.attachment.impl.asm.mixin.entity;

import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.impl.serializer.CodecSerializerList;
import net.devtech.attachment.settings.EntityAttachmentSetting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

@Mixin(Entity.class)
public class EntityMixin_Component {
	public Object[] devtech_attach;
	
	/**
	 * When entities cross dimensions they are actually copied for some godforsaken reason, so we must copy over the attachments
	 */
	@Inject(method = "copyFrom", at = @At("HEAD"))
	public void copy(Entity original, CallbackInfo ci) {
		for(AttachmentProvider.AttachmentPair<Entity, EntityAttachmentSetting> entry : Attachments.ENTITY.getAttachments()) {
			this.devtech_copyAttachment(original, entry.attachment());
		}
	}
	
	@Unique // generics moment
	private <T> void devtech_copyAttachment(Entity original, Attachment<Entity, T> attachment) {
		attachment.setValue((Entity) (Object) this, attachment.getValue(original));
	}
	
	@Inject(method = "readNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;)V"))
	public void readNbt(NbtCompound nbt, CallbackInfo ci) {
		NbtCompound compound = nbt.getCompound("devtech:attach");
		if(compound != null) {
			CodecSerializerList.ENTITY.read((Entity) (Object) this, compound);
		}
	}
	
	@Inject(method = "writeNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;)V"))
	public void writeNbt(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
		NbtElement write = CodecSerializerList.ENTITY.write((Entity) (Object) this);
		if(write != null) {
			nbt.put("devtech:attach", write);
		}
	}
}
