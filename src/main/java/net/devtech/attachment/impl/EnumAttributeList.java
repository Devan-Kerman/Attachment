package net.devtech.attachment.impl;

import java.util.List;
import java.util.Vector;

import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.AttachmentSetting;
import net.devtech.attachment.Attachments;
import net.devtech.attachment.settings.EntityAttachmentSetting;

import net.minecraft.entity.Entity;

/**
 * Maintains a list of Attachments that have the given attachment setting
 */
public class EnumAttributeList<O> {
	public static final EnumAttributeList<Entity> PLAYER_DEATH_ENTITY = new EnumAttributeList<>(Attachments.ENTITY, EntityAttachmentSetting.DeathBehavior.PRESERVE_ON_PLAYER_DEATH);
	public static final EnumAttributeList<Entity> PLAYER_DEATH_ENTITY_KEEP_INVENTORY = new EnumAttributeList<>(Attachments.ENTITY, EntityAttachmentSetting.DeathBehavior.PRESERVE_ON_PLAYER_DEATH_WHEN_KEEP_INVENTORY);
	
	public final List<Attachment<O, ?>> entries = new Vector<>();
	
	public <B extends AttachmentSetting, C extends B> EnumAttributeList(AttachmentProvider<O, B> provider, C instance) {
		provider.registerAndRunListener((attachment, behavior) -> {
			if(behavior.contains(instance)) {
				this.entries.add(attachment);
			}
		});
	}
}
