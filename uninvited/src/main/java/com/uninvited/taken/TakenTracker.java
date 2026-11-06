package com.uninvited.taken;

import com.uninvited.chat.ChatMemory;
import com.uninvited.chat.FakeChat;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.director.VoiceScares;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicPlacement;
import com.uninvited.mimic.MimicSpawner;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps track of who has been taken and runs their impostor: where it appears, who it walks
 * with, and what it says in their name. All in memory; a restart ends every abduction.
 */
public final class TakenTracker {
	/** Things nobody typed, said in the taken player's name when their own chat history runs dry. */
	private static final String[] IMPOSTOR_LINES = {
			"hey", "wait up", "where'd everyone go?", "im back", "sorry, got lost", "this way",
			"follow me", "come look at this", "im right here", "it's me", "that wasnt me", "dont listen to them"
	};

	private static final class Abduction {
		final UUID victim;
		final String victimName;
		final long startedTick;
		RegistryKey<World> homeWorld;
		BlockPos home;
		@Nullable UUID impostor;
		@Nullable RegistryKey<World> impostorWorld;
		boolean waitingAtHome;
		/** True from the grab until the impostor has been sent out. */
		boolean grabbing = true;
		long nextChatTick;

		Abduction(ServerPlayerEntity victim, long now) {
			this.victim = victim.getUuid();
			this.victimName = victim.getGameProfile().getName();
			this.startedTick = now;
			BlockPos spawn = victim.getSpawnPointPosition();
			if (spawn != null) {
				this.home = spawn;
				this.homeWorld = victim.getSpawnPointDimension();
			} else {
				this.home = victim.getBlockPos();
				this.homeWorld = victim.getWorld().getRegistryKey();
			}
		}
	}

	private static final Map<UUID, Abduction> ACTIVE = new HashMap<>();

