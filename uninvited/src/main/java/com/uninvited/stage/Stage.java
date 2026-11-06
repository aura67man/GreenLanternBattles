package com.uninvited.stage;

/** The four escalation stages. {@link #DORMANT} is the first day, before night 1 has fallen. */
public enum Stage {
	DORMANT(0, "Dormant"),
	SETTLING_IN(1, "Settling in"),
	WATCHING(2, "Watching"),
	BECOMING(3, "Becoming"),
	HUNTING(4, "Hunting");

	public final int number;
	public final String title;

	Stage(int number, String title) {
		this.number = number;
		this.title = title;
	}

	public static Stage byNumber(int number) {
		for (Stage stage : values()) {
			if (stage.number == number) return stage;
		}
		return number > HUNTING.number ? HUNTING : DORMANT;
	}

	public boolean atLeast(Stage other) {
		return this.number >= other.number;
	}
}
