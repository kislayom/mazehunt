package ai.mazehunt.voice;

import java.nio.file.Path;

/** Speech-to-text. */
public interface Stt {
    String transcribe(Path audioFile);
    String transcribe(byte[] audioBytes, String mimeType);
}
