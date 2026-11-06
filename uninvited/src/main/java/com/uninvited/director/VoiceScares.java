package com.uninvited.director;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicSpawner;
import com.uninvited.stage.Stage;
import com.uninvited.voicebridge.VoiceBackend;
import com.uninvited.voicebridge.VoiceBridge;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * When the mimic speaks. Contains no Simple Voice Chat types: it only talks to
 * {@link VoiceBridge}, which is empty when SVC is missing, so this is always safe to load.
 */
public final class VoiceScares {
	private static final int COOLDOWN_TICKS = 20 * 30;
	private static final Map<UUID, Long> NEXT = new HashMap<>();

	private VoiceScares() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (isActive()) sendNotice(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID uuid = handler.getPlayer().getUuid();
			NEXT.remove(uuid);
			VoiceBackend backend = VoiceBridge.backend();
			if (backend != null) backend.wipe(uuid);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> wipeAll());
	}

	/** Voice mimicry is switched on AND Simple Voice Chat is installed. */
	public static boolean isActive() {
		return UninvitedConfig.get().voiceMimicry && VoiceBridge.isSvcInstalled();
	}

	public static void sendNotice(ServerPlayerEntity player) {
		player.sendMessage(Text.literal("[UNINVITED] ").formatted(Formatting.DARK_RED)
				.append(Text.literal("Voice mimicry is ON on this server. While you are online, short clips of your "
						+ "Simple Voice Chat audio are kept in server memory (never saved to disk, deleted when you log "
						+ "out) and may be played back to other players by the mimic. Group chats are not captured.")
						.formatted(Formatting.GRAY)), false);
	}

	/** After /uninvited reload: wipe if switched off, tell everyone if switched on. */
	public static void onConfigReloaded(MinecraftServer server, boolean wasOn) {
		boolean on = UninvitedConfig.get().voiceMimicry;
		if (!on) wipeAll();
		if (on && !wasOn && VoiceBridge.isSvcInstalled()) {
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) sendNotice(player);
		}
	}

	private static void wipeAll() {
		VoiceBackend backend = VoiceBridge.backend();
		if (backend != null) backend.wipeAll();
		NEXT.clear();
	}

	/** Once a second per world, with the lone players. */
	static void tick(ServerWorld world, Stage stage, double activity, List<ServerPlayerEntity> lone) {
		if (!isActive() || !stage.atLeast(Stage.BECOMING)) return;
		VoiceBackend backend = VoiceBridge.backend();
		if (backend == null) return;
		double chance = UninvitedConfig.get().voiceChancePerMinute / 60.0 * activity;
		Random random = world.getRandom();
		long now = world.getServer().getTicks();

		for (ServerPlayerEntity player : lone) {
			if (now < NEXT.getOrDefault(player.getUuid(), 0L)) continue;
			MimicEntity mimic = mimicFor(world, player);
			boolean spoke = false;
			if (mimic != null && random.nextDouble() < chance) {
				spoke = speak(backend, mimic, player, random);
			} else if (mimic == null && random.nextDouble() < chance * 0.1) {
				// Rare: your own voice, right behind you.
				spoke = backend.echoBehind(player);
			}
			if (spoke) NEXT.put(player.getUuid(), now + COOLDOWN_TICKS);
		}
	}

	/** The impostor talking to a friend in the taken player's voice. Safe without SVC. */
	public static boolean speakAs(MimicEntity impostor, ServerPlayerEntity listener, UUID voiceOwner) {
		VoiceBackend backend = VoiceBridge.backend();
		if (!isActive() || backend == null || !backend.hasVoice(voiceOwner)) return false;
		return backend.speakFrom(impostor, listener, voiceOwner);
	}

	/** For /uninvited scare ... voice. */
	public static boolean speakNow(ServerPlayerEntity player) {
		VoiceBackend backend = VoiceBridge.backend();
		if (!isActive() || backend == null) return false;
		MimicEntity mimic = mimicFor(player.getServerWorld(), player);
		if (mimic != null && speak(backend, mimic, player, player.getRandom())) return true;
		return backend.echoBehind(player);
	}

	private static boolean speak(VoiceBackend backend, MimicEntity mimic, ServerPlayerEntity player, Random random) {
		// Mostly the friend it's wearing; rarely the target's own voice from behind them.
		if (random.nextFloat() < 0.12f && backend.echoBehind(player)) return true;
		UUID owner = mimic.getDisguiseUuid().orElse(null);
		return owner != null && backend.hasVoice(owner) && backend.speakFrom(mimic, player, owner);
	}

	@Nullable
	private static MimicEntity mimicFor(ServerWorld world, ServerPlayerEntity player) {
		for (MimicEntity mimic : MimicSpawner.activeMimics(world)) {
			if (player.getUuid().equals(mimic.getTargetUuid()) && !mimic.isRevealed()
					&& mimic.getMode() != MimicMode.FLEE && mimic.squaredDistanceTo(player) < 48 * 48) {
				return mimic;
			}
		}
		return null;
	}

	/** One line for /uninvited status. */
	public static String statusLine() {
		if (!VoiceBridge.isSvcInstalled()) return "Voice mimicry: unavailable (Simple Voice Chat not installed)";
		if (!UninvitedConfig.get().voiceMimicry) return "Voice mimicry: off";
		VoiceBackend backend = VoiceBridge.backend();
		return "Voice mimicry: ON, " + (backend == null ? "voice server not running" : backend.clipCount() + " clips in memory");
	}
}
