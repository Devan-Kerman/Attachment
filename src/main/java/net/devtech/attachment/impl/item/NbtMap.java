package net.devtech.attachment.impl.item;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ForwardingMap;
import com.mojang.datafixers.util.Pair;
import net.devtech.attachment.impl.DirtyableAttachment;
import net.devtech.attachment.impl.serializer.CodecSerializerList;
import org.jetbrains.annotations.NotNull;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;

public class NbtMap extends ForwardingMap<String, NbtElement> {
	public static final String KEY = "attachment:nbt";
	
	public static void initRead(NbtCompound read) {
		NbtCompound compound = read.getCompound(KEY);
		if(compound != null && !(compound instanceof Wrapper)) {
			CodecSerializerList.NBT.read(read, compound);
			NbtMap entries = new NbtMap(read);
			read.put(KEY, new Wrapper(entries));
		}
	}
	
	public static void initWrite(NbtCompound tag) {
		if(tag.contains(KEY)) {
			initRead(tag);
		} else {
			NbtMap map = new NbtMap(tag);
			tag.put(KEY, new Wrapper(map));
		}
	}
	
	final NbtCompound parent;
	final Map<String, NbtElement> serialized = new HashMap<>();
	
	public NbtMap(NbtCompound parent) {
		this.parent = parent;
	}
	
	boolean serializationLock = false;
	@Override
	protected @NotNull Map<String, NbtElement> delegate() {
		if(!this.serializationLock) {
			this.serializationLock = true;
			try {
				for(CodecSerializerList.Entry<NbtCompound, ?> entry : CodecSerializerList.NBT.entries) {
					boolean write = !(entry.attachment() instanceof DirtyableAttachment a) || a.consumeNetworkDirtiness(this.parent);
					if(write) {
						Pair<String, NbtElement> pair = CodecSerializerList.NBT.writeEntry(NbtOps.INSTANCE, this.parent, entry);
						if(pair.getSecond() != null) {
							this.serialized.put(pair.getFirst(), pair.getSecond());
						} else {
							this.serialized.remove(pair.getFirst());
						}
					}
				}
			} finally {
				this.serializationLock = false;
			}
		}
		
		return this.serialized;
	}
	
	private static final class Wrapper extends NbtCompound {
		public Wrapper(Map<String, NbtElement> entries) {
			super(entries);
		}
	}
}