	private TakenTracker() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 20 == 0 && !ACTIVE.isEmpty()) tick(server);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	public static boolean isTaken(UUID player) {
		return ACTIVE.containsKey(player);
	}

	static void begin(ServerPlayerEntity victim) {
		ACTIVE.put(victim.getUuid(), new Abduction(victim, victim.getServer().getTicks()));
	}

	/**
	 * Sends the impostor to the taken player's friends: the online player nearest to where they
	 * were taken. It appears out of sight behind that friend and walks up. With nobody else
	 * online, it goes home instead and waits there.
	 */
	static void sendImpostor(ServerPlayerEntity victim) {
		Abduction abduction = ACTIVE.get(victim.getUuid());
		if (abduction == null) return;
		abduction.grabbing = false;
		ServerPlayerEntity friend = nearestFriend(victim.getServer(), victim.getUuid(), victim);
		if (friend == null) {
			abduction.waitingAtHome = true;
			return;
		}
		Random random = friend.getRandom();
		Vec3d spot = MimicPlacement.findSpotAround(friend, 22, 32, 180.0f, 50.0f, false, random);
		if (spot == null) spot = MimicPlacement.findSpotAround(friend, 12, 32, 0.0f, 180.0f, false, random);
		if (spot == null) {
			abduction.waitingAtHome = true;
			return;
		}
		spawnImpostor(abduction, friend.getServerWorld(), spot, victim, friend);
	}

	private static void spawnImpostor(Abduction abduction, ServerWorld world, Vec3d spot, ServerPlayerEntity victim, ServerPlayerEntity audience) {
		float yaw = (float) (MathHelper.atan2(audience.getZ() - spot.z, audience.getX() - spot.x) * MathHelper.DEGREES_PER_RADIAN) - 90.0f;
		MimicEntity impostor = MimicSpawner.spawn(world, spot, yaw, MimicSpawner.disguiseOf(victim), audience, MimicMode.IMPOSTOR,
				UninvitedConfig.get().impostorMinutes * 60 * 20);
		if (impostor == null) return;
		impostor.setVictimUuid(victim.getUuid());
		abduction.impostor = impostor.getUuid();
		abduction.impostorWorld = world.getRegistryKey();
		abduction.waitingAtHome = false;
		abduction.nextChatTick = world.getServer().getTicks() + 20 * 12;
	}

	/** Online, non-spectator player nearest to {@code near}, excluding the victim. Prefers the same world. */
	@Nullable
	public static ServerPlayerEntity nearestFriend(MinecraftServer server, UUID victim, Entity near) {
		ServerPlayerEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.getUuid().equals(victim) || player.isSpectator() || !player.isAlive() || isTaken(player.getUuid())) continue;
			double score = player.getWorld() == near.getWorld() ? player.squaredDistanceTo(near) : 1.0e12;
			if (score < bestScore) {
				bestScore = score;
				best = player;
			}
		}
		return best;
	}

	private static void tick(MinecraftServer server) {
		long now = server.getTicks();
		long maxTicks = UninvitedConfig.get().impostorMinutes * 60L * 20L;
		for (Iterator<Abduction> it = ACTIVE.values().iterator(); it.hasNext(); ) {
			Abduction a = it.next();
			ServerPlayerEntity victim = server.getPlayerManager().getPlayer(a.victim);

			if (a.grabbing) {
				// Mid-grab; the impostor hasn't been sent yet. (Expires if they logged out mid-grab.)
				if (now - a.startedTick > 20 * 10) it.remove();
				continue;
			}
			if (a.waitingAtHome) {
				if (now - a.startedTick > maxTicks * 2) {
					it.remove();
				} else if (victim != null && victim.getWorld().getRegistryKey() == a.homeWorld
						&& victim.getBlockPos().isWithinDistance(a.home, 40)) {
					// Coming home... and someone is already there.
					Vec3d spot = spotNearHome(victim.getServerWorld(), a.home, victim.getRandom());
					if (spot != null) spawnImpostor(a, victim.getServerWorld(), spot, victim, victim);
				}
				continue;
			}

			MimicEntity impostor = findImpostor(server, a);
			if (impostor == null) {
				it.remove();
				continue;
			}
			if (now >= a.nextChatTick) {
				a.nextChatTick = now + (long) (UninvitedConfig.get().impostorChatSeconds * 20 * (0.6 + impostor.getRandom().nextDouble() * 0.8));
				speak(server, a, impostor);
			}
		}
	}

	/** A chat line (and, with voice mimicry on, a clip of their voice) to the friends it's with. */
	private static void speak(MinecraftServer server, Abduction a, MimicEntity impostor) {
		List<ServerPlayerEntity> audience = new ArrayList<>();
		if (impostor.getWorld() instanceof ServerWorld world) {
			for (ServerPlayerEntity player : world.getPlayers()) {
				if (!player.getUuid().equals(a.victim) && !player.isSpectator() && player.squaredDistanceTo(impostor) < 48 * 48) {
					audience.add(player);
				}
			}
		}
		if (audience.isEmpty()) return;
		Random random = impostor.getRandom();
		String line = random.nextFloat() < 0.55f ? FakeChat.pickLine(ChatMemory.linesOf(a.victim), random) : null;
		if (line == null) line = IMPOSTOR_LINES[random.nextInt(IMPOSTOR_LINES.length)];
		Text message = FakeChat.asChatFrom(server, a.victim, a.victimName, line);
		for (ServerPlayerEntity player : audience) player.sendMessage(message, false);

		ServerPlayerEntity listener = impostor.getTargetPlayer();
		if (listener != null && !listener.getUuid().equals(a.victim)) VoiceScares.speakAs(impostor, listener, a.victim);
	}

	@Nullable
	private static MimicEntity findImpostor(MinecraftServer server, Abduction a) {
		if (a.impostor == null || a.impostorWorld == null) return null;
		ServerWorld world = server.getWorld(a.impostorWorld);
		Entity entity = world == null ? null : world.getEntity(a.impostor);
		return entity instanceof MimicEntity mimic && mimic.isAlive() && !mimic.isRemoved() ? mimic : null;
	}

	@Nullable
	private static Vec3d spotNearHome(ServerWorld world, BlockPos home, Random random) {
		for (int attempt = 0; attempt < 16; attempt++) {
			BlockPos guess = home.add(random.nextInt(7) - 3, 0, random.nextInt(7) - 3);
			BlockPos floor = MimicPlacement.findFloor(world, guess, 4);
			if (floor != null && !floor.equals(home)) return Vec3d.ofBottomCenter(floor);
		}
		return null;
	}

	/** Lines for /uninvited status. */
	public static List<String> statusLines(MinecraftServer server) {
		List<String> lines = new ArrayList<>();
		for (Abduction a : ACTIVE.values()) {
			if (a.waitingAtHome) {
				lines.add("Taken: " + a.victimName + " (it is waiting for them at home)");
				continue;
			}
			MimicEntity impostor = findImpostor(server, a);
			ServerPlayerEntity with = impostor == null ? null : impostor.getTargetPlayer();
			lines.add("Taken: " + a.victimName + " (impostor " + (with == null ? "alone" : "with " + with.getGameProfile().getName()) + ")");
		}
		return lines;
	}
}
