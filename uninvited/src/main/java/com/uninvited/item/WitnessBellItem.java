package com.uninvited.item;

import com.uninvited.config.UninvitedConfig;
import com.uninvited.mimic.MimicEntity;
import com.uninvited.mimic.MimicSpawner;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/**
 * Ring it and every REAL player within 48 blocks glows for a few seconds. Anything that looks
 * like a player but isn't glowing is not your friend; and if the mimic is in range it can't hold
 * its shape: it flickers and is gone. Long cooldown, shown on the hotbar like an ender pearl's.
 */
public class WitnessBellItem extends Item {
	public WitnessBellItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		UninvitedConfig config = UninvitedConfig.get();
		user.getItemCooldownManager().set(this, config.bellCooldownSeconds * 20);
		user.swingHand(hand);
		if (!(world instanceof ServerWorld serverWorld)) return TypedActionResult.success(stack, true);

		serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.6f, 1.5f);
		serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 0.8f, 1.2f);

		double radiusSq = config.bellRadius * config.bellRadius;
		for (ServerPlayerEntity player : serverWorld.getPlayers()) {
			if (player.isSpectator() || player.squaredDistanceTo(user) > radiusSq) continue;
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, config.bellGlowSeconds * 20, 0, false, false, true));
		}
		for (MimicEntity mimic : MimicSpawner.activeMimics(serverWorld)) {
			if (mimic.squaredDistanceTo(user) <= radiusSq) mimic.reveal();
		}
		return TypedActionResult.success(stack, false);
	}

	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
		tooltip.add(Text.translatable("item.uninvited.witness_bell.tooltip").formatted(Formatting.GRAY));
	}
}
