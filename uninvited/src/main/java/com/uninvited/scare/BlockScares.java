package com.uninvited.scare;

import com.uninvited.mimic.MimicSenses;
import com.uninvited.util.Scheduler;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.enums.ChestType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** Scares that touch real blocks near a lone player: doors, chests, torches. */
public final class BlockScares {
	private BlockScares() {
	}

	// ---------------------------------------------------------------------------------------
	// Doors and trapdoors
	// ---------------------------------------------------------------------------------------

	/**
	 * A wooden door or trapdoor nearby swings open, then shuts a couple of seconds later.
	 * Prefers one the player isn't looking at. If there isn't one, they just hear a door, slowed
	 * down, somewhere behind them.
	 */
	public static boolean door(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos pos = findNear(player, 16, 6, state ->
				(state.isIn(BlockTags.WOODEN_DOORS) && state.get(DoorBlock.HALF) == DoubleBlockHalf.LOWER)
						|| state.isIn(BlockTags.WOODEN_TRAPDOORS));
		if (pos == null) {
			Vec3d at = PrivateSounds.behind(player, 8.0);
			PrivateSounds.play(player, SoundEvents.BLOCK_WOODEN_DOOR_OPEN, at, 0.9f, 0.7f);
			Scheduler.runLater(30 + player.getRandom().nextInt(20), () ->
					PrivateSounds.play(player, SoundEvents.BLOCK_WOODEN_DOOR_CLOSE, at, 0.9f, 0.65f));
			return true;
		}

		BlockState state = world.getBlockState(pos);
		if (state.getBlock() instanceof DoorBlock door) {
			boolean wasOpen = door.isOpen(state);
			door.setOpen(null, world, state, pos, !wasOpen);
			Scheduler.runLater(35 + player.getRandom().nextInt(30), () -> {
				BlockState now = world.getBlockState(pos);
				if (now.getBlock() instanceof DoorBlock d && d.isOpen(now) != wasOpen) d.setOpen(null, world, now, pos, wasOpen);
			});
		} else {
			toggleTrapdoor(world, pos, player);
			Scheduler.runLater(35 + player.getRandom().nextInt(30), () -> toggleTrapdoor(world, pos, player));
		}
		return true;
	}

