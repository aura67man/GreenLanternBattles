package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicPlacement;
import com.uninvited.mimic.MimicSenses;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Stalking: keep within a distance band of the target, and when it has to move, move to a spot
 * BEHIND them (outside their field of view), so it's only ever seen out of the corner of an eye.
 * Shared by the follow (stage 3) and hunt (stage 4) behaviour.
 */
final class StalkMovement {
	private final MimicEntity mimic;
	private int repathCooldown;

	StalkMovement(MimicEntity mimic) {
		this.mimic = mimic;
	}

	void reset() {
		repathCooldown = 0;
	}

	void tick(ServerPlayerEntity target, double minDist, double maxDist) {
		mimic.getLookControl().lookAt(target, 30.0f, 30.0f);
		if (--repathCooldown > 0 && !mimic.getNavigation().isIdle()) return;
		repathCooldown = 30 + mimic.getRandom().nextInt(20);

		double dist = mimic.distanceTo(target);
		boolean seen = MimicSenses.isLookedAt(target, mimic, MimicSenses.IN_VIEW);
		boolean inBand = dist >= minDist && dist <= maxDist;
		if (inBand && !seen) {
			// Close enough, far enough, and unnoticed: stand still and watch.
			mimic.getNavigation().stop();
			return;
		}

		// Somewhere behind them, inside the band.
		Vec3d spot = MimicPlacement.findSpotAround(target, minDist, maxDist, 180.0f, 70.0f, false, mimic.getRandom());
		if (spot != null) {
			double speed = dist > maxDist * 1.5 ? 1.25 : 1.0;
			mimic.getNavigation().startMovingTo(spot.x, spot.y, spot.z, speed);
		}
	}
}
