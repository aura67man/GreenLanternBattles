package com.uninvited.command;

import com.uninvited.mimic.MimicMode;
import com.uninvited.stage.Stage;

final class MimicDirectorHooks {
	private MimicDirectorHooks() {
	}

	/** What a debug-summoned mimic does when no mode is given: whatever fits the current stage. */
	static MimicMode defaultModeFor(Stage stage) {
		return switch (stage) {
			case WATCHING -> MimicMode.WATCH;
			case DORMANT, SETTLING_IN, BECOMING -> MimicMode.FOLLOW;
			case HUNTING -> MimicMode.HUNT;
		};
	}
}
