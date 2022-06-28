package net.devtech.attachment.impl;

public interface DirtyableAttachment<E> {
	boolean consumeNetworkDirtiness(E entity);
}
