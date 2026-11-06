package com.uninvited.mimic.ai;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.scare.PrivateSounds;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;

import java.util.UUID;

/**
 * The impostor, after it has taken someone. It walks with the taken player's friends like one
 * of the group: catches up when they move off, stands a few blocks away, looks at them, now and
 * then waves. It never attacks.
 *
 * <p>When the real player gets back and sees it (within 12 blocks), it stops, turns, stares at
 * them for two seconds, and comes apart.
 */
public class ImpostorGoal extends MimicGoal {
	private static final double CONFRONT_DISTANCE = 12.0;
	private int repath;
	private int confrontTicks = -1;

	public ImpostorGoal(MimicEntity mimic) {
		super(mimic, MimicMode.IMPOSTOR);
	}

	@Override
	public void start() {
		repath = 0;
		confrontTicks = -1;
	}

	@Override
	public void tick() {
		ServerPlayerEntity victim = victim();
		if (victim != null && confrontTicks < 0 && mimic.squaredDistanceTo(victim) < CONFRONT_DISTANCE * CONFRONT_DISTANCE
				&& victim.canSee(mimic)) {
			// The real one is back.
			confrontTicks = 0;
			PrivateSounds.play(victim, SoundEvents.AMBIENT_CAVE, mimic.getEyePos(), 1.0f, 0.5f);
		}
		if (confrontTicks >= 0 && victim != null) {
			mimic.getNavigation().stop();
			mimic.getLookControl().lookAt(victim, 90.0f, 90.0f);
			if (++confrontTicks >= 40) mimic.reveal();
			return;
		}

		double distSq = mimic.squaredDistanceTo(target);
		if (target == victim) {
			// Nobody else to fool: it waits at their home, staring.
			mimic.getNavigation().stop();
			stare();
			return;
		}
		if (distSq > 5.0 * 5.0) {
			if (--repath <= 0) {
				repath = 10;
				mimic.getNavigation().startMovingTo(target, distSq > 20.0 * 20.0 ? 1.25 : 1.0);
			}
		} else {
			mimic.getNavigation().stop();
		}
		stare();
		if (mimic.getRandom().nextInt(400) == 0) mimic.swingHand(Hand.MAIN_HAND);
	}

	private ServerPlayerEntity victim() {
		UUID id = mimic.getVictimUuid();
		if (id == null || !(mimic.getWorld() instanceof ServerWorld world)) return null;
		PlayerEntity player = world.getPlayerByUuid(id);
		return player instanceof ServerPlayerEntity serverPlayer && serverPlayer.isAlive() && !serverPlayer.isSpectator() ? serverPlayer : null;
	}
}
