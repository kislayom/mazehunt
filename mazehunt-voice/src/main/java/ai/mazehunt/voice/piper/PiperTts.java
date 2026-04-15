package ai.mazehunt.voice.piper;

import ai.mazehunt.voice.Tts;

import java.io.IOException;
import java.nio.file.Path;

/** Local Piper TTS (writes WAV to stdout). */
public final class PiperTts implements Tts {

    private final Path piperBinary;
    private final Path voiceModel;

    public PiperTts(Path piperBinary, Path voiceModel) {
        this.piperBinary = piperBinary;
        this.voiceModel = voiceModel;
    }

    @Override
    public byte[] synthesise(String text) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    piperBinary.toString(),
                    "--model", voiceModel.toString(),
                    "--output_raw");
            pb.redirectErrorStream(false);
            Process p = pb.start();
            p.getOutputStream().write(text.getBytes());
            p.getOutputStream().close();
            byte[] out = p.getInputStream().readAllBytes();
            if (p.waitFor() != 0) {
                throw new RuntimeException(new String(p.getErrorStream().readAllBytes()));
            }
            return out;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("piper failed: " + e.getMessage(), e);
        }
    }

    @Override public String mimeType() { return "audio/wav"; }
}
