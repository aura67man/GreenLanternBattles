package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicSenses;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.Vec3d;

import java.util.EnumSet;

/**
 * Fleeing/vanishing: runs from the target and is gone as soon as they lose sight of it (or after
 * three seconds regardless). Triggered when the target rejoins the group or the hunt is over.
 */
public class FleeGoal extends Goal {
	private final MimicEntity mimic;
	private int ticks;

	public FleeGoal(MimicEntity mimic) {
		this.mimic = mimic;
		this.setControls(EnumSet.of(Control.MOVE, Control.LOOK, Control.JUMP));
	}

	@Override
	public boolean canStart() {
		return mimic.getMode() == MimicMode.FLEE && !mimic.isRevealed();
	}

	@Override
	public boolean shouldContinue() {
		return canStart();
	}

	@Override
	public boolean shouldRunEveryTick() {
		return true;
	}

	@Override
	public void start() {
		ticks = 0;
		mimic.setSprinting(true);
		var target = mimic.getTargetPlayer();
		Vec3d away = target == null ? null : NoPenaltyTargeting.findFrom(mimic, 20, 7, target.getPos());
		if (away == null) {
			mimic.vanish();
			return;
		}
		mimic.getNavigation().startMovingTo(away.x, away.y, away.z, 1.45);
	}

	@Override
	public void tick() {
		ticks++;
		var target = mimic.getTargetPlayer();
		boolean unseen = target == null || !MimicSenses.isLookedAt(target, mimic, 0.3);
		if ((ticks > 10 && unseen) || ticks > 60 || mimic.getNavigation().isIdle()) {
			mimic.vanish();
		}
	}
}
