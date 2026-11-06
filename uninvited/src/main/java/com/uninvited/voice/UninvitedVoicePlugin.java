package com.uninvited.voice;

import com.uninvited.Uninvited;
import com.uninvited.voicebridge.VoiceBridge;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStoppedEvent;

/**
 * Simple Voice Chat plugin entrypoint ({@code "voicechat"} in fabric.mod.json).
 *
 * <p>SVC discovers this class through Fabric's entrypoint system and is the ONLY thing that
 * ever loads it. If SVC isn't installed nobody asks for the "voicechat" entrypoint, this class
 * is never loaded, and neither is anything else in this package. See {@link VoiceBridge}.
 */
public final class UninvitedVoicePlugin implements VoicechatPlugin {
	private volatile SvcVoiceBackend backend;

	@Override
	public String getPluginId() {
		return Uninvited.MOD_ID;
	}

	@Override
	public void initialize(VoicechatApi api) {
		Uninvited.LOG.info("Simple Voice Chat found; voice mimicry is available (it is still off unless the server config enables it)");
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
			backend = new SvcVoiceBackend(event.getVoicechat());
			VoiceBridge.install(backend);
		});
		registration.registerEvent(VoicechatServerStoppedEvent.class, event -> {
			SvcVoiceBackend old = backend;
			backend = null;
			VoiceBridge.install(null);
			if (old != null) old.wipeAll();
		});
		registration.registerEvent(MicrophonePacketEvent.class, event -> {
			SvcVoiceBackend current = backend;
			if (current != null) current.onMicrophone(event);
		});
		registration.registerEvent(PlayerDisconnectedEvent.class, event -> {
			SvcVoiceBackend current = backend;
			if (current != null) current.wipe(event.getPlayerUuid());
		});
	}
}
