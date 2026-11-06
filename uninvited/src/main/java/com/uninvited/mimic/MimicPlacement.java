package com.uninvited.mimic;

import com.uninvited.salt.SaltRules;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.jetbrains.annotations.Nullable;

/** Finds somewhere for a mimic to stand: solid floor, two blocks of air, no fluid, no salt. */
public final class MimicPlacement {
	private MimicPlacement() {
	}

	/**
	 * @param yawOffset    direction relative to where the player faces (0 = in front, 180 = behind)
	 * @param spread       random +- degrees around that direction
	 * @param wantVisible  prefer spots the player has a clear line of sight to
	 */
	@Nullable
	public static Vec3d findSpotAround(ServerPlayerEntity player, double minDist, double maxDist,
									   float yawOffset, float spread, boolean wantVisible, Random random) {
		Vec3d fallback = null;
		for (int attempt = 0; attempt < 32; attempt++) {
			float yaw = player.getYaw() + yawOffset + (random.nextFloat() * 2 - 1) * spread;
			double dist = MathHelper.lerp(random.nextDouble(), minDist, maxDist);
			// Minecraft yaw: 0 = +Z (south), 90 = -X (west).
			double x = player.getX() - Math.sin(Math.toRadians(yaw)) * dist;
			double z = player.getZ() + Math.cos(Math.toRadians(yaw)) * dist;
			BlockPos spot = findFloor(player.getServerWorld(), BlockPos.ofFloored(x, player.getY(), z), 16);
			if (spot == null) continue;
			Vec3d pos = Vec3d.ofBottomCenter(spot);
			if (!wantVisible || hasLineOfSight(player, pos.add(0, 1.6, 0))) return pos;
			if (fallback == null) fallback = pos;
		}
		return fallback;
	}

	/** Scans up and down from {@code start} for a standable block position. */
	@Nullable
	public static BlockPos findFloor(ServerWorld world, BlockPos start, int range) {
		if (!world.isChunkLoaded(ChunkPos.toLong(start))) return null;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 0; i <= range * 2; i++) {
			// 0, +1, -1, +2, -2 ...
			int dy = (i % 2 == 0) ? i / 2 : -(i + 1) / 2;
			pos.set(start.getX(), start.getY() + dy, start.getZ());
			if (canStandAt(world, pos)) return pos.toImmutable();
		}
		return null;
	}

	public static boolean canStandAt(ServerWorld world, BlockPos feet) {
		if (feet.getY() <= world.getBottomY() || feet.getY() >= world.getTopY() - 2) return false;
		BlockPos below = feet.down();
		BlockState floor = world.getBlockState(below);
		if (!floor.isSideSolidFullSquare(world, below, Direction.UP)) return false;
		for (int h = 0; h < 2; h++) {
			BlockPos p = feet.up(h);
			BlockState state = world.getBlockState(p);
			if (!state.getCollisionShape(world, p).isEmpty()) return false;
			if (!world.getFluidState(p).isEmpty()) return false;
			if (SaltRules.isSalt(state)) return false;
		}
		return true;
	}

	public static boolean hasLineOfSight(ServerPlayerEntity player, Vec3d point) {
		Vec3d eye = player.getEyePos();
		return player.getWorld().raycast(new RaycastContext(eye, point, RaycastContext.ShapeType.VISUAL,
				RaycastContext.FluidHandling.NONE, player)).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
	}
}
