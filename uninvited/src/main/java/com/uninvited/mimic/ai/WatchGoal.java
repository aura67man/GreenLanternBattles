package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicSenses;

/**
 * Stage 2, "Watching": stands at the edge of render distance and stares. It is gone the moment
 * you look straight at it for about a second, or get within ~20 blocks.
 */
public class WatchGoal extends MimicGoal {
	private static final int LOOK_TICKS_TO_VANISH = 20;
	private static final double VANISH_DISTANCE = 20.0;

	private int lookedAtTicks;

	public WatchGoal(MimicEntity mimic) {
		super(mimic, MimicMode.WATCH);
	}

	@Override
	public void start() {
		lookedAtTicks = 0;
		mimic.getNavigation().stop();
	}

	@Override
	public void tick() {
		mimic.getNavigation().stop();
		stare();

		if (MimicSenses.isLookedAt(target, mimic, MimicSenses.DIRECT_LOOK)) {
			lookedAtTicks++;
		} else {
			lookedAtTicks = Math.max(0, lookedAtTicks - 1);
		}
		if (lookedAtTicks >= LOOK_TICKS_TO_VANISH || mimic.squaredDistanceTo(target) < VANISH_DISTANCE * VANISH_DISTANCE) {
			mimic.vanish();
		}
	}
}
