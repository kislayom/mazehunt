package ai.mazehunt.voice.whisper;

import ai.mazehunt.core.util.Mimes;
import ai.mazehunt.voice.Stt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs a local {@code whisper.cpp} binary. Zero-dependency: we shell out rather
 * than bundle native bindings. Configure {@code whisperBinary} and {@code modelPath}.
 */
public final class WhisperCppStt implements Stt {

    private final Path whisperBinary;
    private final Path modelPath;

    public WhisperCppStt(Path whisperBinary, Path modelPath) {
        this.whisperBinary = whisperBinary;
        this.modelPath = modelPath;
    }

    @Override
    public String transcribe(Path audioFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    whisperBinary.toString(),
                    "-m", modelPath.toString(),
                    "-f", audioFile.toString(),
                    "-otxt",
                    "-nt")
                    .redirectErrorStream(true);
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes());
            int exit = p.waitFor();
            if (exit != 0) throw new RuntimeException("whisper.cpp exit=" + exit + ": " + out);
            return out.trim();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("whisper transcription failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String transcribe(byte[] audioBytes, String mimeType) {
        try {
            Path tmp = Files.createTempFile("mazehunt-", Mimes.audioExtension(mimeType));
            Files.write(tmp, audioBytes);
            try { return transcribe(tmp); }
            finally { Files.deleteIfExists(tmp); }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
