package com.uninvited.client.render;

import com.uninvited.mimic.MimicEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.ArmorEntityModel;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Draws a mimic with the vanilla player model, so no custom model or texture is needed. One
 * instance per arm style: {@code slim} uses the 3-pixel "Alex" arms, otherwise the 4-pixel
 * "Steve" arms. {@link MimicEntityRenderer} picks which one to use for each frame.
 */
final class MimicModelRenderer extends LivingEntityRenderer<MimicEntity, PlayerEntityModel<MimicEntity>> {
	MimicModelRenderer(EntityRendererFactory.Context ctx, boolean slim) {
		super(ctx, new PlayerEntityModel<>(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM : EntityModelLayers.PLAYER), slim), 0.5f);
		this.addFeature(new ArmorFeatureRenderer<>(this,
				new ArmorEntityModel<>(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_INNER_ARMOR : EntityModelLayers.PLAYER_INNER_ARMOR)),
				new ArmorEntityModel<>(ctx.getPart(slim ? EntityModelLayers.PLAYER_SLIM_OUTER_ARMOR : EntityModelLayers.PLAYER_OUTER_ARMOR)),
				ctx.getModelManager()));
		this.addFeature(new HeldItemFeatureRenderer<>(this, ctx.getHeldItemRenderer()));
	}

	@Override
	public void render(MimicEntity mimic, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertices, int light) {
		// Revealed: it flickers in and out for its last second.
		if (mimic.isRevealed() && ((mimic.age / 2) % 2 == 0 || mimic.getRandom().nextInt(4) == 0)) return;

		PlayerEntityModel<MimicEntity> model = this.getModel();
		model.setVisible(true);
		model.sneaking = mimic.isInSneakingPose();
		model.rightArmPose = mimic.getMainHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
		model.leftArmPose = mimic.getOffHandStack().isEmpty() ? BipedEntityModel.ArmPose.EMPTY : BipedEntityModel.ArmPose.ITEM;
		super.render(mimic, yaw, tickDelta, matrices, vertices, light);
	}

	@Override
	protected void scale(MimicEntity mimic, MatrixStack matrices, float amount) {
		// Same scale the vanilla player renderer uses.
		matrices.scale(0.9375f, 0.9375f, 0.9375f);
	}

	@Override
	public Identifier getTexture(MimicEntity mimic) {
		return MimicSkins.resolve(mimic).texture();
	}

	/** Show a name tag the way a player's is shown (always, not just when looked at). */
	@Override
	protected boolean hasLabel(MimicEntity mimic) {
		MinecraftClient client = MinecraftClient.getInstance();
		return MinecraftClient.isHudEnabled()
				&& mimic != client.getCameraEntity()
				&& client.player != null
				&& !mimic.isInvisibleTo(client.player)
				&& !mimic.hasPassengers()
				&& !mimic.getFakeName().isEmpty();
	}
}
