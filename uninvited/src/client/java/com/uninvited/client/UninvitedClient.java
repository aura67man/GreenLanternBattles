package com.uninvited.client;

import com.uninvited.client.render.MimicEntityRenderer;
import com.uninvited.client.render.MimicSkins;
import com.uninvited.mimic.MimicEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class UninvitedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(MimicEntities.MIMIC, MimicEntityRenderer::new);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MimicSkins.clear());
	}
}
