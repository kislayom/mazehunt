package ai.mazehunt.voice;

/** Text-to-speech. Returns raw audio bytes (WAV or MP3 depending on backend). */
public interface Tts {
    byte[] synthesise(String text);
    String mimeType();
}
