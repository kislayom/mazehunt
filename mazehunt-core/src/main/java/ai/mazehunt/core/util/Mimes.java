package ai.mazehunt.core.util;

import java.nio.file.Path;
import java.util.Locale;

/** Tiny bidirectional MIME / file-extension mapping shared across modules. */
public final class Mimes {

    public static final String DEFAULT_IMAGE = "image/png";
    public static final String DEFAULT_AUDIO = "audio/wav";

    private Mimes() {}

    /** Guess an image MIME type from a filename (extension). */
    public static String imageMimeFromPath(Path p) {
        return imageMimeFromName(p.getFileName().toString());
    }

    public static String imageMimeFromName(String name) {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".webp")) return "image/webp";
        if (n.endsWith(".gif")) return "image/gif";
        if (n.endsWith(".bmp")) return "image/bmp";
        return DEFAULT_IMAGE;
    }

    /** File extension (including dot) for an audio MIME type. */
    public static String audioExtension(String mime) {
        return switch (mime == null ? "" : mime.toLowerCase(Locale.ROOT)) {
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/mpeg", "audio/mp3"   -> ".mp3";
            case "audio/ogg", "audio/vorbis" -> ".ogg";
            case "audio/flac"                -> ".flac";
            case "audio/webm"                -> ".webm";
            default                          -> ".bin";
        };
    }
}
