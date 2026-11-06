package com.uninvited.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.stage.Stage;
import com.uninvited.stage.UninvitedState;
import com.uninvited.util.AloneUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

/** Operator-only debug commands under {@code /uninvited}. */
public final class UninvitedCommands {
	private UninvitedCommands() {
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(literal("uninvited")
				.requires(source -> source.hasPermissionLevel(2))
				.then(literal("stage")
						.then(argument("stage", IntegerArgumentType.integer(1, 4))
								.executes(ctx -> forceStage(ctx, IntegerArgumentType.getInteger(ctx, "stage"))))
						.then(literal("auto").executes(ctx -> forceStage(ctx, 0))))
				.then(literal("status").executes(UninvitedCommands::status))
				.then(literal("reload").executes(ctx -> {
					UninvitedConfig.load();
					ctx.getSource().sendFeedback(() -> Text.literal("Reloaded " + UninvitedConfig.path().getFileName()), true);
					return 1;
				})));
	}

	private static int forceStage(CommandContext<ServerCommandSource> ctx, int stage) {
		UninvitedState state = UninvitedState.get(ctx.getSource().getServer());
		state.forceStage(stage);
		Stage now = state.stage();
		ctx.getSource().sendFeedback(() -> stage == 0
				? Text.literal("Stage now follows the night counter: " + now.number + " (" + now.title + ")")
				: Text.literal("Forced stage " + now.number + " (" + now.title + ")"), true);
		return 1;
	}

	private static int status(CommandContext<ServerCommandSource> ctx) {
		MinecraftServer server = ctx.getSource().getServer();
		UninvitedState state = UninvitedState.get(server);
		Stage stage = state.stage();
		ServerCommandSource source = ctx.getSource();

		source.sendFeedback(() -> Text.literal("UNINVITED").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
		source.sendFeedback(() -> Text.literal("Stage " + stage.number + ": " + stage.title
				+ (state.isForced() ? " (forced)" : "") + ", nights seen: " + state.nightsSeen()
				+ (UninvitedState.isNight(server.getOverworld()) ? ", it is night" : ", it is day")), false);
		StatusExtras.appendMimics(server, source);

		MutableText alone = Text.literal("Alone: ");
		MutableText grouped = Text.literal("In a group: ");
		int aloneCount = 0;
		int groupCount = 0;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!AloneUtil.canBeTargeted(player)) continue;
			if (AloneUtil.isAlone(player)) {
				alone.append((aloneCount++ > 0 ? ", " : "") + player.getGameProfile().getName());
			} else {
				grouped.append((groupCount++ > 0 ? ", " : "") + player.getGameProfile().getName());
			}
		}
		if (aloneCount == 0) alone.append("nobody");
		if (groupCount == 0) grouped.append("nobody");
		source.sendFeedback(() -> alone.formatted(Formatting.RED), false);
		source.sendFeedback(() -> grouped.formatted(Formatting.GREEN), false);
		return 1;
	}
}
