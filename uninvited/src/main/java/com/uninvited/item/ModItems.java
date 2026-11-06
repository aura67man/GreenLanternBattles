package com.uninvited.item;

import com.uninvited.Uninvited;
import com.uninvited.salt.SaltBlock;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Rarity;

/** Counterplay: salt and the Witness Bell. */
public final class ModItems {
	public static final Block SALT_BLOCK = Registry.register(Registries.BLOCK, Uninvited.id("salt"),
			new SaltBlock(AbstractBlock.Settings.create()
					.mapColor(MapColor.WHITE)
					.noCollision()
					.breakInstantly()
					.nonOpaque()
					.sounds(BlockSoundGroup.SAND)
					.pistonBehavior(PistonBehavior.DESTROY)));

	public static final Item SALT = Registry.register(Registries.ITEM, Uninvited.id("salt"),
			new BlockItem(SALT_BLOCK, new Item.Settings()));

	public static final Item WITNESS_BELL = Registry.register(Registries.ITEM, Uninvited.id("witness_bell"),
			new WitnessBellItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

	private ModItems() {
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(SALT));
		ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(WITNESS_BELL));
	}
}
