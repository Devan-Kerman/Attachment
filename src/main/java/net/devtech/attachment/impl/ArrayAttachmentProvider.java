package net.devtech.attachment.impl;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.AttachmentSetting;

public class ArrayAttachmentProvider<E, B extends AttachmentSetting> extends AbstractAttachmentProvider<E, B> {
	final Function<E, Object[]> arrayGetter;
	final BiConsumer<E, Object[]> arraySetter;
	
	
	public ArrayAttachmentProvider(
			Function<E, Object[]> arrayGetter, BiConsumer<E, Object[]> arraySetter) {
		this.arrayGetter = arrayGetter;
		this.arraySetter = arraySetter;
	}
	
	@Override
	public <T> Set<B> getBehavior(Attachment<E, T> attachment) {
		return this.attachments.get(((AttachmentImpl) attachment).index).behavior();
	}
	
	@Override
	protected <T> Attachment<E, T> createAttachment(Set<B> list, boolean atomic) {
		if(atomic) {
			throw new UnsupportedOperationException();
		}
		return new AttachmentImpl<>(this.attachments.size());
	}
	
	private Object[] getDataArray(E object, int index) {
		Object[] arr = ArrayAttachmentProvider.this.arrayGetter.apply(object);
		if(arr == null) {
			arr = new Object[index + 1];
			ArrayAttachmentProvider.this.arraySetter.accept(object, arr);
		} else if(index >= arr.length) {
			arr = Arrays.copyOf(arr, index + 1);
			ArrayAttachmentProvider.this.arraySetter.accept(object, arr);
		}
		return arr;
	}
	
	public class AttachmentImpl<T> implements Attachment<E, T>, DirtyableAttachment<E> {
		final Set<E> dirty = Collections.newSetFromMap(new WeakHashMap<>());
		final int index;
		
		public AttachmentImpl(int index) {
			this.index = index;
		}
		
		@Override
		public T getValue(E object) {
			Object[] apply = ArrayAttachmentProvider.this.arrayGetter.apply(object);
			if(apply == null) {
				return null;
			} else {
				return (T) apply[this.index];
			}
		}
		
		@Override
		public void setValue(E object, T value) {
			int index = this.index;
			Object[] arr = ArrayAttachmentProvider.this.getDataArray(object, index);
			arr[index] = value;
			if(ArrayAttachmentProvider.this.trackDirty) {
				this.dirty.add(object);
			}
		}
		
		@Override
		public AttachmentProvider<E, ?> getProvider() {
			return ArrayAttachmentProvider.this;
		}
		
		@Override
		public boolean consumeNetworkDirtiness(E entity) {
			return this.dirty.remove(entity);
		}
	}
}
