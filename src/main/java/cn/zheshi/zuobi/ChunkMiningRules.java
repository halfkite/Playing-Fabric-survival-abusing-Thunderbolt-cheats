package cn.zheshi.zuobi;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

public final class ChunkMiningRules {
    public static final String RULE_NAME = "creativeChunkMining";
    private static final boolean BUILTIN_DEFAULT = false;
    private boolean creativeChunkMining = BUILTIN_DEFAULT;
    private Boolean configuredDefault;

    public boolean creativeChunkMining() {
        return creativeChunkMining;
    }

    public void setTemporary(boolean value) {
        creativeChunkMining = value;
    }

    public boolean configuredDefault() {
        return configuredDefault == null ? BUILTIN_DEFAULT : configuredDefault;
    }

    public void load(MinecraftServer server) {
        configuredDefault = null;
        Path file = configPath(server);
        if (Files.isRegularFile(file)) {
            try {
                for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split("\\s+", 2);
                    if (parts.length == 2 && RULE_NAME.equalsIgnoreCase(parts[0])) {
                        configuredDefault = parseBoolean(parts[1]);
                    }
                }
            } catch (IOException | IllegalArgumentException e) {
                ZheshiZuobiMod.LOGGER.warn("Failed to read {}", file, e);
            }
        }
        creativeChunkMining = configuredDefault();
    }

    public void setDefault(MinecraftServer server, boolean value) throws IOException {
        configuredDefault = value;
        creativeChunkMining = value;
        write(server);
    }

    public void removeDefault(MinecraftServer server) throws IOException {
        configuredDefault = null;
        creativeChunkMining = BUILTIN_DEFAULT;
        Files.deleteIfExists(configPath(server));
    }

    private void write(MinecraftServer server) throws IOException {
        Files.writeString(configPath(server), RULE_NAME + " " + configuredDefault + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    static boolean parseBoolean(String input) {
        return switch (input.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> true;
            case "false" -> false;
            default -> throw new IllegalArgumentException("Expected true or false: " + input);
        };
    }

    private static Path configPath(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("zuobi.conf");
    }
}

