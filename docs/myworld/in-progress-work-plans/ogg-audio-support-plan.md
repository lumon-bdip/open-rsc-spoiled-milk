# OGG Audio Support Plan

This plan covers adding OGG Vorbis support to the PC client while keeping the
current WAV sound effects working. The goal is to allow better, smaller, newer
sound assets without forcing a full audio-system rewrite.

## Current State

- Runtime sound names are extensionless. The server sends a key such as
  `mine`, `fish`, or `spellok`, and the client decides what file to play.
- `Client_Base/src/orsc/mudclient.java` currently scans `Client_Base/Cache/audio`
  and only caches files ending in `.wav`.
- `PC_Client/src/orsc/soundPlayer.java` currently looks up `key + ".wav"` and
  plays it through Java Sound with `AudioSystem.getClip()`.
- `Client_Base/Cache/audio/sounds.mem` is still present, but the current PC
  client does not use it for playback.
- `Client_Base/build.xml` already includes every jar under `PC_Client/lib`, so
  a new decoder jar can be added without changing the Ant classpath.
- `scripts/package-player-release.sh` copies the whole `Cache/audio` folder, so
  OGG assets will package automatically after the client can play them.
- Current LWJGL dependencies are core, GLFW, and OpenGL only. There is no
  decoder module in the shipped set today.

## Recommendation

Use OGG Vorbis for new or upgraded sound effects, keep WAV as the fallback, and
do not change the server sound protocol.

The best first implementation path is LWJGL STB Vorbis:

- The project already vendors LWJGL jars under `PC_Client/lib/lwjgl`.
- `scripts/download-lwjgl.sh` already supports downloading extra LWJGL modules
  through `LWJGL_MODULES`; adding `lwjgl-stb` fits the existing dependency
  shape.
- STB Vorbis decodes OGG data to PCM, which can still be handed to Java Sound
  `Clip` for short effects.
- The code stays local to PC audio playback instead of touching server packets,
  cache layout, or renderer code.

Do not use `Clip` for long music or ambience tracks later. `Clip` loads the
whole decoded sound into memory, which is fine for the short effects this game
currently uses, but longer tracks should stream through a `SourceDataLine` or a
dedicated mixer path.

## File And Build Changes

1. Update `scripts/download-lwjgl.sh`.
   - Add `lwjgl-stb` to the default `LWJGL_MODULES`.
   - Keep the pinned LWJGL version aligned with the existing `3.3.4` jars unless
     the whole LWJGL set is upgraded together.
   - Download the same native classifiers already used for OpenGL release
     builds: Linux x64, Windows x64, macOS x64, and macOS arm64.

2. Add the STB jars to `PC_Client/lib/lwjgl`.
   - Expected jar shape:
     - `lwjgl-stb-3.3.4.jar`
     - `lwjgl-stb-3.3.4-natives-linux.jar`
     - `lwjgl-stb-3.3.4-natives-windows.jar`
     - `lwjgl-stb-3.3.4-natives-macos.jar`
     - `lwjgl-stb-3.3.4-natives-macos-arm64.jar`

3. Update `scripts/package-player-release.sh`.
   - Add release validation for the STB native entries in the built client jar.
   - This should mirror the existing OpenGL native checks so a release cannot be
     published with Linux working but Windows or macOS missing decoder natives.

4. Update `Client_Base/src/orsc/mudclient.java`.
   - Make `loadSounds()` cache `.wav` and `.ogg` files.
   - Make the audio folder scan null-safe.
   - Keep filenames lowercased in `mudclient.soundCache`.
   - Leave `sounds.mem` alone unless a separate cleanup pass removes legacy
     audio code.

5. Update `PC_Client/src/orsc/soundPlayer.java`.
   - Resolve extensionless keys through a small helper.
   - Prefer `key.ogg` first, then `key.wav`, so a new OGG file can replace an
     old WAV without server changes.
   - Keep missing sounds silent, but log the key in debug builds if that helps
     future cleanup.
   - Keep the existing WAV playback path.
   - Add an OGG playback path that decodes Vorbis to PCM and opens a Java Sound
     `Clip` from the decoded buffer.

6. Add a small PC-client audio helper.
   - Suggested location: `PC_Client/src/orsc/audio/OggVorbisDecoder.java`.
   - The helper should own STB calls, native buffers, PCM format conversion, and
     cleanup.
   - `soundPlayer` should stay a small resolver/player class instead of growing
     all decoder details inline.

7. Update cache metadata and packaging inputs.
   - Add any new `.ogg` files under `Client_Base/Cache/audio`.
   - Update `Client_Base/Cache/MD5.SUM` for changed or added audio files.
   - Record third-party audio asset sources in `release/player/ASSET-SOURCES.txt`
     if the new sounds come from outside the project.

## Rollout

Phase 1: Add playback support only.

- Add STB dependency jars and release validation.
- Teach the client to cache and resolve `.ogg`.
- Add one tiny test OGG sound, ideally replacing a low-risk local sound effect
  such as `click` or `coins`.
- Confirm WAV fallback still works by leaving most current effects unchanged.

Phase 2: Convert or replace sound effects gradually.

- Replace effects one group at a time: UI, skilling, combat, magic, ambience.
- Keep extensionless keys unchanged.
- Prefer mono OGG for positional/simple effects and stereo only where it
  actually improves the sound.
- Normalize volume during conversion so new sounds do not blast over old ones.

Phase 3: Optional music or ambient loop support.

- Add streaming playback instead of using `Clip`.
- Add a separate music/ambience volume toggle if the scope grows beyond short
  sound effects.
- Consider LWJGL OpenAL only if the game later needs proper mixing, positional
  audio, or many simultaneous looping sources.

## Validation Checklist

- `./scripts/build-client.sh` succeeds.
- The built `Client_Base/Open_RSC_Client.jar` contains STB classes and native
  libraries for all release platforms.
- The client can play an OGG effect and an existing WAV effect in the same
  session.
- Missing sound keys do not crash the client.
- Sound toggle behavior still suppresses WAV and OGG playback.
- Player release packaging succeeds and includes all OGG files in `Cache/audio`.
- Windows package launches and plays an OGG sound using the bundled Java 17
  runtime.
- Existing server packets still send extensionless sound keys.

## Risks

- STB native jars must be present for every release platform, not just the local
  development OS.
- Decoded OGG data should be short-lived or cached carefully. Decoding every
  repeated sound effect on demand may become wasteful if the same effects play
  often.
- Preloading every decoded OGG into memory is probably fine for short effects,
  but should be measured before replacing the whole sound library.
- Java Sound mixers vary by OS. Keep WAV fallback until OGG playback is tested
  on Linux and the Windows player package.
- Long tracks should not use the same `Clip` path as short effects.

## Open Decisions

- Whether OGG should be preferred over WAV when both files exist. This plan
  recommends OGG first because it makes asset replacement simple.
- Whether decoded PCM should be cached. Start with decode-on-play for a small
  proof, then cache frequently used effects if latency or CPU cost is noticeable.
- Whether to clean up `sounds.mem` in the same patch. The safer choice is to
  leave it alone until OGG support is proven.
