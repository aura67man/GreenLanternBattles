package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicSenses;
import net.minecraft.util.Hand;

/**
 * Luring: when the target notices it, it stops dead, faces them and waves (swings its arm a few
 * times), then just stands there. Come any closer than 8 blocks and it is gone.
 */
public class LureGoal extends MimicGoal {
	private int ticks;
	private int duration;
	private int cooldown;

	public LureGoal(MimicEntity mimic) {
		super(mimic, MimicMode.FOLLOW);
	}

	@Override
	public boolean canStart() {
		if (cooldown > 0) {
			cooldown--;
			return false;
		}
		if (!super.canStart()) return false;
		double dist = mimic.distanceTo(target);
		return dist > 8.0 && dist < 48.0 && MimicSenses.isLookedAt(target, mimic, MimicSenses.IN_VIEW);
	}

	@Override
	public void start() {
		ticks = 0;
		duration = 60 + mimic.getRandom().nextInt(60);
		mimic.getNavigation().stop();
	}

	@Override
	public boolean shouldContinue() {
		return super.shouldContinue() && ticks < duration;
	}

	@Override
	public void tick() {
		ticks++;
		mimic.getNavigation().stop();
		stare();
		// Three slow waves, then stillness.
		if (ticks <= 40 && ticks % 12 == 4) mimic.swingHand(Hand.MAIN_HAND);
		if (mimic.squaredDistanceTo(target) < 8.0 * 8.0) mimic.vanish();
	}

	@Override
	public void stop() {
		super.stop();
		cooldown = 200;
	}
}
