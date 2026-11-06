package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.util.AloneUtil;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Rules that apply on top of whatever goal is running, checked every tick on the server.
 * This is where "it only hunts players who are alone" is enforced for an existing mimic.
 */
public final class MimicBrain {
	private MimicBrain() {
	}

	public static void tick(MimicEntity mimic) {
		MimicMode mode = mimic.getMode();
		if (mode == MimicMode.IDLE || mode == MimicMode.FLEE || mimic.isRevealed()) return;

		ServerPlayerEntity target = mimic.getTargetPlayer();
		if (target == null || target.getWorld() != mimic.getWorld() || mimic.squaredDistanceTo(target) > 160 * 160) {
			// Target logged out, died, changed dimension or ran very far: nothing left to haunt.
			mimic.vanish();
			return;
		}

		// Only check a few times a second; the group can't form faster than that.
		if (mimic.age % 5 != 0) return;
		if (!AloneUtil.isAlone(target)) {
			// They found their friends. A watcher just isn't there any more; anything closer runs.
			if (mode == MimicMode.WATCH) mimic.vanish();
			else mimic.setMode(MimicMode.FLEE);
		}
	}
}
