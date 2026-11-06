package com.uninvited.client.render;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.uninvited.mimic.MimicEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.SkinTextures;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Works out which skin a mimic should wear on this client.
 *
 * <p>Resolution order (first one that works wins):
 * <ol>
 *   <li><b>Tab list.</b> If the impersonated player is online, their {@link PlayerListEntry}
 *       already has their {@link SkinTextures}: the exact texture and arm model (slim/wide) the
 *       client uses to draw the real player. {@code getSkinTextures()} starts the download the
 *       first time and returns the default skin until it has arrived, so calling it every frame
 *       is fine; vanilla does the same.</li>
 *   <li><b>Server-supplied profile.</b> The server remembers each player's public, Mojang-signed
 *       "textures" property and syncs it on the mimic. We wrap it in a {@link GameProfile} and
 *       ask the {@code PlayerSkinProvider}, which downloads and caches it the same way it would
 *       for a real player. This is what makes offline friends impersonable.</li>
 *   <li><b>Seen earlier this session.</b> A skin we resolved from the tab list before the player
 *       logged off.</li>
 *   <li><b>Default skin.</b> Steve/Alex/etc. picked from the UUID, exactly like vanilla does for
 *       a player without a skin (offline-mode servers, dev environments).</li>
 * </ol>
 */
public final class MimicSkins {
	private static final Map<UUID, SkinTextures> LAST_KNOWN = new HashMap<>();
	private static final Map<UUID, GameProfile> PROFILES = new HashMap<>();

	private MimicSkins() {
	}

	public static SkinTextures resolve(MimicEntity mimic) {
		Optional<UUID> disguise = mimic.getDisguiseUuid();
		if (disguise.isEmpty()) return DefaultSkinHelper.getSkinTextures(mimic.getUuid());
		UUID uuid = disguise.get();
		MinecraftClient client = MinecraftClient.getInstance();

		// 1. Online: use exactly what the tab list uses.
		ClientPlayNetworkHandler network = client.getNetworkHandler();
		PlayerListEntry entry = network == null ? null : network.getPlayerListEntry(uuid);
		if (entry != null) {
			SkinTextures skin = entry.getSkinTextures();
			// Only remember real downloaded skins; default skins have no URL.
			if (skin.textureUrl() != null) LAST_KNOWN.put(uuid, skin);
			return skin;
		}

		// 2. Offline: the server sent us their signed textures property.
		String value = mimic.getSkinValue();
		if (!value.isEmpty()) {
			GameProfile profile = PROFILES.get(uuid);
			if (profile == null || !hasTextures(profile, value)) {
				profile = new GameProfile(uuid, mimic.getDisguiseName());
				String signature = mimic.getSkinSignature();
				profile.getProperties().put("textures", signature.isEmpty()
						? new Property("textures", value)
						: new Property("textures", value, signature));
				PROFILES.put(uuid, profile);
			}
			SkinTextures skin = client.getSkinProvider().getSkinTextures(profile);
			if (skin.textureUrl() != null) return skin;
		}

		// 3. Seen earlier this session.
		SkinTextures remembered = LAST_KNOWN.get(uuid);
		if (remembered != null) return remembered;

		// 4. Fallback.
		return DefaultSkinHelper.getSkinTextures(uuid);
	}

	private static boolean hasTextures(GameProfile profile, String value) {
		for (Property property : profile.getProperties().get("textures")) {
			if (property.value().equals(value)) return true;
		}
		return false;
	}

	/** Forget everything when leaving a server. */
	public static void clear() {
		LAST_KNOWN.clear();
		PROFILES.clear();
	}
}
