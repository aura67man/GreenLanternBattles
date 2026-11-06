package com.uninvited.mimic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Makes the mimic's name tag almost, but not quite, right: exactly ONE character is swapped for a
 * lookalike. The choice is seeded, so a given disguise always shows the same wrong name.
 *
 * <p>"Visible" swaps (i -> l, o -> 0, m -> rn...) are learnable by sharp-eyed players. Cyrillic
 * swaps render almost identically in Minecraft's font and are much harder to spot; they are used
 * less often so the tell stays fair.
 */
public final class Homoglyphs {
	private static final Map<Character, String[]> VISIBLE = Map.ofEntries(
			Map.entry('i', new String[]{"l", "1"}),
			Map.entry('l', new String[]{"I", "1"}),
			Map.entry('I', new String[]{"l"}),
			Map.entry('o', new String[]{"0"}),
			Map.entry('O', new String[]{"0"}),
			Map.entry('0', new String[]{"O"}),
			Map.entry('1', new String[]{"l"}),
			Map.entry('m', new String[]{"rn"}),
			Map.entry('w', new String[]{"vv"}),
			Map.entry('n', new String[]{"h"}),
			Map.entry('u', new String[]{"v"}),
			Map.entry('v', new String[]{"u"}),
			Map.entry('e', new String[]{"c"}),
			Map.entry('S', new String[]{"5"}),
			Map.entry('s', new String[]{"5"}),
			Map.entry('B', new String[]{"8"}),
			Map.entry('g', new String[]{"q"}),
			Map.entry('q', new String[]{"g"}),
			Map.entry('_', new String[]{"."}));

	// Latin -> Cyrillic lookalikes.
	private static final Map<Character, String[]> CYRILLIC = Map.ofEntries(
			Map.entry('a', new String[]{"а"}),
			Map.entry('c', new String[]{"с"}),
			Map.entry('e', new String[]{"е"}),
			Map.entry('o', new String[]{"о"}),
			Map.entry('p', new String[]{"р"}),
			Map.entry('x', new String[]{"х"}),
			Map.entry('y', new String[]{"у"}),
			Map.entry('A', new String[]{"А"}),
			Map.entry('B', new String[]{"В"}),
			Map.entry('C', new String[]{"С"}),
			Map.entry('E', new String[]{"Е"}),
			Map.entry('H', new String[]{"Н"}),
			Map.entry('K', new String[]{"К"}),
			Map.entry('M', new String[]{"М"}),
			Map.entry('O', new String[]{"О"}),
			Map.entry('P', new String[]{"Р"}),
			Map.entry('T', new String[]{"Т"}),
			Map.entry('X', new String[]{"Х"}));

	private Homoglyphs() {
	}

	/** Returns {@code name} with exactly one character swapped, chosen deterministically from {@code seed}. */
	public static String disguise(String name, long seed) {
		if (name == null || name.isEmpty()) return name;
		Random random = new Random(seed);
		boolean preferVisible = random.nextFloat() < 0.7f;

		String result = trySwap(name, preferVisible ? VISIBLE : CYRILLIC, random);
		if (result == null) result = trySwap(name, preferVisible ? CYRILLIC : VISIBLE, random);
		if (result != null) return result;
		// Nothing swappable (rare): add a trailing character that is easy to miss.
		return name + (name.length() < 16 ? "_" : "");
	}

	private static String trySwap(String name, Map<Character, String[]> table, Random random) {
		List<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < name.length(); i++) {
			if (table.containsKey(name.charAt(i))) candidates.add(i);
		}
		if (candidates.isEmpty()) return null;
		int index = candidates.get(random.nextInt(candidates.size()));
		String[] options = table.get(name.charAt(index));
		return name.substring(0, index) + options[random.nextInt(options.length)] + name.substring(index + 1);
	}
}
