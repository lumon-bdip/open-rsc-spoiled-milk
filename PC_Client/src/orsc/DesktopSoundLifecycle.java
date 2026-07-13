package orsc;

import javax.sound.sampled.Clip;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Owns Java Sound clips started by the desktop client.
 *
 * Clips normally close themselves when playback stops. Disconnect and process
 * lifecycle transitions may close them earlier through {@link #stopAll()}.
 * Registration and cleanup are synchronized so repeated cleanup is harmless
 * and a clip cannot be left between opening and lifecycle registration.
 */
final class DesktopSoundLifecycle {
	private final Set<Clip> activeClips =
		Collections.newSetFromMap(new IdentityHashMap<Clip, Boolean>());

	synchronized void register(Clip clip) {
		if (clip != null) {
			activeClips.add(clip);
		}
	}

	synchronized void release(Clip clip) {
		if (clip != null) {
			activeClips.remove(clip);
		}
	}

	void releaseAndClose(Clip clip) {
		release(clip);
		if (clip == null) {
			return;
		}
		try {
			clip.close();
		} catch (RuntimeException ignored) {
			// Audio event callbacks must not surface cleanup failures.
		}
	}

	void stopAll() {
		Clip[] clips;
		synchronized (this) {
			clips = activeClips.toArray(new Clip[activeClips.size()]);
			activeClips.clear();
		}
		for (Clip clip : clips) {
			try {
				clip.stop();
			} catch (RuntimeException ignored) {
				// Continue closing the remaining desktop audio resources.
			}
			try {
				clip.close();
			} catch (RuntimeException ignored) {
				// Cleanup is best-effort and must never break disconnect/shutdown.
			}
		}
	}
}
