# Attachment

Attach custom data to Entities, Worlds, and ItemStacks (NbtCompounds)

## Gradle
```groovy
repositories {
    maven {
        url = uri("https://storage.googleapis.com/devan-maven/")
    }
}

dependencies {
  // there is no api (well except for render handler), just set maxCount in Item$Settings to values over 64
  modRuntime(include("net.devtech:Attachment:1.2.0"))
}
```

## Entity
```java
public class MyAttachments {
	public static final Identifier ID = new Identifier("mymod:entity_attachment");
	// all settings are optional
	public static final Attachment<Entity, Integer> ENTITY_ATTACHMENT = Attachments.ENTITY.registerAttachment(
			EntityAttachmentSetting.serializer(ID, Codec.INT), // saves the data when the entity is written to the disk
            EntityAttachmentSetting.preserveRespawn(), // copies over the data from the old to new player when they respawn
            EntityAttachmentSetting.sync(ID, TrackedDataHandlerRegistry.INTEGER) // syncs the data to the client
    );
}
```

## World
World attachments are bound to `BlockRenderView` to allow them to be used in BakedModel rendering

```java
public class MyAttachments {
	public static final Identifier ID = new Identifier("mymod:entity_attachment");
	// all settings are optional
	public static final Attachment<BlockRenderView, Integer> WORLD_ATTACHMENT = Attachments.WORLD.registerAttachment(
			EntityAttachmentSetting.serializer(ID, Codec.INT), // saves the data when the world is written to the disk
            EntityAttachmentSetting.sync(ID, TrackedDataHandlerRegistry.INTEGER) // syncs the data to the client
    );
}
```

## Item / NbtCompound
World attachments are bound to `BlockRenderView` to allow them to be used in BakedModel rendering

```java
public class MyAttachments {
	public static final Identifier ID = new Identifier("mymod:entity_attachment");
	// all settings are optional, but recommended
	public static final Attachment<NbtCompound, Integer> NBT_ATTACHMENT = Attachments.NBT.registerAttachment(
			EntityAttachmentSetting.serializer(ID, Codec.INT) // saves the data when the nbt tag is written to the disk
    );
	
	public static void set(ItemStack stack, int value) {
		NBT_ATTACHMENT.setValue(stack.getOrCreateNbt(), value);
    }
}
```