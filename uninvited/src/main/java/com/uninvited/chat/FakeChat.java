package com.uninvited.chat;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicSpawner;
import com.uninvited.stage.UninvitedState;
import net.minecraft.entity.EntityType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Stage 3+: a lone player gets a chat line "from" a friend, a line that friend really typed
 * earlier, picked for being short and ominous out of context ("wait", "come here", "found it").
 *
 * <p>Signed player chat can't be forged, so this is a SYSTEM message built to look exactly like
 * vanilla chat: the {@code chat.type.text} translation ("<%s> %s") with the friend's name
 * carrying the same hover card and click-to-/tell that a real chat name has. Only the target
 * receives it.
 */
public final class FakeChat {
	private static final String[] LURE_WORDS = {
			"here", "come", "found", "help", "wait", "look", "over", "quick", "hurry", "follow", "where",
			"behind", "this way", "stop", "hello", "hey", "back", "door", "outside", "down"
	};

	private FakeChat() {
	}

	/** @return true if a message was sent */
	public static boolean sendTo(ServerPlayerEntity target, boolean allowOwnLines) {
		MinecraftServer server = target.getServer();
		Random random = target.getRandom();
		UninvitedState.KnownPlayer friend = pickFriend(target, allowOwnLines, random);
		if (friend == null) return false;
		String line = pickLine(ChatMemory.linesOf(friend.uuid()), random);
		if (line == null) return false;

		ServerPlayerEntity online = server.getPlayerManager().getPlayer(friend.uuid());
		Text name = online != null ? online.getDisplayName() : offlineName(friend.uuid(), friend.name());
		target.sendMessage(Text.translatable("chat.type.text", name, Text.literal(line)), false);
		return true;
	}

	/**
	 * Prefer the friend the mimic is currently wearing, so the message and the figure agree.
	 * Otherwise any other player with chat history.
	 */
	@Nullable
	private static UninvitedState.KnownPlayer pickFriend(ServerPlayerEntity target, boolean allowOwnLines, Random random) {
		UninvitedState state = UninvitedState.get(target.getServer());
		for (MimicEntity mimic : MimicSpawner.activeMimics(target.getServerWorld())) {
			if (target.getUuid().equals(mimic.getTargetUuid()) && mimic.getDisguiseUuid().isPresent()) {
				UUID disguise = mimic.getDisguiseUuid().get();
				if (!disguise.equals(target.getUuid()) && ChatMemory.hasLines(disguise)) {
					UninvitedState.KnownPlayer known = state.knownPlayer(disguise);
					if (known != null) return known;
				}
			}
		}
		List<UninvitedState.KnownPlayer> candidates = new ArrayList<>();
		for (UninvitedState.KnownPlayer known : state.knownPlayers()) {
			if (!known.uuid().equals(target.getUuid()) && ChatMemory.hasLines(known.uuid())) candidates.add(known);
		}
		if (candidates.isEmpty() && allowOwnLines && ChatMemory.hasLines(target.getUuid())) {
			// Solo worlds / debugging: your own words, coming back at you.
			UninvitedState.KnownPlayer self = state.knownPlayer(target.getUuid());
			return self != null ? self : new UninvitedState.KnownPlayer(target.getUuid(), target.getGameProfile().getName(), null, null);
		}
		return candidates.isEmpty() ? null : candidates.get(random.nextInt(candidates.size()));
	}

	/** Weighted pick: short lines and lines with lure words are much more likely. */
	@Nullable
	static String pickLine(List<String> lines, Random random) {
		if (lines.isEmpty()) return null;
		double[] weights = new double[lines.size()];
		double total = 0;
		for (int i = 0; i < lines.size(); i++) {
			String line = lines.get(i);
			String lower = line.toLowerCase(Locale.ROOT);
			double w = 1.0;
			if (line.length() <= 24) w += 2.0;
			else if (line.length() <= 40) w += 1.0;
			else w *= 0.3;
			for (String word : LURE_WORDS) {
				if (lower.contains(word)) w += 3.0;
			}
			if (lower.contains("http://") || lower.contains("https://")) w = 0;
			weights[i] = w;
			total += w;
		}
		if (total <= 0) return null;
		double roll = random.nextDouble() * total;
		for (int i = 0; i < lines.size(); i++) {
			roll -= weights[i];
			if (roll <= 0) return lines.get(i);
		}
		return lines.get(lines.size() - 1);
	}

	/** What vanilla's PlayerEntity#getDisplayName produces, for a player who isn't online. */
	private static Text offlineName(UUID uuid, String name) {
		MutableText text = Text.literal(name);
		return text.styled(style -> style
				.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tell " + name + " "))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ENTITY,
						new HoverEvent.EntityContent(EntityType.PLAYER, uuid, Text.literal(name))))
				.withInsertion(name));
	}
}
