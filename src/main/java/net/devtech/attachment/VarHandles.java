package net.devtech.attachment;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import org.jetbrains.annotations.ApiStatus;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;

@ApiStatus.Internal
class VarHandles {
	public static final VarHandle ENTITY, WORLD, NBT;
	
	static {
		try {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			ENTITY = lookup.findVarHandle(Entity.class, "devtech_attach", Object[].class);
			WORLD = lookup.findVarHandle(World.class, "devtech_attach", Object[].class);
			NBT = lookup.findVarHandle(NbtCompound.class, "devtech_attach", Object[].class);
			//ENTITY_CLIENT = lookup.findVarHandle(Entity.class, "devtech_attach_client", Object[].class);
		} catch(NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
