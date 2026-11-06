package com.uninvited.director;

import com.uninvited.Uninvited;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicPlacement;
import com.uninvited.mimic.MimicSpawner;
import com.uninvited.stage.Stage;
import com.uninvited.stage.UninvitedState;
import com.uninvited.util.AloneUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Decides when and where the mimic shows up. Runs once a second per world and only ever looks
 * at players who are alone ({@link AloneUtil}).
 */
public final class MimicDirector {
	private static final Map<RegistryKey<World>, Long> LAST_APPEARANCE = new HashMap<>();

	private MimicDirector() {
	}

	/** Called once a second for each world from {@link Director}. */
	static void tick(ServerWorld world, Stage stage, double activity, List<ServerPlayerEntity> lonePlayers) {
		if (!stage.atLeast(Stage.WATCHING) || lonePlayers.isEmpty()) return;
		UninvitedConfig config = UninvitedConfig.get();
		if (MimicSpawner.activeMimics(world).size() >= config.maxMimics) return;

		Long last = LAST_APPEARANCE.get(world.getRegistryKey());
		if (last != null && world.getTime() - last < config.secondsBetweenAppearances * 20L) return;

		double chancePerSecond = config.spawnChancePerMinute / 60.0 * activity;
		List<ServerPlayerEntity> shuffled = new ArrayList<>(lonePlayers);
		Collections.shuffle(shuffled, new java.util.Random(world.getRandom().nextLong()));
		for (ServerPlayerEntity player : shuffled) {
			if (world.getRandom().nextDouble() < chancePerSecond) {
				appearFor(player, stage, null);
				return;
			}
		}
	}

	/** The mode an appearance takes at a given stage (with some variety). */
	public static MimicMode modeFor(Stage stage, Random random) {
		return switch (stage) {
			case DORMANT, SETTLING_IN -> MimicMode.IDLE;
			case WATCHING -> MimicMode.WATCH;
			case BECOMING -> random.nextFloat() < 0.75f ? MimicMode.FOLLOW : MimicMode.WATCH;
			case HUNTING -> random.nextFloat() < 0.7f ? MimicMode.HUNT : MimicMode.FOLLOW;
		};
	}

	/**
	 * Makes a mimic appear for {@code player}. Respects maxMimics, ignores the cooldown (used by
	 * /uninvited scare too).
	 *
	 * @param forcedMode null to pick one for the stage
	 * @return the mimic, or null if it couldn't find anywhere to stand
	 */
	@Nullable
	public static MimicEntity appearFor(ServerPlayerEntity player, Stage stage, @Nullable MimicMode forcedMode) {
		ServerWorld world = player.getServerWorld();
		if (MimicSpawner.activeMimics(world).size() >= UninvitedConfig.get().maxMimics) return null;
		Random random = world.getRandom();
		MimicMode mode = forcedMode != null ? forcedMode : modeFor(stage, random);
		if (mode == MimicMode.IDLE) return null;

		Vec3d spot;
		int lifetime;
		switch (mode) {
			case WATCH -> {
				// The edge of what the player can see. Entities are only sent to clients within
				// the server's view distance, so stay a chunk inside it.
				int viewBlocks = (world.getServer().getPlayerManager().getViewDistance() - 1) * 16;
				double far = MathHelper.clamp(viewBlocks - 8, 32, 72);
				float side = random.nextBoolean() ? 1 : -1;
				// Off to one side, in peripheral vision, where it's noticed late.
				spot = MimicPlacement.findSpotAround(player, far * 0.8, far, side * 55.0f, 20.0f, true, random);
				lifetime = 20 * 45;
			}
			case FOLLOW -> {
				spot = MimicPlacement.findSpotAround(player, 20, 30, 180.0f, 60.0f, false, random);
				lifetime = 20 * 120;
			}
			default -> {
				spot = MimicPlacement.findSpotAround(player, 26, 36, 180.0f, 60.0f, false, random);
				lifetime = 20 * 150;
			}
		}
		if (spot == null) return null;

		double dx = player.getX() - spot.x;
		double dz = player.getZ() - spot.z;
		float yaw = (float) (MathHelper.atan2(dz, dx) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
		MimicEntity mimic = MimicSpawner.spawn(world, spot, yaw, MimicSpawner.pickDisguiseFor(player, random), player, mode, lifetime);
		if (mimic != null) {
			LAST_APPEARANCE.put(world.getRegistryKey(), world.getTime());
			Uninvited.LOG.debug("Mimic appeared for {} as {} ({})", player.getGameProfile().getName(), mimic.getDisguiseName(), mode);
		}
		return mimic;
	}

	static void clear() {
		LAST_APPEARANCE.clear();
	}
}
