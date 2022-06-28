package net.devtech.attachment.impl.serializer;

import net.devtech.attachment.impl.RequiresDirty;

import net.minecraft.entity.data.TrackedDataHandler;
import net.minecraft.util.Identifier;

public interface ContextIdentifiedTrackedDataHandler<O, T> extends RequiresDirty {
	Identifier networkId();
	
	TrackedDataHandler<T> getTrackedHandler(O context);
	
	default boolean serializeNulls() {
		return false;
	}
}
