package com.fighter.mugen;

import java.nio.file.Path;
import java.util.Map;

public record MugenCharacterInfo(
        String rosterEntry, String folder, String defName, String name,
        String displayName, String author, int order, Path defFile,
        Map<String, Path> files
) {
    public Path file(String key) { return files.get(key.toLowerCase()); }
}
