package com.uninvited.mimic;

import com.uninvited.stage.UninvitedState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Creating mimics and choosing whose face they wear. */
public final class MimicSpawner {
	private MimicSpawner() {
	}

	public record Disguise(UninvitedState.KnownPlayer player, @Nullable ServerPlayerEntity online) {
	}

	/** Every mimic currently loaded in this world. */
	public static List<? extends MimicEntity> activeMimics(ServerWorld world) {
		return world.getEntitiesByType(MimicEntities.MIMIC, mimic -> mimic.isAlive() && !mimic.isRemoved());
	}

	public static int countAll(MinecraftServer server) {
		int count = 0;
		for (ServerWorld world : server.getWorlds()) count += activeMimics(world).size();
		return count;
	}

	/**
	 * Picks a friend of {@code target} to impersonate. Prefers someone who is NOT near the target
	 * (an offline friend, or one far away), because "they're over there AND over here" breaks
	 * the illusion too early. Falls back to the target's own face if nobody else has ever joined.
	 */
	public static Disguise pickDisguiseFor(ServerPlayerEntity target, Random random) {
		MinecraftServer server = target.getServer();
		UninvitedState state = UninvitedState.get(server);
		List<Disguise> far = new ArrayList<>();
		List<Disguise> near = new ArrayList<>();
		for (UninvitedState.KnownPlayer known : state.knownPlayers()) {
			if (known.uuid().equals(target.getUuid())) continue;
			ServerPlayerEntity online = server.getPlayerManager().getPlayer(known.uuid());
			boolean isNear = online != null && online.getWorld() == target.getWorld() && online.squaredDistanceTo(target) < 64 * 64;
			(isNear ? near : far).add(new Disguise(known, online));
		}
		if (!far.isEmpty()) return far.get(random.nextInt(far.size()));
		if (!near.isEmpty()) return near.get(random.nextInt(near.size()));
		return disguiseOf(target);
	}

	/** Disguise as a specific player (used by /uninvited summon). */
	public static Disguise disguiseOf(ServerPlayerEntity player) {
		UninvitedState.KnownPlayer known = UninvitedState.get(player.getServer()).knownPlayer(player.getUuid());
		if (known == null) {
			known = new UninvitedState.KnownPlayer(player.getUuid(), player.getGameProfile().getName(), null, null);
		}
		return new Disguise(known, player);
	}

	public static MimicEntity spawn(ServerWorld world, Vec3d pos, float yaw, Disguise disguise,
									@Nullable ServerPlayerEntity target, MimicMode mode, int lifetimeTicks) {
		MimicEntity mimic = MimicEntities.MIMIC.create(world);
		if (mimic == null) return null;
		mimic.refreshPositionAndAngles(pos.x, pos.y, pos.z, yaw, 0.0f);
		mimic.setHeadYaw(yaw);
		mimic.setBodyYaw(yaw);
		mimic.applyConfigAttributes();
		mimic.disguiseAs(disguise.player(), disguise.online(), world.getRandom().nextLong());
		mimic.setMode(mode);
		mimic.setTargetUuid(target == null ? null : target.getUuid());
		mimic.setLifetimeTicks(lifetimeTicks);
		world.spawnEntity(mimic);
		return mimic;
	}
}
