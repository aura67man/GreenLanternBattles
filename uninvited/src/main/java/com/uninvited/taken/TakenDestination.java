package com.uninvited.taken;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicPlacement;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

/**
 * Where a taken player wakes up: 250-450 blocks from where they were grabbed, in a random
 * direction, preferably in a cave (at least 8 blocks under the surface, with a bit of room
 * around them), otherwise on the surface. Never in fluid, never next to lava, never outside
 * the world border.
 */
final class TakenDestination {
	private TakenDestination() {
	}

	@Nullable
	static BlockPos find(ServerWorld world, BlockPos origin, Random random) {
		UninvitedConfig config = UninvitedConfig.get();
		BlockPos surfaceFallback = null;
		for (int attempt = 0; attempt < 10; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double dist = config.takenDistanceMin + random.nextDouble() * (config.takenDistanceMax - config.takenDistanceMin);
			int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
			int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);
			if (!world.getWorldBorder().contains(new BlockPos(x, origin.getY(), z))) continue;

			// Loads (and if needed generates) the chunk, so we can look inside it.
			world.getChunk(x >> 4, z >> 4);

			BlockPos cave = findCave(world, x, z);
			if (cave != null) return cave;
			if (surfaceFallback == null) surfaceFallback = findSurface(world, x, z);
		}
		return surfaceFallback;
	}

	@Nullable
	private static BlockPos findCave(ServerWorld world, int x, int z) {
		boolean ceiling = world.getDimension().hasCeiling();
		for (int dx = -4; dx <= 4; dx += 4) {
			for (int dz = -4; dz <= 4; dz += 4) {
				int cx = x + dx, cz = z + dz;
				int top = ceiling ? 100 : world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 8;
				int bottom = world.getBottomY() + 6;
				for (int y = top; y > bottom; y--) {
					BlockPos pos = new BlockPos(cx, y, cz);
					if (MimicPlacement.canStandAt(world, pos) && roomy(world, pos) && !lavaNear(world, pos)) return pos;
				}
			}
		}
		return null;
	}

	@Nullable
	private static BlockPos findSurface(ServerWorld world, int x, int z) {
		if (world.getDimension().hasCeiling()) return null;
		BlockPos pos = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
		return MimicPlacement.canStandAt(world, pos) && !lavaNear(world, pos) ? pos : null;
	}

	/** A cave, not a coffin: enough air around to move. */
	private static boolean roomy(ServerWorld world, BlockPos feet) {
		int air = 0;
		for (BlockPos p : BlockPos.iterate(feet.add(-2, 0, -2), feet.add(2, 2, 2))) {
			if (world.getBlockState(p).getCollisionShape(world, p).isEmpty() && world.getFluidState(p).isEmpty()) air++;
		}
		return air >= 30;
	}

	private static boolean lavaNear(ServerWorld world, BlockPos feet) {
		for (BlockPos p : BlockPos.iterate(feet.add(-3, -2, -3), feet.add(3, 3, 3))) {
			if (world.getFluidState(p).isIn(FluidTags.LAVA)) return true;
		}
		return false;
	}
}
