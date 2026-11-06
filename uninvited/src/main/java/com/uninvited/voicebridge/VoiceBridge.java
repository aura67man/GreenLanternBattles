package com.uninvited.voicebridge;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;

/**
 * The one door between UNINVITED and Simple Voice Chat.
 *
 * <p>Loading rules that keep SVC optional:
 * <ol>
 *   <li>SVC is a {@code compileOnly} dependency: its classes are not in our jar.</li>
 *   <li>Everything that names an SVC type lives in {@code com.uninvited.voice}. Nothing outside
 *       that package imports from it, so the JVM never tries to load those classes (and never
 *       fails with NoClassDefFoundError) unless SVC itself calls into them.</li>
 *   <li>The entry into that package is the {@code "voicechat"} entrypoint in fabric.mod.json.
 *       Fabric does not touch entrypoints nobody asks for; only SVC asks for "voicechat", so
 *       without SVC our plugin class is never even loaded.</li>
 *   <li>Once SVC's voice server starts, the plugin installs a {@link VoiceBackend} here. The
 *       rest of the mod only ever sees this interface, and checks
 *       {@link FabricLoader#isModLoaded} as a belt-and-braces guard.</li>
 * </ol>
 * With SVC missing (or voice mimicry off) the backend is simply null and the mimic is silent.
 */
public final class VoiceBridge {
	@Nullable
	private static volatile VoiceBackend backend;

	private VoiceBridge() {
	}

	public static boolean isSvcInstalled() {
		return FabricLoader.getInstance().isModLoaded("voicechat");
	}

	@Nullable
	public static VoiceBackend backend() {
		return isSvcInstalled() ? backend : null;
	}

	public static void install(@Nullable VoiceBackend newBackend) {
		backend = newBackend;
	}
}
