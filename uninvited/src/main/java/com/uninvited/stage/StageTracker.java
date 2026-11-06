package com.uninvited.stage;

import com.mojang.authlib.properties.Property;
import com.uninvited.Uninvited;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Iterator;

/** Advances the night counter and remembers who has played in this world. */
public final class StageTracker {
	private StageTracker() {
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server ->
				UninvitedState.get(server).activateIfNeeded(server.getOverworld()));

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 != 0) return;
			UninvitedState state = UninvitedState.get(server);
			Stage before = state.stage();
			if (state.tickNights(server.getOverworld())) {
				Stage after = state.stage();
				Uninvited.LOG.info("Night {} has begun (stage {}: {})", state.nightsSeen(), after.number, after.title);
				if (after != before) Uninvited.LOG.info("UNINVITED escalated from {} to {}", before.title, after.title);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> remember(server, handler.getPlayer()));
	}

	public static Stage currentStage(MinecraftServer server) {
		return UninvitedState.get(server).stage();
	}

	/** Records the player's name and public skin property so it can be worn while they're offline. */
	private static void remember(MinecraftServer server, ServerPlayerEntity player) {
		String value = null;
		String signature = null;
		Iterator<Property> textures = player.getGameProfile().getProperties().get("textures").iterator();
		if (textures.hasNext()) {
			Property property = textures.next();
			value = property.value();
			signature = property.signature();
		}
		UninvitedState.get(server).rememberPlayer(new UninvitedState.KnownPlayer(
				player.getUuid(), player.getGameProfile().getName(), value, signature));
	}
}
