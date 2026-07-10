package com.fighter.character;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;

public final class CharacterProfileLoader {
    private static final String ROOT = "/assets/characters/";

    private CharacterProfileLoader() {
    }

    public static CharacterProfile load(String id) {
        return loadAt(ROOT + id + "/character.properties", id);
    }

    public static CharacterProfile loadRuntime(String key) {
        return loadAt("/assets/mugen-runtime/" + key + "/character.properties", key);
    }

    private static CharacterProfile loadAt(String resource, String id) {
        Properties values = new Properties();
        try (InputStream stream = CharacterProfileLoader.class.getResourceAsStream(resource)) {
            if (stream == null) return null;
            values.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            String[] idle = frames(values.getProperty("idle"));
            if (idle.length == 0) return null;
            return new CharacterProfile(
                    id,
                    values.getProperty("name", id),
                    values.getProperty("avatar", idle[0]),
                    number(values.getProperty("imageHeight"), 240),
                    idle,
                    fallback(frames(values.getProperty("punch")), idle),
                    fallback(frames(values.getProperty("kick")), idle),
                    fallback(frames(values.getProperty("guard")), idle),
                    fallback(frames(values.getProperty("hit")), idle)
            );
        } catch (Exception exception) {
            System.err.println("[CharacterProfile] 无法读取 " + id + ": " + exception.getMessage());
            return null;
        }
    }

    private static String[] frames(String value) {
        if (value == null || value.isBlank()) return new String[0];
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(frame -> !frame.isEmpty())
                .toArray(String[]::new);
    }

    private static String[] fallback(String[] frames, String[] fallback) {
        return frames.length == 0 ? fallback : frames;
    }

    private static double number(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
