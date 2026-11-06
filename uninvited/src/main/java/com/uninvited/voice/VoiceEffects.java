package com.uninvited.voice;

import java.util.Random;

/** Turns a captured phrase into something slightly wrong. Plain DSP on 48 kHz mono PCM. */
final class VoiceEffects {
	private VoiceEffects() {
	}

	/**
	 * Picks a 1-3 second window, slows it down (which also lowers the pitch, like a tape
	 * running slow), and fades the edges so it doesn't click.
	 *
	 * @param slowdown playback speed, e.g. 0.88 = 12% slower and lower
	 */
	static short[] process(short[] pcm, double slowdown, Random random) {
		int maxLen = (int) (48000 * (1.0 + random.nextDouble() * 2.0));
		int start = 0;
		if (pcm.length > maxLen) start = random.nextInt(pcm.length - maxLen + 1);
		int len = Math.min(maxLen, pcm.length);

		// Linear-interpolation resample: read the source more slowly than we write.
		int outLen = (int) (len / slowdown);
		short[] out = new short[outLen];
		for (int i = 0; i < outLen; i++) {
			double src = start + i * slowdown;
			int a = (int) src;
			int b = Math.min(a + 1, start + len - 1);
			double t = src - a;
			double v = pcm[Math.min(a, pcm.length - 1)] * (1 - t) + pcm[Math.min(b, pcm.length - 1)] * t;
			out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v * 0.9));
		}

		// 15 ms fades.
		int fade = Math.min(720, outLen / 4);
		for (int i = 0; i < fade; i++) {
			double g = i / (double) fade;
			out[i] = (short) (out[i] * g);
			out[outLen - 1 - i] = (short) (out[outLen - 1 - i] * g);
		}
		return out;
	}
}
