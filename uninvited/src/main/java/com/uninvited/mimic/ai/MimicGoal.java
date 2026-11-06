package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.EnumSet;

/** Base for goals that only run in one {@link MimicMode} and need the target player. */
abstract class MimicGoal extends Goal {
	protected final MimicEntity mimic;
	private final MimicMode mode;
	protected ServerPlayerEntity target;

	MimicGoal(MimicEntity mimic, MimicMode mode) {
		this.mimic = mimic;
		this.mode = mode;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
	}

	@Override
	public boolean canStart() {
		if (mimic.getMode() != mode || mimic.isRevealed()) return false;
		target = mimic.getTargetPlayer();
		return target != null && target.getWorld() == mimic.getWorld();
	}

	@Override
	public boolean shouldContinue() {
		return mimic.getMode() == mode && !mimic.isRevealed() && target != null && target.isAlive()
				&& target == mimic.getTargetPlayer();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		mimic.getNavigation().stop();
		target = null;
	}

	protected void stare() {
		mimic.getLookControl().lookAt(target, 30.0f, 30.0f);
	}
}
