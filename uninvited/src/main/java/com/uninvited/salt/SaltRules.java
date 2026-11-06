package com.uninvited.salt;

import com.uninvited.item.ModItems;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Single place that answers "is this salt?" and "is there salt between these two points?". */
public final class SaltRules {
	private SaltRules() {
	}

	public static boolean isSalt(BlockState state) {
		return state.isOf(ModItems.SALT_BLOCK);
	}

	/**
	 * True if a salt block lies on the floor-level line between two entities' feet. Used so the
	 * mimic can't swing at someone standing just inside a ring from just outside it.
	 */
	public static boolean saltBetween(World world, Vec3d from, Vec3d to) {
		double dist = from.distanceTo(to);
		int steps = Math.max(1, MathHelper.ceil(dist * 4));
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int i = 0; i <= steps; i++) {
			Vec3d p = from.lerp(to, i / (double) steps);
			pos.set(p.x, p.y + 0.1, p.z);
			if (isSalt(world.getBlockState(pos))) return true;
		}
		return false;
	}
}
