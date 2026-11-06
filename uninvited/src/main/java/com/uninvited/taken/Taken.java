package com.uninvited.taken;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.scare.PrivateSounds;
import com.uninvited.util.Scheduler;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The climax of the hunt. When the mimic catches a lone player it doesn't kill them, it TAKES
 * them:
 * <ol>
 *   <li>The grab: the screen goes black and they can't move.</li>
 *   <li>They wake up alone, somewhere dark, hundreds of blocks away.</li>
 *   <li>Meanwhile a mimic wearing THEIR face walks up to their friends and carries on as them
 *       ({@link TakenTracker}). If they were playing alone, it waits for them at home.</li>
 * </ol>
 * It ends when someone exposes the impostor (Witness Bell, a punch), when the real player gets
 * back to it, or after {@code impostorMinutes}.
 */
public final class Taken {
	private static final int GRAB_TICKS = 30;

	private Taken() {
	}

	/** @param hunter the mimic doing the taking, or null (debug command) */
	public static boolean take(@Nullable MimicEntity hunter, ServerPlayerEntity victim) {
		if (victim.isSpectator() || !victim.isAlive() || TakenTracker.isTaken(victim.getUuid())) return false;
		ServerWorld world = victim.getServerWorld();
		UUID victimId = victim.getUuid();
		BlockPos origin = victim.getBlockPos();
		TakenTracker.begin(victim);

		// 1. The grab.
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, GRAB_TICKS + 60, 0, false, false, false));
		victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, GRAB_TICKS, 255, false, false, false));
		Vec3d at = hunter != null ? hunter.getEyePos() : victim.getEyePos();
		PrivateSounds.play(victim, SoundEvents.ENTITY_PLAYER_HURT, at, 1.0f, 0.5f);
		PrivateSounds.play(victim, SoundEvents.AMBIENT_CAVE, victim.getEyePos(), 1.0f, 0.6f);
		if (hunter != null) {
			hunter.setMode(MimicMode.IDLE);
			hunter.getNavigation().stop();
			hunter.getLookControl().lookAt(victim, 90, 90);
		}

		// 2. They wake up somewhere else.
		Scheduler.runLater(GRAB_TICKS, () -> {
			if (hunter != null && !hunter.isRemoved()) hunter.discard();
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(victimId);
			if (player == null || !player.isAlive()) return;
			BlockPos dest = player.getServerWorld() == world ? TakenDestination.find(world, origin, world.getRandom()) : null;
			if (dest != null) {
				player.stopRiding();
				player.teleport(world, dest.getX() + 0.5, dest.getY(), dest.getZ() + 0.5,
						world.getRandom().nextFloat() * 360f - 180f, 0.0f);
			}
			player.fallDistance = 0;
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 20 * 40, 0, false, false, true));
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 50, 0, false, false, false));
			PrivateSounds.play(player, SoundEvents.AMBIENT_CAVE, player.getEyePos(), 1.0f, 0.5f);

			// 3. Someone else goes back to the group wearing their face.
			TakenTracker.sendImpostor(player);
		});
		Scheduler.runLater(GRAB_TICKS + 70, () -> {
			ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(victimId);
			if (player != null) {
				player.sendMessage(Text.literal("You don't remember walking here.").formatted(Formatting.GRAY, Formatting.ITALIC), true);
			}
		});
		return true;
	}
}
