package com.uninvited.voice;

import com.uninvited.Uninvited;
import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.scare.PrivateSounds;
import com.uninvited.voicebridge.VoiceBackend;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The real voice implementation: captures microphone audio into {@link VoiceBuffer}s and plays
 * clips back through SVC audio channels that only one player can hear.
 *
 * <p>Privacy, enforced here:
 * <ul>
 *   <li>Nothing is captured unless {@code voiceMimicry} is true in the server config. It is
 *       re-checked on every packet, so switching it off stops capture immediately.</li>
 *   <li>Audio is held in memory only. There is no code path that writes it anywhere.</li>
 *   <li>A player's audio is wiped when they disconnect; everything is wiped when the voice
 *       server or the game server stops, or when voice mimicry is switched off.</li>
 *   <li>Players talking in an SVC group (a private channel) are not captured.</li>
 * </ul>
 */
final class SvcVoiceBackend implements VoiceBackend {
	private final VoicechatServerApi api;
	private final Map<UUID, VoiceBuffer> buffers = new ConcurrentHashMap<>();
	private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();
	/** Listeners currently hearing something, so clips never overlap. */
	private final Set<UUID> busy = ConcurrentHashMap.newKeySet();
	private final Random random = new Random();

	SvcVoiceBackend(VoicechatServerApi api) {
		this.api = api;
	}

	// ---------------------------------------------------------------------------------------
	// Capture (SVC network thread)
	// ---------------------------------------------------------------------------------------

	void onMicrophone(MicrophonePacketEvent event) {
		if (!UninvitedConfig.get().voiceMimicry) return;
		VoicechatConnection sender = event.getSenderConnection();
		if (sender == null || sender.isInGroup()) return;
		UUID uuid = sender.getPlayer().getUuid();
		byte[] data = event.getPacket().getOpusEncodedData();
		VoiceBuffer buffer = buffers.computeIfAbsent(uuid, id -> new VoiceBuffer());

		// An empty packet means the speaker stopped talking (push-to-talk released, etc.).
		if (data == null || data.length == 0) {
			buffer.endOfTransmission();
			OpusDecoder decoder = decoders.get(uuid);
			if (decoder != null) decoder.resetState();
			return;
		}
		// Opus decoders are stateful, so each speaker gets their own. We decode only to
		// measure loudness and then keep the original (compact) Opus bytes.
		OpusDecoder decoder = decoders.computeIfAbsent(uuid, id -> api.createDecoder());
		short[] pcm = decoder.decode(data);
		buffer.accept(data.clone(), VoiceBuffer.rms(pcm));
	}

	// ---------------------------------------------------------------------------------------
	// Playback (server thread)
	// ---------------------------------------------------------------------------------------

	@Override
	public boolean speakFrom(MimicEntity mimic, ServerPlayerEntity listener, UUID voiceOwner) {
		short[] pcm = prepareClip(voiceOwner, listener.getUuid());
		if (pcm == null) return false;
		// The voice comes from the mimic's body: SVC positions and attenuates it like a player.
		EntityAudioChannel channel = api.createEntityAudioChannel(UUID.randomUUID(), api.fromEntity(mimic));
		if (channel == null) return false;
		channel.setDistance(48.0f);
		return play(channel, listener.getUuid(), pcm);
	}

	@Override
	public boolean echoBehind(ServerPlayerEntity listener) {
		short[] pcm = prepareClip(listener.getUuid(), listener.getUuid());
		if (pcm == null) return false;
		Vec3d behind = PrivateSounds.behind(listener, 2.5).add(0, listener.getStandingEyeHeight(), 0);
		LocationalAudioChannel channel = api.createLocationalAudioChannel(UUID.randomUUID(),
				api.fromServerLevel(listener.getServerWorld()), api.createPosition(behind.x, behind.y, behind.z));
		if (channel == null) return false;
		channel.setDistance(16.0f);
		return play(channel, listener.getUuid(), pcm);
	}

	/** Decodes a stored clip with a fresh decoder, then slows/pitches it down slightly. */
	private short[] prepareClip(UUID voiceOwner, UUID listener) {
		if (!UninvitedConfig.get().voiceMimicry || busy.contains(listener)) return null;
		VoiceBuffer buffer = buffers.get(voiceOwner);
		VoiceClip clip = buffer == null ? null : buffer.pick(random);
		if (clip == null) return null;

		OpusDecoder decoder = api.createDecoder();
		short[] pcm;
		try {
			pcm = new short[clip.frameCount() * VoiceBuffer.FRAME_SAMPLES];
			int at = 0;
			for (byte[] frame : clip.frames()) {
				short[] decoded = decoder.decode(frame);
				int n = Math.min(decoded.length, pcm.length - at);
				System.arraycopy(decoded, 0, pcm, at, n);
				at += n;
			}
		} finally {
			decoder.close();
		}
		return VoiceEffects.process(pcm, 0.86 + random.nextDouble() * 0.06, random);
	}

	/** Only {@code listener} hears this channel; SVC drops it for everyone else. */
	private boolean play(AudioChannel channel, UUID listener, short[] pcm) {
		channel.setFilter(player -> player.getUuid().equals(listener));
		OpusEncoder encoder = api.createEncoder();
		AudioPlayer player = api.createAudioPlayer(channel, encoder, pcm);
		busy.add(listener);
		player.setOnStopped(() -> {
			encoder.close();
			busy.remove(listener);
		});
		try {
			player.startPlaying();
		} catch (RuntimeException e) {
			Uninvited.LOG.warn("Voice playback failed", e);
			encoder.close();
			busy.remove(listener);
			return false;
		}
		return true;
	}

	// ---------------------------------------------------------------------------------------
	// Bookkeeping
	// ---------------------------------------------------------------------------------------

	@Override
	public boolean hasVoice(UUID player) {
		VoiceBuffer buffer = buffers.get(player);
		return buffer != null && !buffer.isEmpty();
	}

	@Override
	public int clipCount() {
		int count = 0;
		for (VoiceBuffer buffer : buffers.values()) count += buffer.clipCount();
		return count;
	}

	@Override
	public void wipe(UUID player) {
		VoiceBuffer buffer = buffers.remove(player);
		if (buffer != null) buffer.clear();
		OpusDecoder decoder = decoders.remove(player);
		if (decoder != null) decoder.close();
	}

	@Override
	public void wipeAll() {
		for (UUID player : Set.copyOf(buffers.keySet())) wipe(player);
		for (UUID player : Set.copyOf(decoders.keySet())) wipe(player);
	}
}
