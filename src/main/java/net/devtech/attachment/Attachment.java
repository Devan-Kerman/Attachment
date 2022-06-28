package net.devtech.attachment;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * An accessor interface for custom data within a specified type
 * @param <O> the type who's instances this data should be attached to
 */
public interface Attachment<O, T> {
	T getValue(O object);

	void setValue(O object, T value);

	default T getOrDefault(O object, T default_) {
		T value = this.getValue(object);
		return value == null ? default_ : value;
	}

	default T getOrApply(O object, Function<O, T> func) {
		T value = this.getValue(object);
		return value == null ? func.apply(object) : value;
	}

	default <M> M mapOrNull(O object, Function<T, M> map) {
		T value = this.getValue(object);
		return value == null ? null : map.apply(value);
	}

	default Optional<T> getOpt(O object) {
		return Optional.of(this.getValue(object));
	}

	default T getAndUpdate(O object, T default_, UnaryOperator<T> updater) {
		T value = this.getOrDefault(object, default_);
		this.setValue(object, updater.apply(value));
		return value;
	}
	
	/**
	 * @return The provider that was used to register this attachment
	 */
	AttachmentProvider<O, ?> getProvider();
	
	interface Atomic<O, T> extends Attachment<O, T> {
		default T weakUpdateAndGet(O object, UnaryOperator<T> operator) {
			T val, new_;
			do {
				val = this.getValue(object);
				new_ = operator.apply(val);
			} while(!this.weakCompareAndSet(object, val, new_));
			return new_;
		}

		default T weakGetAndUpdate(O object, UnaryOperator<T> operator) {
			T val;
			do {
				val = this.getValue(object);
			} while(!this.weakCompareAndSet(object, val, operator.apply(val)));
			return val;
		}

		default T weakGetOrDefaultAndUpdate(O object, T default_, UnaryOperator<T> operator) {
			T val, value;
			do {
				val = this.getValue(object);
				if(val != null) {
					value = val;
				} else {
					value = default_;
				}
			} while(!this.weakCompareAndSet(object, val, operator.apply(value)));
			return value;
		}

		default T strongUpdateAndGet(O object, UnaryOperator<T> operator) {
			T val, new_;
			do {
				val = this.getValue(object);
				new_ = operator.apply(val);
			} while(!this.strongCompareAndSet(object, val, new_));
			return new_;
		}

		default T strongGetAndUpdate(O object, UnaryOperator<T> operator) {
			T val;
			do {
				val = this.getValue(object);
			} while(!this.strongCompareAndSet(object, val, operator.apply(val)));
			return val;
		}

		/**
		 * {@link #weakGetOrDefaultAndUpdate(Object, Object, UnaryOperator)} but using {@link #strongCompareAndSet(Object,
		 * Object, Object)} this makes it particularly useful for incrementing integers
		 */
		default T strongGetOrDefaultAndUpdate(O object, T default_, UnaryOperator<T> operator) {
			T val, value;
			do {
				val = this.getValue(object);
				if(val != null) {
					value = val;
				} else {
					value = default_;
				}
			} while(!this.strongCompareAndSet(object, val, operator.apply(value)));
			return value;
		}

		/**
		 * Tries to set the value of {@link #getValue(Object)} == {@code `expected`} atomically
		 *
		 * @return true if the value was successfully exchanged
		 */
		boolean weakCompareAndSet(O object, T expected, T set);

		/**
		 * Tries to set the value of {@link #getValue(Object)} {@link Objects#equals(Object, Object)} {@code `expected`} atomically
		 *
		 * @return true if the value was successfully exchanged
		 */
		default boolean strongCompareAndSet(O object, T expected, T set) {
			T value = this.getValue(object);
			if(Objects.equals(value, expected)) {
				return this.weakCompareAndSet(object, expected, set);
			} else {
				return false;
			}
		}
	}
}
