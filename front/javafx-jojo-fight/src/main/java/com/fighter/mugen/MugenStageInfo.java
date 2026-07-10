package com.fighter.mugen;

import java.nio.file.Path;
import java.util.Map;

public record MugenStageInfo(
        String rosterEntry, String name, Path defFile, Path spriteFile,
        Path musicFile, Map<String, String> camera, Map<String, String> stageInfo
) {}
