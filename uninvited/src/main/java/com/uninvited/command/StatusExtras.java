package com.uninvited.command;

import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicSpawner;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Locale;

/** The "active mimic" lines of /uninvited status. */
final class StatusExtras {
	private StatusExtras() {
	}

	static void appendMimics(MinecraftServer server, ServerCommandSource source) {
		int count = 0;
		for (ServerWorld world : server.getWorlds()) {
			for (MimicEntity mimic : MimicSpawner.activeMimics(world)) {
				count++;
				ServerPlayerEntity target = mimic.getTargetPlayer();
				String line = String.format(Locale.ROOT, "Mimic as %s (shows \"%s\"), %s, target %s, at %d %d %d in %s",
						mimic.getDisguiseName(), mimic.getFakeName(), mimic.getMode().name().toLowerCase(Locale.ROOT),
						target == null ? "none" : target.getGameProfile().getName(),
						mimic.getBlockX(), mimic.getBlockY(), mimic.getBlockZ(), world.getRegistryKey().getValue());
				source.sendFeedback(() -> Text.literal(line), false);
			}
		}
		if (count == 0) source.sendFeedback(() -> Text.literal("Active mimics: none"), false);
	}
}
