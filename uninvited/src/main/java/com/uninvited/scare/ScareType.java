package com.uninvited.scare;

import com.uninvited.stage.Stage;

/** Every scare there is, and the first stage it can happen in. */
public enum ScareType {
	FOOTSTEPS(Stage.SETTLING_IN),
	DOOR(Stage.SETTLING_IN),
	CHEST(Stage.SETTLING_IN),
	TORCH(Stage.SETTLING_IN),
	ODD_BLOCK(Stage.WATCHING),
	WATCHER(Stage.WATCHING),
	FAKE_CHAT(Stage.BECOMING),
	/** Only with Simple Voice Chat installed and voiceMimicry on. */
	VOICE(Stage.BECOMING),
	FOLLOWER(Stage.BECOMING),
	HUNTER(Stage.HUNTING);

	public final Stage minStage;

	ScareType(Stage minStage) {
		this.minStage = minStage;
	}
}
