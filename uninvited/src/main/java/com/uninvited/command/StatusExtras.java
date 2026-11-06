package com.uninvited.command;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/** Extra /uninvited status lines from later features (filled in by the mimic milestone). */
final class StatusExtras {
	private StatusExtras() {
	}

	static void appendMimics(MinecraftServer server, ServerCommandSource source) {
		source.sendFeedback(() -> Text.literal("Active mimics: none"), false);
	}
}
