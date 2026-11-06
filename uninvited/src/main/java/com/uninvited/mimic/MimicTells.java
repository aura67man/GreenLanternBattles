package com.uninvited.mimic;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The visual tells. A mimic copies a player, but never quite right:
 * <ul>
 *   <li>it holds the wrong item, or nothing;</li>
 *   <li>its armour is the real player's armour with exactly one piece wrong
 *       (missing, or a piece the real player isn't wearing).</li>
 * </ul>
 * If the real player is offline we can't compare, so it gets a plausible random outfit.
 */
public final class MimicTells {
	/** Things a person might plausibly be holding. {@code null} means an empty hand. */
	private static final Item[] HELD = {
			null, Items.STICK, Items.TORCH, Items.ROTTEN_FLESH, Items.BONE, Items.POPPY,
			Items.WHEAT_SEEDS, Items.DIRT, Items.STONE_PICKAXE, Items.BREAD, Items.COMPASS,
			Items.WOODEN_SWORD, Items.OAK_SAPLING, Items.STRING
	};

	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private static final Item[][] ARMOR_BY_SLOT = {
			{Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.IRON_HELMET, Items.GOLDEN_HELMET},
			{Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.IRON_CHESTPLATE, Items.GOLDEN_CHESTPLATE},
			{Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.IRON_LEGGINGS, Items.GOLDEN_LEGGINGS},
			{Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.IRON_BOOTS, Items.GOLDEN_BOOTS},
	};

	private MimicTells() {
	}

	public static void apply(MimicEntity mimic, @Nullable ServerPlayerEntity real, long seed) {
		Random random = new Random(seed * 31 + 7);
		applyHeldItem(mimic, real, random);
		applyArmor(mimic, real, random);
		mimic.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
	}

	private static void applyHeldItem(MimicEntity mimic, @Nullable ServerPlayerEntity real, Random random) {
		Item realItem = real == null || real.getMainHandStack().isEmpty() ? null : real.getMainHandStack().getItem();
		List<Item> options = new ArrayList<>();
		for (Item item : HELD) {
			if (item != realItem) options.add(item);
		}
		Item chosen = options.get(random.nextInt(options.size()));
		mimic.equipStack(EquipmentSlot.MAINHAND, chosen == null ? ItemStack.EMPTY : new ItemStack(chosen));
	}

	private static void applyArmor(MimicEntity mimic, @Nullable ServerPlayerEntity real, Random random) {
		ItemStack[] outfit = new ItemStack[ARMOR_SLOTS.length];
		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			if (real != null) {
				outfit[i] = real.getEquippedStack(ARMOR_SLOTS[i]).copy();
			} else {
				outfit[i] = random.nextFloat() < 0.3f ? randomPiece(i, random) : ItemStack.EMPTY;
			}
		}

		// Exactly one piece is wrong.
		int wrong = random.nextInt(ARMOR_SLOTS.length);
		if (outfit[wrong].isEmpty()) {
			outfit[wrong] = randomPiece(wrong, random);
		} else if (random.nextBoolean()) {
			outfit[wrong] = ItemStack.EMPTY;
		} else {
			Item current = outfit[wrong].getItem();
			ItemStack replacement;
			do {
				replacement = randomPiece(wrong, random);
			} while (replacement.getItem() == current);
			outfit[wrong] = replacement;
		}

		for (int i = 0; i < ARMOR_SLOTS.length; i++) {
			mimic.equipStack(ARMOR_SLOTS[i], outfit[i]);
		}
	}

	private static ItemStack randomPiece(int slotIndex, Random random) {
		Item[] options = ARMOR_BY_SLOT[slotIndex];
		return new ItemStack(options[random.nextInt(options.length)]);
	}
}
