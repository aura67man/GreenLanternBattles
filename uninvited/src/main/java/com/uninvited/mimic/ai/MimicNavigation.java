package com.uninvited.mimic.ai;

import com.uninvited.salt.SaltAvoidingNodeMaker;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.PathNodeNavigator;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.world.World;

/**
 * Ordinary ground navigation, except the node maker treats salt as a wall (see
 * {@link SaltAvoidingNodeMaker}). It can also open and walk through wooden doors, like a person.
 */
public class MimicNavigation extends MobNavigation {
	public MimicNavigation(MobEntity mob, World world) {
		super(mob, world);
		this.setCanPathThroughDoors(true);
		this.setCanEnterOpenDoors(true);
	}

	@Override
	protected PathNodeNavigator createPathNodeNavigator(int range) {
		// Mirrors MobNavigation#createPathNodeNavigator, with our node maker swapped in.
		this.nodeMaker = new SaltAvoidingNodeMaker();
		this.nodeMaker.setCanEnterOpenDoors(true);
		return new PathNodeNavigator(this.nodeMaker, range);
	}
}
