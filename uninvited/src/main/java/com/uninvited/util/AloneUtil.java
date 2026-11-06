package com.uninvited.util;

import com.uninvited.config.UninvitedConfig;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;

/**
 * The core rule of UNINVITED: it only hunts players who are alone.
 *
 * <p>A player is alone when no OTHER living, non-spectator player is within
 * {@link UninvitedConfig#aloneRadius} blocks in the same dimension. Every scare, spawn and
 * attack goes through this class, so the rule stays consistent everywhere.
 */
public final class AloneUtil {
	private AloneUtil() {
	}

	/** Can this player be targeted at all? (alive, in a world, not a spectator) */
	public static boolean canBeTargeted(ServerPlayerEntity player) {
		return player.isAlive() && !player.isSpectator() && !player.isRemoved();
	}

	/** Does this player count as company for someone else? */
	public static boolean countsAsCompany(ServerPlayerEntity player) {
		return player.isAlive() && !player.isSpectator() && !player.isRemoved();
	}

	public static boolean isAlone(ServerPlayerEntity player) {
		return isAlone(player, UninvitedConfig.get().aloneRadius);
	}

	public static boolean isAlone(ServerPlayerEntity player, double radius) {
		if (!canBeTargeted(player)) return false;
		return nearestCompanionDistanceSq(player) > radius * radius;
	}

	/** Squared distance to the nearest other player who counts as company, or +infinity. */
	public static double nearestCompanionDistanceSq(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		double best = Double.POSITIVE_INFINITY;
		for (ServerPlayerEntity other : world.getPlayers()) {
			if (other == player || !countsAsCompany(other)) continue;
			best = Math.min(best, other.squaredDistanceTo(player));
		}
		return best;
	}

	/** All players in this world who are currently alone. */
	public static List<ServerPlayerEntity> lonePlayers(ServerWorld world) {
		List<ServerPlayerEntity> result = new ArrayList<>();
		for (ServerPlayerEntity player : world.getPlayers()) {
			if (isAlone(player)) result.add(player);
		}
		return result;
	}
}
