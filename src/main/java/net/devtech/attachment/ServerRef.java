package net.devtech.attachment;

import java.util.Objects;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.MinecraftServer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface ServerRef {
	Object getRef();
	
	
	static ServerRef of(MinecraftServer server) {
		return new Server(server);
	}
	
	@Environment(EnvType.CLIENT)
	static ServerRef clientConnectedTo() {
		return Client.CURRENTLY_CONNECTED;
	}
	
	@Environment(EnvType.CLIENT)
	enum Client implements ServerRef {
		CURRENTLY_CONNECTED;
		
		@Override
		public Object getRef() {
			MinecraftClient instance = MinecraftClient.getInstance();
			ClientPlayerEntity player = instance.player;
			Objects.requireNonNull(player, "Client is not in a world or on a server!");
			return player;
		}
	}
	
	record Server(MinecraftServer reference) implements ServerRef {
		public Server {
			Objects.requireNonNull(reference, "Server cannot be null!");
		}
		
		@Override
		public Object getRef() {
			return this.reference;
		}
	}
}
