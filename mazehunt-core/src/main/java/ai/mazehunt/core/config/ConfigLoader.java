package ai.mazehunt.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigLoader {
    private static final ObjectMapper YAML =
            new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    private ConfigLoader() {}

    public static MazehuntConfig load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return YAML.readValue(in, MazehuntConfig.class);
        }
    }

    public static MazehuntConfig loadOrDefault(Path file) {
        try {
            if (Files.exists(file)) return load(file);
        } catch (IOException e) {
            // fall through
        }
        return Defaults.workstation48gb();
    }
}
