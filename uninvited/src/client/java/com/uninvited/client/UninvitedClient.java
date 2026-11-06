package com.uninvited.client;

import com.uninvited.client.render.MimicEntityRenderer;
import com.uninvited.client.render.MimicSkins;
import com.uninvited.item.ModItems;
import com.uninvited.mimic.MimicEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.RenderLayer;

public final class UninvitedClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(MimicEntities.MIMIC, MimicEntityRenderer::new);
		// Salt is a flat layer of grains with transparent gaps between them.
		BlockRenderLayerMap.INSTANCE.putBlock(ModItems.SALT_BLOCK, RenderLayer.getCutout());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> MimicSkins.clear());
	}
}
