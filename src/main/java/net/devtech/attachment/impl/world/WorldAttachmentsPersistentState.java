package net.devtech.attachment.impl.world;

import net.devtech.attachment.impl.serializer.CodecSerializerList;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

/**
 * Serializes world attachments
 */
public class WorldAttachmentsPersistentState extends PersistentState {
	final World world;
	
	public WorldAttachmentsPersistentState(World world) {
		this.world = world;
	}
	
	public WorldAttachmentsPersistentState(World world, NbtCompound compound) {
		this.world = world;
		CodecSerializerList.WORLD.read(world, compound);
	}
	
	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtElement write = CodecSerializerList.WORLD.write(this.world);
		return (NbtCompound) write;
	}
}
