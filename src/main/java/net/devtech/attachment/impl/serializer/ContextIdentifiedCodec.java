package net.devtech.attachment.impl.serializer;

import com.mojang.serialization.Codec;

import net.minecraft.util.Identifier;

public interface ContextIdentifiedCodec<O, T> {
	Identifier serializerId();
	
	Codec<T> codec(O entity);
	
	/**
	 * @return whether to serialize null values with the codec
	 */
	default boolean serializesNulls() {
		return false;
	}
	
	default ContextIdentifiedCodec<O, T> withNulls() {
		if(this instanceof ContextIdentifiedCodec.Nullable) {
			return this;
		}
		return new Nullable<>(this);
	}
	
	final class Nullable<T, O> implements ContextIdentifiedCodec<O, T> {
		final ContextIdentifiedCodec<O, T> delegate;
		
		public Nullable(ContextIdentifiedCodec<O, T> delegate) {this.delegate = delegate;}
		
		@Override
		public Identifier serializerId() {
			return this.delegate.serializerId();
		}
		
		@Override
		public Codec<T> codec(O entity) {
			return this.delegate.codec(entity);
		}
		
		@Override
		public boolean serializesNulls() {
			return true;
		}
	}
}
