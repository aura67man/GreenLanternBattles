package com.uninvited.salt;

import net.minecraft.block.BlockState;

/** Single place that answers "is this salt?". Filled in by the salt milestone. */
public final class SaltRules {
	private SaltRules() {
	}

	public static boolean isSalt(BlockState state) {
		return false;
	}
}