	private static void toggleTrapdoor(ServerWorld world, BlockPos pos, ServerPlayerEntity player) {
		BlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof TrapdoorBlock)) return;
		boolean open = !state.get(TrapdoorBlock.OPEN);
		world.setBlockState(pos, state.with(TrapdoorBlock.OPEN, open), Block.NOTIFY_LISTENERS);
		PrivateSounds.play(player, open ? SoundEvents.BLOCK_WOODEN_TRAPDOOR_OPEN : SoundEvents.BLOCK_WOODEN_TRAPDOOR_CLOSE,
				Vec3d.ofCenter(pos), 1.0f, 0.75f);
	}

	// ---------------------------------------------------------------------------------------
	// Chests
	// ---------------------------------------------------------------------------------------

	/**
	 * A chest in their base opens and closes, lid and all, with nobody there. The lid uses the
	 * chest's own "viewer count" block event, so the contents are never touched.
	 */
	public static boolean chest(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		BlockPos home = player.getSpawnPointPosition();
		boolean homeNearby = home != null && player.getSpawnPointDimension() == world.getRegistryKey()
				&& home.isWithinDistance(player.getBlockPos(), 48);
		BlockPos center = homeNearby ? home : player.getBlockPos();
		Predicate<BlockState> isContainer = state -> state.getBlock() instanceof ChestBlock || state.isOf(Blocks.BARREL);
		BlockPos pos = findAround(player, center, homeNearby ? 20 : 16, 6, isContainer);
		if (pos == null) return false;

		BlockState state = world.getBlockState(pos);
		Vec3d at = Vec3d.ofCenter(pos);
		if (state.isOf(Blocks.BARREL)) {
			world.setBlockState(pos, state.with(BarrelBlock.OPEN, true), Block.NOTIFY_LISTENERS);
			PrivateSounds.play(player, SoundEvents.BLOCK_BARREL_OPEN, at, 0.8f, 0.8f);
			Scheduler.runLater(40 + player.getRandom().nextInt(40), () -> {
				BlockState now = world.getBlockState(pos);
				if (now.isOf(Blocks.BARREL)) world.setBlockState(pos, now.with(BarrelBlock.OPEN, false), Block.NOTIFY_LISTENERS);
				PrivateSounds.play(player, SoundEvents.BLOCK_BARREL_CLOSE, at, 0.8f, 0.75f);
			});
			return true;
		}

		setChestLid(world, pos, state, 1);
		PrivateSounds.play(player, SoundEvents.BLOCK_CHEST_OPEN, at, 0.8f, 0.8f);
		Scheduler.runLater(40 + player.getRandom().nextInt(40), () -> {
			BlockState now = world.getBlockState(pos);
			if (now.getBlock() instanceof ChestBlock) setChestLid(world, pos, now, 0);
			PrivateSounds.play(player, SoundEvents.BLOCK_CHEST_CLOSE, at, 0.8f, 0.75f);
		});
		return true;
	}

	/** Block event type 1 = "number of players looking inside"; clients animate the lid from it. */
	private static void setChestLid(ServerWorld world, BlockPos pos, BlockState state, int viewers) {
		world.addSyncedBlockEvent(pos, state.getBlock(), 1, viewers);
		if (state.get(ChestBlock.CHEST_TYPE) != ChestType.SINGLE) {
			BlockPos other = pos.offset(ChestBlock.getFacing(state));
			world.addSyncedBlockEvent(other, world.getBlockState(other).getBlock(), 1, viewers);
		}
	}

	// ---------------------------------------------------------------------------------------
	// Torches
	// ---------------------------------------------------------------------------------------

	/**
	 * A nearby torch the player isn't looking at comes off the wall. It's broken normally, so it
	 * drops as an item: nothing is deleted, and the player can find it on the floor.
	 */
	public static boolean torch(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		List<BlockPos> torches = findAll(player, player.getBlockPos(), 12, 5, state ->
				state.isOf(Blocks.TORCH) || state.isOf(Blocks.WALL_TORCH)
						|| state.isOf(Blocks.SOUL_TORCH) || state.isOf(Blocks.SOUL_WALL_TORCH));
		if (torches.isEmpty()) return false;
		BlockPos chosen = null;
		for (BlockPos pos : torches) {
			if (lookDot(player, pos) < 0.2) {
				chosen = pos;
				break;
			}
		}
		if (chosen == null) chosen = torches.get(0);
		world.breakBlock(chosen, true, null, 512);
		return true;
	}

	// ---------------------------------------------------------------------------------------
	// Searching
	// ---------------------------------------------------------------------------------------

	private static double lookDot(ServerPlayerEntity player, BlockPos pos) {
		Vec3d to = Vec3d.ofCenter(pos).subtract(player.getEyePos()).normalize();
		return player.getRotationVec(1.0f).dotProduct(to);
	}

	/** The nearest matching block, preferring ones outside the player's view. */
	@Nullable
	private static BlockPos findNear(ServerPlayerEntity player, int radius, int height, Predicate<BlockState> match) {
		return findAround(player, player.getBlockPos(), radius, height, match);
	}

	@Nullable
	private static BlockPos findAround(ServerPlayerEntity player, BlockPos center, int radius, int height, Predicate<BlockState> match) {
		List<BlockPos> all = findAll(player, center, radius, height, match);
		for (BlockPos pos : all) {
			if (lookDot(player, pos) < MimicSenses.IN_VIEW) return pos;
		}
		return all.isEmpty() ? null : all.get(0);
	}

	/** Matching blocks, nearest first. */
	private static List<BlockPos> findAll(ServerPlayerEntity player, BlockPos center, int radius, int height, Predicate<BlockState> match) {
		ServerWorld world = player.getServerWorld();
		List<BlockPos> found = new ArrayList<>();
		for (BlockPos pos : BlockPos.iterateOutwards(center, radius, height, radius)) {
			if (match.test(world.getBlockState(pos))) {
				found.add(pos.toImmutable());
				if (found.size() >= 16) break;
			}
		}
		return found;
	}
}
