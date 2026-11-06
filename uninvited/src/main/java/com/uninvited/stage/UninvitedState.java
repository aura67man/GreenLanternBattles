package com.uninvited.stage;

import com.uninvited.config.UninvitedConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-saved state ({@code data/uninvited.dat} in the overworld folder).
 *
 * <p>Holds the night counter that drives escalation and a small directory of players who have
 * ever joined (UUID, name, and their public skin property) so the mimic can wear the skin of a
 * friend who is offline. Chat and voice are deliberately NOT stored here; they live in memory.
 */
public final class UninvitedState extends PersistentState {
	private static final String KEY = "uninvited";

	private static final Type<UninvitedState> TYPE = new Type<>(UninvitedState::new, UninvitedState::fromNbt, null);

	/** Has the mod been running in this world yet? Set on first server start with the mod. */
	private boolean activated;
	/** Nights that have begun since activation. Only ever goes up. */
	private int nightsSeen;
	/** The day index whose night was last counted. Used to detect a new night. */
	private long lastCountedNightDay = Long.MIN_VALUE;
	/** 0 = follow the night counter; 1-4 = forced by /uninvited stage. */
	private int forcedStage;

	private final Map<UUID, KnownPlayer> knownPlayers = new LinkedHashMap<>();

	public record KnownPlayer(UUID uuid, String name, @Nullable String skinValue, @Nullable String skinSignature) {
	}

	public static UninvitedState get(MinecraftServer server) {
		return server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, KEY);
	}

	// ---------------------------------------------------------------------------------------
	// Nights and stages
	// ---------------------------------------------------------------------------------------

	/** First tick in this world with the mod installed: start counting from here. */
	public void activateIfNeeded(World overworld) {
		if (activated) return;
		activated = true;
		nightsSeen = 0;
		lastCountedNightDay = currentNightDay(overworld.getTimeOfDay());
		markDirty();
	}

	/**
	 * Called once a second. A night "begins" at 12542 ticks into the day (when beds start to
	 * work), so sleeping early still counts the night. Time jumps (/time set, sleeping) count as
	 * ONE new night at most, so skipping time can't fast-forward several stages. Turning time
	 * backwards just re-anchors; the counter never goes down.
	 *
	 * @return true when a new night started
	 */
	public boolean tickNights(World overworld) {
		long nightDay = currentNightDay(overworld.getTimeOfDay());
		if (nightDay > lastCountedNightDay) {
			lastCountedNightDay = nightDay;
			nightsSeen++;
			markDirty();
			return true;
		}
		if (nightDay < lastCountedNightDay) {
			lastCountedNightDay = nightDay;
			markDirty();
		}
		return false;
	}

	/** The index of the most recent day whose night has begun (may be the previous day). */
	private static long currentNightDay(long timeOfDay) {
		long day = Math.floorDiv(timeOfDay, 24000L);
		long timeInDay = Math.floorMod(timeOfDay, 24000L);
		return timeInDay >= 12542 ? day : day - 1;
	}

	public static boolean isNight(World overworld) {
		long timeInDay = Math.floorMod(overworld.getTimeOfDay(), 24000L);
		return timeInDay >= 12542 && timeInDay < 23460;
	}

	public int nightsSeen() {
		return nightsSeen;
	}

	public Stage naturalStage() {
		if (nightsSeen <= 0) return Stage.DORMANT;
		int perStage = UninvitedConfig.get().nightsPerStage;
		return Stage.byNumber(Math.min(4, 1 + (nightsSeen - 1) / perStage));
	}

	public Stage stage() {
		return forcedStage > 0 ? Stage.byNumber(forcedStage) : naturalStage();
	}

	public boolean isForced() {
		return forcedStage > 0;
	}

	/** @param stage 1-4 to force, 0 to go back to the night counter */
	public void forceStage(int stage) {
		forcedStage = Math.max(0, Math.min(4, stage));
		markDirty();
	}

	// ---------------------------------------------------------------------------------------
	// Known players
	// ---------------------------------------------------------------------------------------

	public void rememberPlayer(KnownPlayer player) {
		KnownPlayer old = knownPlayers.get(player.uuid());
		if (player.equals(old)) return;
		// Keep the old skin if this login didn't carry one (offline-mode servers).
		if (old != null && player.skinValue() == null) {
			player = new KnownPlayer(player.uuid(), player.name(), old.skinValue(), old.skinSignature());
		}
		knownPlayers.put(player.uuid(), player);
		markDirty();
	}

	@Nullable
	public KnownPlayer knownPlayer(UUID uuid) {
		return knownPlayers.get(uuid);
	}

	public Collection<KnownPlayer> knownPlayers() {
		return Collections.unmodifiableCollection(knownPlayers.values());
	}

	// ---------------------------------------------------------------------------------------
	// NBT
	// ---------------------------------------------------------------------------------------

	@Override
	public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		nbt.putBoolean("Activated", activated);
		nbt.putInt("NightsSeen", nightsSeen);
		nbt.putLong("LastCountedNightDay", lastCountedNightDay);
		nbt.putInt("ForcedStage", forcedStage);
		NbtList players = new NbtList();
		for (KnownPlayer p : knownPlayers.values()) {
			NbtCompound tag = new NbtCompound();
			tag.putUuid("Uuid", p.uuid());
			tag.putString("Name", p.name());
			if (p.skinValue() != null) tag.putString("SkinValue", p.skinValue());
			if (p.skinSignature() != null) tag.putString("SkinSignature", p.skinSignature());
			players.add(tag);
		}
		nbt.put("KnownPlayers", players);
		return nbt;
	}

	private static UninvitedState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
		UninvitedState state = new UninvitedState();
		state.activated = nbt.getBoolean("Activated");
		state.nightsSeen = nbt.getInt("NightsSeen");
		state.lastCountedNightDay = nbt.contains("LastCountedNightDay") ? nbt.getLong("LastCountedNightDay") : Long.MIN_VALUE;
		state.forcedStage = nbt.getInt("ForcedStage");
		NbtList players = nbt.getList("KnownPlayers", NbtElement.COMPOUND_TYPE);
		for (int i = 0; i < players.size(); i++) {
			NbtCompound tag = players.getCompound(i);
			if (!tag.containsUuid("Uuid")) continue;
			UUID uuid = tag.getUuid("Uuid");
			state.knownPlayers.put(uuid, new KnownPlayer(uuid, tag.getString("Name"),
					tag.contains("SkinValue") ? tag.getString("SkinValue") : null,
					tag.contains("SkinSignature") ? tag.getString("SkinSignature") : null));
		}
		return state;
	}
}
