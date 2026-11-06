package com.uninvited.mimic;

/** What a mimic is currently doing. Set by the spawn director (and /uninvited summon). */
public enum MimicMode {
	/** No behaviour of its own; stands and stares at the nearest player. Debug summons start here. */
	IDLE,
	/** Stage 2: appears far away, stares, vanishes when looked at or approached. */
	WATCH,
	/** Stage 3: walks around as a copy, follows at a distance and waves. */
	FOLLOW,
	/** Stage 4: stalks a lone player, approaches disguised as a friend, then attacks. */
	HUNT,
	/** Running away before vanishing (target rejoined the group, or it took too much damage). */
	FLEE
}
