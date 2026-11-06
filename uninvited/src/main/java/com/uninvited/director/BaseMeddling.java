package com.uninvited.director;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicPlacement;
import com.uninvited.stage.Stage;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Stage 2+: "something was in here". While a lone player is away from home, a single
 * out-of-place block appears on the floor inside their base. At most once per player per night.
 *
 * <p>"Base" = the player's respawn point (bed or anchor) in the current dimension. "Inside" =
 * no sky above it, so it lands indoors rather than in the garden.
 */
public final class BaseMeddling {
	private static final Block[] ODD_BLOCKS = {Blocks.DIRT, Blocks.DIRT, Blocks.DIRT, Blocks.GRAVEL, Blocks.COARSE_DIRT, Blocks.MOSS_BLOCK};
	private static final Map<UUID, Integer> LAST_NIGHT = new HashMap<>();

	private BaseMeddling() {
	}

	static void tick(ServerPlayerEntity player, Stage stage, double activity, int night) {
		if (!stage.atLeast(Stage.WATCHING)) return;
		if (LAST_NIGHT.getOrDefault(player.getUuid(), -1) == night) return;
		// A few times less likely than a mimic sighting.
		double chancePerSecond = UninvitedConfig.get().spawnChancePerMinute * 0.3 / 60.0 * activity;
		if (player.getRandom().nextDouble() >= chancePerSecond) return;
		if (placeOddBlock(player, false)) LAST_NIGHT.put(player.getUuid(), night);
	}

	/** @param anywhere true for /uninvited scare: don't require the player to be away from home */
	public static boolean placeOddBlock(ServerPlayerEntity player, boolean anywhere) {
		BlockPos home = player.getSpawnPointPosition();
		ServerWorld world = player.getServerWorld();
		if (home == null || player.getSpawnPointDimension() != world.getRegistryKey()) return false;
		if (!anywhere && player.getBlockPos().isWithinDistance(home, 24)) return false;
		if (!world.isChunkLoaded(ChunkPos.toLong(home))) return false;

		Random random = world.getRandom();
		for (int attempt = 0; attempt < 24; attempt++) {
			BlockPos guess = home.add(random.nextInt(11) - 5, 0, random.nextInt(11) - 5);
			BlockPos floor = MimicPlacement.findFloor(world, guess, 3);
			if (floor == null || world.isSkyVisible(floor)) continue;
			if (!world.getBlockState(floor).isAir()) continue;
			world.setBlockState(floor, ODD_BLOCKS[random.nextInt(ODD_BLOCKS.length)].getDefaultState());
			return true;
		}
		return false;
	}

	static void forget(UUID player) {
		LAST_NIGHT.remove(player);
	}
}
