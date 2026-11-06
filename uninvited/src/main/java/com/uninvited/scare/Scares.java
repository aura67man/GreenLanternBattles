package com.uninvited.scare;

import com.uninvited.chat.FakeChat;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.director.BaseMeddling;
import com.uninvited.director.MimicDirector;
import com.uninvited.director.VoiceScares;
import com.uninvited.mimic.MimicMode;
import com.uninvited.stage.Stage;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.random.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Rolls and runs the small scares (sounds, doors, chests, torches, fake chat) for lone players. */
public final class Scares {
	private static final ScareType[] AMBIENT = {ScareType.FOOTSTEPS, ScareType.DOOR, ScareType.CHEST, ScareType.TORCH};
	private static final int AMBIENT_COOLDOWN_TICKS = 20 * 30;
	private static final int CHAT_COOLDOWN_TICKS = 20 * 90;

	private static final Map<UUID, Long> NEXT_AMBIENT = new HashMap<>();
	private static final Map<UUID, Long> NEXT_CHAT = new HashMap<>();

	private Scares() {
	}

	public static void register() {
		FootstepScare.register();
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			NEXT_AMBIENT.remove(handler.getPlayer().getUuid());
			NEXT_CHAT.remove(handler.getPlayer().getUuid());
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			NEXT_AMBIENT.clear();
			NEXT_CHAT.clear();
		});
	}

	/** Once a second, for each lone player. */
	public static void tick(ServerPlayerEntity player, Stage stage, double activity) {
		UninvitedConfig config = UninvitedConfig.get();
		long now = player.getServer().getTicks();
		Random random = player.getRandom();

		if (stage.atLeast(Stage.SETTLING_IN) && now >= NEXT_AMBIENT.getOrDefault(player.getUuid(), 0L)
				&& random.nextDouble() < config.scareChancePerMinute / 60.0 * activity) {
			if (runAmbient(player)) NEXT_AMBIENT.put(player.getUuid(), now + AMBIENT_COOLDOWN_TICKS);
		}

		if (config.fakeChat && stage.atLeast(Stage.BECOMING) && now >= NEXT_CHAT.getOrDefault(player.getUuid(), 0L)
				&& random.nextDouble() < config.fakeChatChancePerMinute / 60.0 * activity) {
			if (FakeChat.sendTo(player, false)) NEXT_CHAT.put(player.getUuid(), now + CHAT_COOLDOWN_TICKS);
		}
	}

	/** Tries the ambient scares in random order until one finds something to do. */
	private static boolean runAmbient(ServerPlayerEntity player) {
		List<ScareType> order = new ArrayList<>(List.of(AMBIENT));
		Collections.shuffle(order, new java.util.Random(player.getRandom().nextLong()));
		for (ScareType type : order) {
			if (run(player, type, Stage.SETTLING_IN)) return true;
		}
		return false;
	}

	/** A random scare that fits the stage (for /uninvited scare). Returns what happened, or null. */
	public static ScareType runRandom(ServerPlayerEntity player, Stage stage) {
		List<ScareType> options = new ArrayList<>();
		for (ScareType type : ScareType.values()) {
			if (stage.atLeast(type.minStage)) options.add(type);
		}
		if (options.isEmpty()) options.addAll(List.of(AMBIENT));
		Collections.shuffle(options, new java.util.Random(player.getRandom().nextLong()));
		for (ScareType type : options) {
			if (run(player, type, stage)) return type;
		}
		return null;
	}

	/** Runs one specific scare. Debug callers may run it below its usual stage. */
	public static boolean run(ServerPlayerEntity player, ScareType type, Stage stage) {
		return switch (type) {
			case FOOTSTEPS -> FootstepScare.start(player);
			case DOOR -> BlockScares.door(player);
			case CHEST -> BlockScares.chest(player);
			case TORCH -> BlockScares.torch(player);
			case ODD_BLOCK -> BaseMeddling.placeOddBlock(player, true);
			case WATCHER -> MimicDirector.appearFor(player, stage, MimicMode.WATCH) != null;
			case FOLLOWER -> MimicDirector.appearFor(player, stage, MimicMode.FOLLOW) != null;
			case HUNTER -> MimicDirector.appearFor(player, stage, MimicMode.HUNT) != null;
			case FAKE_CHAT -> FakeChat.sendTo(player, true);
			case VOICE -> VoiceScares.speakNow(player);
		};
	}
}
