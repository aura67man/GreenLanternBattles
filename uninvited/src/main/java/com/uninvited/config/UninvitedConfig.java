package com.uninvited.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.uninvited.Uninvited;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Server config, stored as {@code config/uninvited.json}.
 *
 * <p>Fields are public and read directly ({@code UninvitedConfig.get().aloneRadius}). Unknown or
 * missing fields fall back to the defaults below, and the file is rewritten on load so new
 * options show up for operators after an update. Edit it while the server is stopped, or use
 * {@code /uninvited reload}.
 */
public final class UninvitedConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static UninvitedConfig instance = new UninvitedConfig();

	// --- The "alone" rule -------------------------------------------------------------------
	/** A player is alone when no other non-spectator player is within this many blocks (same dimension). */
	public double aloneRadius = 24.0;

	// --- Escalation -------------------------------------------------------------------------
	/** How many in-game nights each stage lasts. 2 gives: nights 1-2, 3-4, 5-6, 7+. */
	public int nightsPerStage = 2;
	/** Multiplier for event chances during the day (0 = nothing happens in daylight). */
	public double daytimeActivity = 0.25;

	// --- Spawning ---------------------------------------------------------------------------
	/** Chance per minute, per lone player at night, that a mimic appears (stage 2+). */
	public double spawnChancePerMinute = 0.6;
	/** Chance per minute, per lone player at night, of a sound scare (stage 1+). */
	public double scareChancePerMinute = 1.0;
	/** Minimum seconds between two mimic appearances in the same world. */
	public int secondsBetweenAppearances = 90;
	/** Maximum mimics alive at the same time in one world. */
	public int maxMimics = 1;

	// --- The mimic --------------------------------------------------------------------------
	/** Damage per hit during the hunting stage (2.0 = one heart). */
	public double mimicDamage = 5.0;
	/** Ticks between two mimic hits. */
	public int mimicAttackCooldownTicks = 30;
	public double mimicHealth = 30.0;
	/** The mimic gives up and vanishes after losing this fraction of its health. */
	public double mimicFleeHealthFraction = 0.5;

	// --- Fake chat --------------------------------------------------------------------------
	public boolean fakeChat = true;
	/** Chance per minute, per lone player, of a fake chat line (stage 3+). */
	public double fakeChatChancePerMinute = 0.35;

	// --- Witness Bell -----------------------------------------------------------------------
	public int bellCooldownSeconds = 300;
	public double bellRadius = 48.0;
	public int bellGlowSeconds = 5;

	// --- Voice (Simple Voice Chat, optional) ------------------------------------------------
	/**
	 * OFF by default. Only a server operator can turn this on (by editing this file). When it is
	 * on, every player is told on join. Audio is kept in memory only and never written to disk.
	 */
	public boolean voiceMimicry = false;
	/** Chance per minute, per lone player, that the mimic plays a voice clip (when voiceMimicry is on). */
	public double voiceChancePerMinute = 0.5;

	public static UninvitedConfig get() {
		return instance;
	}

	public static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("uninvited.json");
	}

	public static void load() {
		Path path = path();
		UninvitedConfig loaded = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, UninvitedConfig.class);
			} catch (IOException | JsonParseException e) {
				Uninvited.LOG.error("Could not read {}; using defaults", path, e);
			}
		}
		instance = loaded == null ? new UninvitedConfig() : loaded;
		instance.sanitize();
		save();
	}

	public static void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			Uninvited.LOG.error("Could not write {}", path, e);
		}
	}

	private void sanitize() {
		aloneRadius = Math.max(1.0, aloneRadius);
		nightsPerStage = Math.max(1, nightsPerStage);
		daytimeActivity = clamp01(daytimeActivity);
		spawnChancePerMinute = Math.max(0, spawnChancePerMinute);
		scareChancePerMinute = Math.max(0, scareChancePerMinute);
		fakeChatChancePerMinute = Math.max(0, fakeChatChancePerMinute);
		voiceChancePerMinute = Math.max(0, voiceChancePerMinute);
		secondsBetweenAppearances = Math.max(0, secondsBetweenAppearances);
		maxMimics = Math.max(0, maxMimics);
		mimicDamage = Math.max(0, mimicDamage);
		mimicAttackCooldownTicks = Math.max(1, mimicAttackCooldownTicks);
		mimicHealth = Math.max(1, mimicHealth);
		mimicFleeHealthFraction = clamp01(mimicFleeHealthFraction);
		bellCooldownSeconds = Math.max(0, bellCooldownSeconds);
		bellRadius = Math.max(1, bellRadius);
		bellGlowSeconds = Math.max(1, bellGlowSeconds);
	}

	private static double clamp01(double v) {
		return Math.max(0, Math.min(1, v));
	}
}
