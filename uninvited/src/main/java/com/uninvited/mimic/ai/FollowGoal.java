package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;

/**
 * Stage 3, "Becoming": walks around as a copy of a friend, keeping 14-28 blocks away and staying
 * behind the target. (When they turn round and look, {@link LureGoal} takes over and it waves.)
 */
public class FollowGoal extends MimicGoal {
	private final StalkMovement stalk;

	public FollowGoal(MimicEntity mimic) {
		super(mimic, MimicMode.FOLLOW);
		this.stalk = new StalkMovement(mimic);
	}

	@Override
	public void start() {
		stalk.reset();
	}

	@Override
	public void tick() {
		stalk.tick(target, 14.0, 28.0);
		// Stage 3 never lets itself be caught.
		if (mimic.squaredDistanceTo(target) < 6.0 * 6.0) mimic.vanish();
	}
}
