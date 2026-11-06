package com.uninvited.mimic.ai;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import net.minecraft.util.Hand;
import net.minecraft.world.Difficulty;

/**
 * Stage 4, "Hunting". Four phases:
 * <ol>
 *   <li>STALK: shadows the lone target from behind for a while.</li>
 *   <li>APPROACH: walks up openly, like a friend would, and waves when it gets close.</li>
 *   <li>PAUSE: stops a few blocks away and just stares.</li>
 *   <li>CHARGE: sprints in and attacks. After a few hits (or if it takes too much damage) it
 *       runs and vanishes, so a player who fights back or runs to their friends survives.</li>
 * </ol>
 * If it can't reach the target (salt ring, pillar, water), it goes back to stalking the edge.
 */
public class HuntGoal extends MimicGoal {
	private enum Phase { STALK, APPROACH, PAUSE, CHARGE }

	private static final int MAX_HITS = 3;
	private static final int MAX_CHARGE_TICKS = 20 * 15;

	private final StalkMovement stalk;
	private Phase phase;
	private int phaseTicks;
	private int phaseLength;
	private int attackCooldown;
	private int hits;
	private int stuckTicks;
	private boolean waved;

	public HuntGoal(MimicEntity mimic) {
		super(mimic, MimicMode.HUNT);
		this.stalk = new StalkMovement(mimic);
	}

	@Override
	public void start() {
		hits = 0;
		enter(Phase.STALK, 20 * (8 + mimic.getRandom().nextInt(15)));
	}

	private void enter(Phase next, int length) {
		phase = next;
		phaseTicks = 0;
		phaseLength = length;
		stuckTicks = 0;
		waved = false;
		stalk.reset();
		mimic.getNavigation().stop();
		mimic.setSprinting(next == Phase.CHARGE);
	}

	@Override
	public void tick() {
		phaseTicks++;
		if (attackCooldown > 0) attackCooldown--;
		double distSq = mimic.squaredDistanceTo(target);
		boolean peaceful = mimic.getWorld().getDifficulty() == Difficulty.PEACEFUL;

		switch (phase) {
			case STALK -> {
				stalk.tick(target, 16.0, 30.0);
				if (phaseTicks >= phaseLength) enter(Phase.APPROACH, 20 * 30);
			}
			case APPROACH -> {
				stare();
				if (phaseTicks % 10 == 1) {
					if (!mimic.getNavigation().startMovingTo(target, 0.9)) stuckTicks += 10;
				}
				if (!waved && distSq < 12.0 * 12.0) {
					waved = true;
					mimic.swingHand(Hand.MAIN_HAND);
				}
				if (distSq < 5.0 * 5.0) enter(Phase.PAUSE, 20 + mimic.getRandom().nextInt(25));
				else if (stuckTicks > 60 || phaseTicks > phaseLength) enter(Phase.STALK, 20 * 10);
			}
			case PAUSE -> {
				mimic.getNavigation().stop();
				stare();
				if (phaseTicks >= phaseLength) {
					if (peaceful) mimic.setMode(MimicMode.FLEE);
					else enter(Phase.CHARGE, MAX_CHARGE_TICKS);
				}
			}
			case CHARGE -> {
				stare();
				if (phaseTicks % 5 == 1 && !mimic.getNavigation().startMovingTo(target, 1.4)) stuckTicks += 5;
				double reach = mimic.getWidth() * 2.0 + target.getWidth();
				if (distSq <= reach * reach && attackCooldown <= 0 && mimic.canSee(target)) {
					attackCooldown = UninvitedConfig.get().mimicAttackCooldownTicks;
					mimic.swingHand(Hand.MAIN_HAND);
					if (mimic.tryAttack(target)) hits++;
				}
				if (hits >= MAX_HITS || phaseTicks >= phaseLength) {
					mimic.setMode(MimicMode.FLEE);
				} else if (stuckTicks > 60) {
					// Can't get to them. Stand at the edge and wait.
					enter(Phase.STALK, 20 * 10);
				}
			}
		}
	}

	@Override
	public void stop() {
		super.stop();
		mimic.setSprinting(false);
	}
}
