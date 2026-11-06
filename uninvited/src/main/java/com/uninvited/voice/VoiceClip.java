package com.uninvited.voice;

import java.util.List;

/**
 * One captured phrase: the raw Opus frames exactly as they arrived (20 ms each, 48 kHz mono),
 * plus how loud it was. Stored encoded, not as PCM: three minutes of Opus is well under a
 * megabyte, the same audio as 16-bit PCM is about 17 MB.
 */
record VoiceClip(List<byte[]> frames, double rms, long capturedAt) {
	int frameCount() {
		return frames.size();
	}
}
