package com.uninvited.salt;

import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathContext;
import net.minecraft.entity.ai.pathing.PathNodeType;
import net.minecraft.util.math.BlockPos;

/**
 * Pathfinding exclusion for salt.
 *
 * <p>How vanilla land pathfinding classifies a candidate node: for the node at (x, y, z), where
 * y is the FEET level, {@code LandPathNodeMaker.getNodeType(context, x, y, z, mob)} calls
 * {@code getCollidingNodeTypes}, which calls {@link #getDefaultNodeType} once for every block
 * the mob's bounding box would occupy (x..x+width, y..y+height, z..z+width). The worst type
 * wins, and {@link PathNodeType#BLOCKED} has a negative penalty, which means "never use".
 *
 * <p>Salt is a thin floor layer (like redstone dust), so it occupies the FEET block of anything
 * standing on it. Returning BLOCKED for any block that contains salt therefore:
 * <ul>
 *   <li>stops the mimic standing on salt (its feet block would be salt), and</li>
 *   <li>stops it walking across a salt line (each crossing node has salt at feet level).</li>
 * </ul>
 * Because the A* search never expands these nodes, a closed ring of salt has no path into it
 * at all: the navigation reports the target as unreachable rather than walking into the line
 * and being pushed back. Collision is left alone on purpose; players and other mobs walk over
 * salt normally.
 *
 * <p>Diagonal moves are covered too: {@code isValidDiagonalSuccessor} rejects a diagonal whose
 * side neighbours are BLOCKED, so the mimic can't squeeze through the corner of a ring.
 */
public class SaltAvoidingNodeMaker extends LandPathNodeMaker {
	private final BlockPos.Mutable scratch = new BlockPos.Mutable();

	@Override
	public PathNodeType getDefaultNodeType(PathContext context, int x, int y, int z) {
		if (SaltRules.isSalt(context.getBlockState(scratch.set(x, y, z)))) {
			return PathNodeType.BLOCKED;
		}
		return super.getDefaultNodeType(context, x, y, z);
	}
}
