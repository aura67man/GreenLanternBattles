package com.uninvited.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.director.VoiceScares;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicMode;
import com.uninvited.mimic.MimicSpawner;
import com.uninvited.scare.ScareType;
import com.uninvited.scare.Scares;
import com.uninvited.stage.Stage;
import com.uninvited.stage.UninvitedState;
import com.uninvited.util.AloneUtil;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.Locale;

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
				.then(literal("summon")
						.then(argument("player", EntityArgumentType.player())
								.executes(ctx -> summon(ctx, null))
								.then(argument("mode", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (MimicMode mode : MimicMode.values()) builder.suggest(mode.name().toLowerCase(Locale.ROOT));
											return builder.buildFuture();
										})
										.executes(ctx -> summon(ctx, StringArgumentType.getString(ctx, "mode"))))))
				.then(literal("scare")
						.then(argument("player", EntityArgumentType.player())
								.executes(ctx -> scare(ctx, null))
								.then(argument("type", StringArgumentType.word())
										.suggests((ctx, builder) -> {
											for (ScareType type : ScareType.values()) builder.suggest(type.name().toLowerCase(Locale.ROOT));
											return builder.buildFuture();
										})
										.executes(ctx -> scare(ctx, StringArgumentType.getString(ctx, "type"))))))
				.then(literal("despawn").executes(ctx -> {
					int count = 0;
					for (ServerWorld world : ctx.getSource().getServer().getWorlds()) {
						for (MimicEntity mimic : MimicSpawner.activeMimics(world)) {
							mimic.vanish();
							count++;
						}
					}
					int removed = count;
					ctx.getSource().sendFeedback(() -> Text.literal("Removed " + removed + " mimic(s)"), true);
					return removed;
				}))
				.then(literal("reload").executes(ctx -> {
					boolean voiceWasOn = UninvitedConfig.get().voiceMimicry;
					UninvitedConfig.load();
					VoiceScares.onConfigReloaded(ctx.getSource().getServer(), voiceWasOn);
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

	private static int summon(CommandContext<ServerCommandSource> ctx, String modeName) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity disguiseAs = EntityArgumentType.getPlayer(ctx, "player");
		MimicMode mode;
		if (modeName == null) {
			mode = MimicDirectorHooks.defaultModeFor(UninvitedState.get(source.getServer()).stage());
		} else {
			try {
				mode = MimicMode.valueOf(modeName.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				source.sendError(Text.literal("Unknown mode " + modeName));
				return 0;
			}
		}

		// The mimic targets whoever ran the command (if a player), otherwise the disguise's owner.
		ServerPlayerEntity target = source.getPlayer() != null ? source.getPlayer() : disguiseAs;
		ServerWorld world = source.getWorld();
		Vec3d look = source.getRotation() == null ? Vec3d.ZERO
				: Vec3d.fromPolar(0, source.getRotation().y);
		Vec3d pos = source.getPosition().add(look.multiply(5));
		float yaw = source.getRotation() == null ? 0 : source.getRotation().y + 180f;

		MimicEntity mimic = MimicSpawner.spawn(world, pos, yaw, MimicSpawner.disguiseOf(disguiseAs), target, mode, 20 * 300);
		if (mimic == null) {
			source.sendError(Text.literal("Could not spawn a mimic"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Summoned a mimic as " + mimic.getFakeName() + " (" + mode.name().toLowerCase(Locale.ROOT)
				+ ", targeting " + target.getGameProfile().getName() + ")"), true);
		return 1;
	}

	private static int scare(CommandContext<ServerCommandSource> ctx, String typeName) throws CommandSyntaxException {
		ServerCommandSource source = ctx.getSource();
		ServerPlayerEntity player = EntityArgumentType.getPlayer(ctx, "player");
		Stage stage = UninvitedState.get(source.getServer()).stage();
		ScareType ran;
		if (typeName == null) {
			ran = Scares.runRandom(player, stage);
		} else {
			ScareType type;
			try {
				type = ScareType.valueOf(typeName.toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException e) {
				source.sendError(Text.literal("Unknown scare " + typeName));
				return 0;
			}
			ran = Scares.run(player, type, stage.atLeast(type.minStage) ? stage : type.minStage) ? type : null;
		}
		if (ran == null) {
			source.sendError(Text.literal("Nothing to do here (no doors/chests/torches nearby, no chat history, or no room to spawn)"));
			return 0;
		}
		source.sendFeedback(() -> Text.literal("Scared " + player.getGameProfile().getName() + ": " + ran.name().toLowerCase(Locale.ROOT)), true);
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
		source.sendFeedback(() -> Text.literal(VoiceScares.statusLine()), false);

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
