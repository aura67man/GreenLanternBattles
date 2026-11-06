package com.uninvited.director;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.scare.Scares;
import com.uninvited.taken.TakenTracker;
import com.uninvited.stage.Stage;
import com.uninvited.stage.UninvitedState;
import com.uninvited.util.AloneUtil;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * The once-a-second heartbeat that drives every scare. For each world it works out the stage,
 * how active things should be (nights are worse than days) and who is alone, then hands that to
 * the individual systems.
 */
public final class Director {
	private Director() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(world -> {
			if (world.getTime() % 20 != 0) return;
			tickWorld(world);
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BaseMeddling.forget(handler.getPlayer().getUuid()));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> MimicDirector.clear());
		VoiceScares.register();
	}

	private static void tickWorld(ServerWorld world) {
		UninvitedState state = UninvitedState.get(world.getServer());
		Stage stage = state.stage();
		if (stage == Stage.DORMANT) return;

		double activity = activity(world);
		if (activity <= 0) return;
		List<ServerPlayerEntity> lone = AloneUtil.lonePlayers(world);
		// Someone already taken has enough to deal with.
		lone.removeIf(player -> TakenTracker.isTaken(player.getUuid()));
		if (lone.isEmpty()) return;

		MimicDirector.tick(world, stage, activity, lone);
		VoiceScares.tick(world, stage, activity, lone);
		for (ServerPlayerEntity player : lone) {
			BaseMeddling.tick(player, stage, activity, state.nightsSeen());
			Scares.tick(player, stage, activity);
		}
	}

	/** 1.0 at night, {@code daytimeActivity} by day. Night is read from the overworld clock. */
	public static double activity(ServerWorld world) {
		return UninvitedState.isNight(world.getServer().getOverworld()) ? 1.0 : UninvitedConfig.get().daytimeActivity;
	}
}
