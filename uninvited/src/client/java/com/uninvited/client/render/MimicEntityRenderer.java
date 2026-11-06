package com.uninvited.client.render;

import com.uninvited.mimic.MimicEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.SkinTextures;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Entry renderer for the mimic. Vanilla handles slim vs. wide arms with two separate player
 * renderers, so we do the same: each frame we resolve the disguise's skin, read its arm model,
 * and hand off to the matching {@link MimicModelRenderer}.
 */
public final class MimicEntityRenderer extends EntityRenderer<MimicEntity> {
	private final MimicModelRenderer wide;
	private final MimicModelRenderer slim;

	public MimicEntityRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.wide = new MimicModelRenderer(ctx, false);
		this.slim = new MimicModelRenderer(ctx, true);
		this.shadowRadius = 0.5f;
	}

	private MimicModelRenderer pick(MimicEntity mimic) {
		return MimicSkins.resolve(mimic).model() == SkinTextures.Model.SLIM ? slim : wide;
	}

	@Override
	public void render(MimicEntity mimic, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
		pick(mimic).render(mimic, yaw, tickDelta, matrices, vertices, light);
	}

	@Override
	public Identifier getTexture(MimicEntity mimic) {
		return MimicSkins.resolve(mimic).texture();
	}
}
