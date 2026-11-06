package com.uninvited.mimic;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/** "Can they see me?" checks shared by the AI goals and the spawn director. */
public final class MimicSenses {
	/** cos(10 deg): looking straight at it. */
	public static final double DIRECT_LOOK = 0.985;
	/** cos(35 deg): it is somewhere in the middle of their screen. */
	public static final double IN_VIEW = 0.82;

	private MimicSenses() {
	}

	/** How directly the player is looking at {@code target}: 1 = dead centre, -1 = behind them. */
	public static double lookDot(ServerPlayerEntity player, Entity target) {
		Vec3d look = player.getRotationVec(1.0f);
		Vec3d toTarget = target.getEyePos().subtract(player.getEyePos());
		double length = toTarget.length();
		if (length < 1.0e-4) return 1.0;
		return look.dotProduct(toTarget.multiply(1.0 / length));
	}

	/** Is the player looking at it within {@code dotThreshold}, with nothing in the way? */
	public static boolean isLookedAt(ServerPlayerEntity player, Entity target, double dotThreshold) {
		return lookDot(player, target) >= dotThreshold && player.canSee(target);
	}
}
