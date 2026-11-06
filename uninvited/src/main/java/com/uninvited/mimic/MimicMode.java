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
	/**
	 * After it has taken someone: wears the taken player's face and walks with their friends as if
	 * nothing happened. Never attacks. Exposed by the Witness Bell, a punch, or the real player
	 * coming back.
	 */
	IMPOSTOR,
	/** Running away before vanishing (target rejoined the group, or it took too much damage). */
	FLEE
}
