package net.devtech.attachment;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public abstract class AttachableObject {
	static final VarHandle HANDLE;
	
	static {
		try {
			HANDLE = MethodHandles.lookup().findVarHandle(AttachableObject.class, "attachedData", Object[].class);
		} catch(NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	Object[] attachedData;
}
