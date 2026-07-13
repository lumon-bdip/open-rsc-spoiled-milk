package orsc;

import orsc.util.GenUtil;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.io.File;

public class soundPlayer {
	private static final DesktopSoundLifecycle LIFECYCLE = new DesktopSoundLifecycle();

	public static void playSoundFile(String key) {
		try {
			if (!mudclient.optionSoundDisabled) {
				File sound = mudclient.soundCache.get(key + ".wav");
				if (sound == null)
					return;
				Clip clip = null;
				try {
					// PC sound code:
					clip = AudioSystem.getClip();
					final Clip activeClip = clip;
					clip.addLineListener(myLineEvent -> {
						if (myLineEvent.getType() == LineEvent.Type.STOP) {
							LIFECYCLE.releaseAndClose(activeClip);
						}
					});
					try (AudioInputStream audioInput = AudioSystem.getAudioInputStream(sound)) {
						clip.open(audioInput);
					}
					LIFECYCLE.register(clip);
					clip.start();

					// Android sound code:
					//int dataLength = DataOperations.getDataFileLength(key + ".pcm", soundData);
					//int offset = DataOperations.getDataFileOffset(key + ".pcm", soundData);
					//clientPort.playSound(soundData, offset, dataLength);
				} catch (Exception ex) {
					LIFECYCLE.releaseAndClose(clip);
					ex.printStackTrace();
				}
			}

		} catch (RuntimeException var6) {
			throw GenUtil.makeThrowable(var6, "client.SC(" + "dummy" + ',' + (key != null ? "{...}" : "null") + ')');
		}
	}

	/** Stops and closes every desktop clip; safe before playback and on repeats. */
	public static void stopAllSounds() {
		LIFECYCLE.stopAll();
	}
}
