package net.devtech.attachment.impl;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import net.devtech.attachment.Attachment;
import net.devtech.attachment.AttachmentProvider;
import net.devtech.attachment.AttachmentSetting;

// maybe make custom provider for AttachableObject

/**
 * A concurrent version of {@link ArrayAttachmentProvider}
 */
public class ConcurrentAttachmentProvider<E, B extends AttachmentSetting> extends AbstractAttachmentProvider<E, B>
	implements AttachmentProvider.Atomic<E, B> {
	private static final VarHandle ARRAY_ELEMENT_VAR_HANDLE = MethodHandles.arrayElementVarHandle(Object[].class);
	final Function<E, Object[]> getVolatile;
	final AttachmentProvider.CompareAndSet<E> cas;

	public ConcurrentAttachmentProvider(VarHandle handle) {
		this(e -> (Object[]) handle.getVolatile(e), handle::weakCompareAndSet);
	}

	public ConcurrentAttachmentProvider(
		Function<E, Object[]> get, CompareAndSet<E> set) {
		this.getVolatile = get;
		this.cas = set;
	}

	@Override
	public <T> Set<B> getBehavior(Attachment<E, T> attachment) {
		return this.attachments.get(((CASAttachmentImpl) attachment).index).behavior();
	}

	@Override
	protected <T> Attachment<E, T> createAttachment(Set<B> behaviors, boolean isAtomic) {
		if(isAtomic) {
			return new AtomicAttachmentImpl<>(this.attachments.size());
		} else {
			return new CASAttachmentImpl<>(this.attachments.size());
		}
	}

	public class CASAttachmentImpl<T> implements Attachment<E, T>, DirtyableAttachment<E> {
		final int index;
		final Lock dirtyLock = new ReentrantLock();
		final Set<E> dirty = Collections.newSetFromMap(new WeakHashMap<>());
		
		public CASAttachmentImpl(int index) {
			this.index = index;
		}

		@Override
		public T getValue(E object) {
			Object[] apply = ConcurrentAttachmentProvider.this.getVolatile.apply(object);
			if(apply == null || this.index >= apply.length) {
				return null;
			} else {
				//noinspection unchecked
				return (T) apply[this.index];
			}
		}
		
		@Override
		public boolean consumeNetworkDirtiness(E entity) {
			Lock lock = this.dirtyLock;
			lock.lock();
			try {
				return this.dirty.remove(entity);
			} finally {
				lock.unlock();
			}
		}
		
		void markDirty(E object) {
			if(ConcurrentAttachmentProvider.this.trackDirty) {
				Lock lock = this.dirtyLock;
				lock.lock();
				try {
					this.dirty.add(object);
				} finally {
					lock.unlock();
				}
			}
		}

		@Override
		public void setValue(E object, T value) {
			int index = this.index;
			Object[] arr, new_;
			do {
				new_ = arr = ConcurrentAttachmentProvider.this.getVolatile.apply(object);
				if(arr == null) {
					new_ = new Object[index + 1];
				} else if(index >= arr.length) {
					new_ = copyOfVolatile(arr, index + 1);
				}
				new_[index] = value;
			} while(!ConcurrentAttachmentProvider.this.cas.compareAndSet(object, arr, new_));
			this.markDirty(object);
		}

		@Override
		public AttachmentProvider<E, ?> getProvider() {
			return ConcurrentAttachmentProvider.this;
		}
		
	}

	static Object[] copyOfVolatile(Object[] arr, int newLen) {
		Object[] new_ = new Object[newLen];
		for(int i = 0; i < arr.length; i++) {
			new_[i] = ARRAY_ELEMENT_VAR_HANDLE.getVolatile(arr, newLen);
		}
		return new_;
	}

	public class AtomicAttachmentImpl<T> extends CASAttachmentImpl<T> implements Attachment.Atomic<E, T> {
		public AtomicAttachmentImpl(int index) {
			super(index);
		}

		@Override
		public T getValue(E object) {
			Object[] apply = ConcurrentAttachmentProvider.this.getVolatile.apply(object);
			if(apply == null || this.index >= apply.length) {
				return null;
			} else {
				//noinspection unchecked
				return (T) ARRAY_ELEMENT_VAR_HANDLE.getVolatile(apply, this.index);
			}
		}

		@Override
		public void setValue(E object, T value) {
			int index = this.index;
			Object[] arr, new_;
			do {
				new_ = arr = ConcurrentAttachmentProvider.this.getVolatile.apply(object);
				if(arr == null) {
					new_ = new Object[index + 1];
				} else if(index >= arr.length) {
					new_ = copyOfVolatile(arr, index + 1);
				}

				ARRAY_ELEMENT_VAR_HANDLE.setVolatile(new_, index, value);
			} while(!ConcurrentAttachmentProvider.this.cas.compareAndSet(object, arr, new_));
			this.markDirty(object);
		}

		@Override
		public boolean weakCompareAndSet(E object, T expected, T value) {
			int index = this.index;
			Object[] arr, new_;
			new_ = arr = ConcurrentAttachmentProvider.this.getVolatile.apply(object);
			if(arr == null) {
				new_ = new Object[index + 1];
			} else if(index >= arr.length) {
				new_ = Arrays.copyOf(arr, index + 1);
			}

			if(ARRAY_ELEMENT_VAR_HANDLE.compareAndSet(new_, index, expected, value)) {
				boolean didSet = ConcurrentAttachmentProvider.this.cas.compareAndSet(object, arr, new_);
				if(didSet) {
					this.markDirty(object);
				}
				return didSet;
			} else {
				return false;
			}
		}
	}
}
