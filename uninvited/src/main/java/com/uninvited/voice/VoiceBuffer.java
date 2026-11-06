package com.uninvited.voice;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolling, in-memory store of one player's voice clips. Never written to disk.
 *
 * <p>Capture runs on SVC's network thread and playback on the server thread, so every public
 * method is synchronized.
 *
 * <p><b>How clips are cut.</b> Each 20 ms frame is decoded once, only to measure its loudness
 * (RMS); the PCM is then thrown away and the original Opus bytes are kept. A clip starts at
 * the first loud frame and ends after {@value #SILENCE_SPLIT_FRAMES} quiet frames in a row
 * (a pause between phrases), when the speaker releases push-to-talk (an empty packet), or
 * at {@value #MAX_CLIP_FRAMES} frames. Trailing silence is trimmed.
 *
 * <p><b>Louder is better.</b> Clips quieter than {@link #MIN_KEEP_RMS} are discarded outright.
 * When the buffer is over budget the quietest clip among the older half goes first, so laughs
 * and shouts survive the longest; playback also weights by loudness.
 */
final class VoiceBuffer {
	static final int FRAME_SAMPLES = 960;                 // 20 ms at 48 kHz
	static final int FRAMES_PER_SECOND = 50;
	static final int MAX_FRAMES = 3 * 60 * FRAMES_PER_SECOND; // ~3 minutes per player
	static final int SILENCE_SPLIT_FRAMES = 12;           // 240 ms pause ends a clip
	static final int MAX_CLIP_FRAMES = 4 * FRAMES_PER_SECOND;
	static final int MIN_VOICED_FRAMES = 25;              // at least 0.5 s of actual sound
	static final double SILENCE_RMS = 350.0;              // 16-bit sample units
	static final double MIN_KEEP_RMS = 700.0;

	private final List<VoiceClip> clips = new ArrayList<>();
	private int totalFrames;

	// Clip being built
	private final List<byte[]> current = new ArrayList<>();
	private double currentSumSquares;
	private int currentVoiced;
	private int silentRun;

	/** Feed one received frame; {@code rms} is its loudness after decoding. */
	synchronized void accept(byte[] opus, double rms) {
		boolean voiced = rms >= SILENCE_RMS;
		if (current.isEmpty()) {
			if (!voiced) return;
		}
		current.add(opus);
		if (voiced) {
			currentVoiced++;
			currentSumSquares += rms * rms;
			silentRun = 0;
		} else {
			silentRun++;
		}
		if (silentRun >= SILENCE_SPLIT_FRAMES || current.size() >= MAX_CLIP_FRAMES) {
			finishClip();
		}
	}

	/** The speaker stopped transmitting. */
	synchronized void endOfTransmission() {
		finishClip();
	}

	private void finishClip() {
		int keep = current.size() - silentRun;
		if (currentVoiced >= MIN_VOICED_FRAMES && keep > 0) {
			double rms = Math.sqrt(currentSumSquares / currentVoiced);
			if (rms >= MIN_KEEP_RMS) {
				clips.add(new VoiceClip(List.copyOf(current.subList(0, keep)), rms, System.currentTimeMillis()));
				totalFrames += keep;
				evictToBudget();
			}
		}
		current.clear();
		currentSumSquares = 0;
		currentVoiced = 0;
		silentRun = 0;
	}

	private void evictToBudget() {
		while (totalFrames > MAX_FRAMES && !clips.isEmpty()) {
			int olderHalf = Math.max(1, clips.size() / 2);
			int quietest = 0;
			for (int i = 1; i < olderHalf; i++) {
				if (clips.get(i).rms() < clips.get(quietest).rms()) quietest = i;
			}
			totalFrames -= clips.remove(quietest).frameCount();
		}
	}

	/** Weighted towards loud clips (weight = rms squared). Null if empty. */
	synchronized VoiceClip pick(java.util.Random random) {
		if (clips.isEmpty()) return null;
		double total = 0;
		for (VoiceClip clip : clips) total += clip.rms() * clip.rms();
		double roll = random.nextDouble() * total;
		for (VoiceClip clip : clips) {
			roll -= clip.rms() * clip.rms();
			if (roll <= 0) return clip;
		}
		return clips.get(clips.size() - 1);
	}

	synchronized int clipCount() {
		return clips.size();
	}

	synchronized boolean isEmpty() {
		return clips.isEmpty();
	}

	/** Drop every reference so the audio can be garbage collected. */
	synchronized void clear() {
		clips.clear();
		current.clear();
		totalFrames = 0;
		currentSumSquares = 0;
		currentVoiced = 0;
		silentRun = 0;
	}

	static double rms(short[] pcm) {
		if (pcm == null || pcm.length == 0) return 0;
		double sum = 0;
		for (short s : pcm) sum += (double) s * s;
		return Math.sqrt(sum / pcm.length);
	}
}
