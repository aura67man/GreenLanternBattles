package com.uninvited.scare;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Footsteps that follow the player a few blocks behind, in step with them. When the player
 * stops, the steps stop too... and then there is exactly one more.
 */
public final class FootstepScare {
	private static final class Session {
		int ticksLeft;
		int stepTimer;
		Vec3d lastPos;
		int stillTicks;
		boolean lastStepPlayed = true;
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	private FootstepScare() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(FootstepScare::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());
	}

	public static boolean start(ServerPlayerEntity player) {
		if (SESSIONS.containsKey(player.getUuid())) return false;
		Session session = new Session();
		session.ticksLeft = 20 * (8 + player.getRandom().nextInt(8));
		session.lastPos = player.getPos();
		SESSIONS.put(player.getUuid(), session);
		return true;
	}

	private static void tick(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		for (Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<UUID, Session> entry = it.next();
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
			Session s = entry.getValue();
			if (player == null || !player.isAlive() || --s.ticksLeft <= 0) {
				it.remove();
				continue;
			}

			Vec3d pos = player.getPos();
			double moved = pos.subtract(s.lastPos).horizontalLength();
			s.lastPos = pos;
			boolean walking = moved > 0.04 && player.isOnGround();

			if (walking) {
				s.stillTicks = 0;
				s.lastStepPlayed = false;
				// Roughly a walking (every 8 ticks) or sprinting (every 6 ticks) cadence.
				if (--s.stepTimer <= 0) {
					s.stepTimer = player.isSprinting() ? 6 : 8;
					step(player);
				}
			} else {
				s.stillTicks++;
				// They stopped. So did it. Then: one more step.
				if (!s.lastStepPlayed && s.stillTicks == 14) {
					s.lastStepPlayed = true;
					step(player);
				}
			}
		}
	}

	private static void step(ServerPlayerEntity player) {
		Vec3d at = PrivateSounds.behind(player, 3.0);
		BlockPos below = BlockPos.ofFloored(at.x, player.getY() - 0.2, at.z);
		BlockState floor = player.getWorld().getBlockState(below);
		BlockSoundGroup group = floor.isAir() ? BlockSoundGroup.STONE : floor.getSoundGroup();
		// Slightly slower and lower than a real step.
		PrivateSounds.play(player, group.getStepSound(), at, group.getVolume() * 0.5f, group.getPitch() * 0.82f);
	}
}
