package com.uninvited.voicebridge;

import com.uninvited.mimic.MimicEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * What the rest of the mod can ask of the voice integration. Deliberately contains NO Simple
 * Voice Chat types, so the core mod can call it without ever loading an SVC class. The only
 * implementation lives in {@code com.uninvited.voice} and is installed by SVC's own plugin
 * entrypoint, which SVC only calls when it is installed.
 */
public interface VoiceBackend {
	/** Plays a short clip of {@code voiceOwner}'s voice from the mimic, heard only by {@code listener}. */
	boolean speakFrom(MimicEntity mimic, ServerPlayerEntity listener, UUID voiceOwner);

	/** Plays a short clip of the listener's OWN voice, from just behind them, heard only by them. */
	boolean echoBehind(ServerPlayerEntity listener);

	boolean hasVoice(UUID player);

	/** How many clips are held, for /uninvited status. */
	int clipCount();

	/** Deletes everything held for one player (on logout). */
	void wipe(UUID player);

	/** Deletes everything (server stop, or voice mimicry switched off). */
	void wipeAll();
}
