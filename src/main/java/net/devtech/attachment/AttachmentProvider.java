package net.devtech.attachment;

import java.lang.invoke.VarHandle;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;

import net.devtech.attachment.impl.ArrayAttachmentProvider;
import net.devtech.attachment.impl.ConcurrentAttachmentProvider;
import net.devtech.attachment.settings.EntityAttachmentSetting;
import net.devtech.attachment.settings.NbtAttachmentSetting;
import net.devtech.attachment.settings.WorldAttachmentSetting;

import net.minecraft.nbt.NbtCompound;

/**
 * A provider interface for attaching custom data to game objects
 * @param <O> The type to access the data from
 * @param <B> custom settings for the given provider
 */
public interface AttachmentProvider<O, B extends AttachmentSetting> {
	
	/**
	 * Creates a simple provider that can't be used on multiple threads.
	 * @param arrayGetter a function to get the datastructures which stores the custom information
	 * @param <E> the type that you are attaching data to
	 * @param <B> the custom attachment settings class {@link EntityAttachmentSetting} {@link WorldAttachmentSetting}
	 * @return a provider for that object
	 */
	static <E, B extends AttachmentSetting> AttachmentProvider<E, B> simple(
		Function<E, Object[]> arrayGetter,
		BiConsumer<E, Object[]> arraySetter) {
		return new ArrayAttachmentProvider<>(arrayGetter, arraySetter);
	}

	static <E extends AttachableObject, B extends AttachmentSetting> AttachmentProvider<E, B> simple() {
		return new ArrayAttachmentProvider<>(a -> a.attachedData, (e, a) -> e.attachedData = a);
	}

	/**
	 * Creates an attachment provider that can be accessed from multiple threads.
	 * <br>
	 * <b>The array field must be marked `volatile` (or you must use VarHandle's volatile methods)!</b>
	 */
	static <E, B extends AttachmentSetting> Atomic<E, B> atomic(
		Function<E, Object[]> getVolatile,
		CompareAndSet<E> compareAndSet) {
		return new ConcurrentAttachmentProvider<>(getVolatile, compareAndSet);
	}

	static <E extends AttachableObject, B extends AttachmentSetting> Atomic<E, B> atomic() {
		return new ConcurrentAttachmentProvider<>(a -> (Object[]) AttachableObject.HANDLE.getVolatile(a), AttachableObject.HANDLE::weakCompareAndSet);
	}

	/**
	 * This method is similar to {@link #atomic(Function, CompareAndSet)} however it <b>may</b> be slower as the jvm might have a harder time inlining it
	 * @param handle the varhandle of an Object[] field in E
	 */
	static <E, B extends AttachmentSetting> Atomic<E, B> atomic(VarHandle handle) {
		return new ConcurrentAttachmentProvider<>(handle);
	}
	
	static <E, B extends AttachmentSetting> AttachmentProvider<E, B> simple(VarHandle handle) {
		return new ArrayAttachmentProvider<>(e -> (Object[]) handle.get(e), handle::set);
	}

	/**
	 * Register a new attachment
	 * @param behavior any custom behavior supported by the current attachment provider
	 * @return an accessor for your custom data within the type {@link O}
	 */
	<T> Attachment<O, T> registerAttachment(B... behavior);
	
	/**
	 * @return an unmodifiable list of all currently registered attachments
	 */
	List<AttachmentPair<O, B>> getAttachments();
	
	/**
	 * @return The custom behaviors for the given attachment
	 */
	<T> Set<B> getBehavior(Attachment<O, T> attachment);
	
	/**
	 * Register a listener which is fired every time a new attachment is registered.
	 */
	void registerListener(AttachmentRegistrationListener<O, B> listener);
	
	/**
	 * Register a listener and execute it for all existing attachments
	 */
	default void registerAndRunListener(AttachmentRegistrationListener<O, B> listener) {
		this.registerListener(listener);
		for(AttachmentPair<O, B> attachment : this.getAttachments()) {
			listener.accept(attachment.attachment, attachment.behavior);
		}
	}
	
	interface AttachmentRegistrationListener<E, B extends AttachmentSetting> {
		/**
		 * @param attachment the attachment being registered
		 * @param behavior the passed set does preserve the order of the original array
		 */
		void accept(Attachment<E, ?> attachment, Set<B> behavior);
	}
	
	interface Atomic<O, B extends AttachmentSetting> extends AttachmentProvider<O, B> {
		/**
		 * Creates an atomic attachment, which contains concurrent operations like {@link AtomicReference}
		 * @see #registerAttachment(AttachmentSetting[])
		 */
		<T> Attachment.Atomic<O, T> registerAtomicAttachment(B... behavior);
	}
	
	interface CompareAndSet<E> {
		/**
		 * @see VarHandle#weakCompareAndSet(Object...)
		 */
		boolean compareAndSet(E obj, Object[] expected, Object[] set);
	}

	/**
	 * @param behavior the passed set does preserve the order of the original array
	 */
	record AttachmentPair<E, B extends AttachmentSetting>(Attachment<E, ?> attachment, Set<B> behavior) {}
	
}
