package com.uninvited.chat;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The last ~50 chat lines of each player, kept so the mimic can say things they actually said.
 *
 * <p>Memory only: never written to disk, gone when the server stops. Lines stay after the player
 * logs out (on purpose: a message "from" a friend who left is exactly the point).
 */
public final class ChatMemory {
	private static final int MAX_LINES = 50;
	private static final Map<UUID, Deque<String>> LINES = new HashMap<>();

	private ChatMemory() {
	}

	public static void register() {
		ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
			String text = message.getSignedContent();
			if (text == null || text.isBlank() || text.startsWith("/")) return;
			Deque<String> lines = LINES.computeIfAbsent(sender.getUuid(), id -> new ArrayDeque<>());
			lines.addLast(text.strip());
			while (lines.size() > MAX_LINES) lines.removeFirst();
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LINES.clear());
	}

	public static List<String> linesOf(UUID player) {
		Deque<String> lines = LINES.get(player);
		return lines == null ? List.of() : new ArrayList<>(lines);
	}

	public static boolean hasLines(UUID player) {
		Deque<String> lines = LINES.get(player);
		return lines != null && !lines.isEmpty();
	}
}
