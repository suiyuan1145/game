package com.fighter.mugen;

import com.fighter.model.Fighter;
import com.fighter.character.CharacterProfile;
import com.fighter.character.CharacterProfileLoader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Runtime index for Elecbyte M.U.G.E.N text definitions. Binary SFF/SND files are indexed, not decoded. */
public final class MugenCatalog {
    private static final Path DEFAULT_HOME = Path.of("C:", "Users", "36451", "Downloads", "JOJO精致整合V6（主程序）");
    private static final MugenCatalog INSTANCE = new MugenCatalog();

    private Path home;
    private List<MugenCharacterInfo> characters = List.of();
    private List<MugenStageInfo> stages = List.of();
    private String error = "尚未初始化";
    private Map<String, CharacterProfile> runtimeProfiles = Map.of();

    private MugenCatalog() {}
    public static MugenCatalog getInstance() { return INSTANCE; }

    public synchronized void load() {
        Path configured = configuredHome();
        if (!Files.isDirectory(configured)) {
            home = configured;
            characters = List.of(); stages = List.of();
            error = "未找到 MUGEN 目录: " + configured;
            System.err.println("[MUGEN] " + error);
            return;
        }
        try {
            home = configured.toAbsolutePath().normalize();
            Path select = home.resolve("data").resolve("select.def");
            Map<String, List<String>> sections = parseSections(select);
            characters = List.copyOf(parseCharacters(sections.getOrDefault("characters", List.of())));
            stages = List.copyOf(parseStages(sections.getOrDefault("extrastages", List.of())));
            runtimeProfiles = loadRuntimeProfiles();
            error = "";
            System.out.printf("[MUGEN] 已解析 %d 个人物、%d 个场景，目录: %s%n", characters.size(), stages.size(), home);
        } catch (Exception exception) {
            characters = List.of(); stages = List.of();
            error = "解析失败: " + exception.getMessage();
            System.err.println("[MUGEN] " + error);
        }
    }

    public Path home() { return home; }
    public List<MugenCharacterInfo> characters() { return characters; }
    public List<MugenStageInfo> stages() { return stages; }
    public boolean available() { return error.isEmpty(); }
    public String status() { return available() ? characters.size() + " 人物 / " + stages.size() + " 场景" : error; }

    /** Command-line parser diagnostic; does not start JavaFX. */
    public static void main(String[] args) {
        MugenCatalog catalog = getInstance();
        catalog.load();
        long characterDefs = catalog.characters.stream().filter(c -> Files.isRegularFile(c.defFile())).count();
        long sprites = catalog.characters.stream().map(c -> c.file("sprite")).filter(Objects::nonNull).filter(Files::isRegularFile).count();
        long stageDefs = catalog.stages.stream().filter(s -> Files.isRegularFile(s.defFile())).count();
        long stageMusic = catalog.stages.stream().map(MugenStageInfo::musicFile).filter(Objects::nonNull).filter(Files::isRegularFile).count();
        System.out.printf("status=%s%ncharacterDefs=%d%ncharacterSprites=%d%nstageDefs=%d%nstageMusic=%d%n",
                catalog.status(), characterDefs, sprites, stageDefs, stageMusic);
    }

    public MugenCharacterInfo findCharacter(Fighter.CharType type) {
        String wanted = normalize(type.label);
        return characters.stream().filter(character -> {
            String candidate = normalize(character.displayName() + character.name() + character.folder());
            return candidate.equals(wanted) || candidate.startsWith(wanted) || candidate.contains(wanted);
        }).findFirst().orElse(null);
    }

    public String displayNameFor(Fighter.CharType type) {
        MugenCharacterInfo found = findCharacter(type);
        return found == null || found.displayName().isBlank() ? type.label : found.displayName();
    }

    public Fighter.CharType archetypeFor(MugenCharacterInfo character) {
        if (character == null) return Fighter.CharType.BLUE;
        String key = normalize(character.displayName() + character.name() + character.folder());
        if (normalize(character.folder()).equals("dio") || normalize(character.displayName()).equals("dio")) return Fighter.CharType.RED;
        if (key.contains("jotaro")) return Fighter.CharType.BLUE;
        if (key.contains("giorno")) return Fighter.CharType.YELLOW;
        if (key.contains("pucci")) return Fighter.CharType.WHITE;
        if (key.contains("polnareff")) return Fighter.CharType.GREEN;
        if (key.contains("diavolo")) return Fighter.CharType.BLACK;
        Fighter.CharType[] melee = {Fighter.CharType.BLUE, Fighter.CharType.YELLOW, Fighter.CharType.WHITE,
                Fighter.CharType.GREEN, Fighter.CharType.PINK, Fighter.CharType.BLACK};
        return melee[Math.floorMod(character.folder().hashCode(), melee.length)];
    }

    public CharacterProfile runtimeProfileFor(MugenCharacterInfo character) {
        if (character == null) return null;
        return runtimeProfiles.get(normalize(character.folder()));
    }

    public int runtimeProfileCount() { return runtimeProfiles.size(); }

    private Map<String, CharacterProfile> loadRuntimeProfiles() {
        Map<String, CharacterProfile> profiles = new HashMap<>();
        try (var stream = MugenCatalog.class.getResourceAsStream("/assets/mugen-runtime/index.tsv")) {
            if (stream == null) return Map.of();
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\\t", -1);
                    if (parts.length < 3 || !Boolean.parseBoolean(parts[2])) continue;
                    CharacterProfile profile = CharacterProfileLoader.loadRuntime(parts[1]);
                    if (profile != null) profiles.put(normalize(parts[0]), profile);
                }
            }
        } catch (Exception exception) {
            System.err.println("[MUGEN] 动态动画清单读取失败: " + exception.getMessage());
        }
        return Map.copyOf(profiles);
    }

    public String previewFor(MugenStageInfo stage, int fallbackIndex) {
        String key = normalize(stage.name() + stage.defFile().getFileName());
        if (key.contains("diosmansion")) return "mugen/stages/22-dio-s-mansion.png";
        if (key.contains("redmoonlight") || key.contains("bridgemoon")) return "mugen/stages/36-final-battle-under-the-red-moonlight.png";
        if (key.contains("italy") || key.contains("kof2001")) return "mugen/stages/47-italy-stage.png";
        if (key.contains("kennedyspacecenter")) return "mugen/stages/50-kennedy-space-center.png";
        String[] fallback = {"mugen/stages/22-dio-s-mansion.png", "mugen/stages/36-final-battle-under-the-red-moonlight.png",
                "mugen/stages/47-italy-stage.png", "mugen/stages/50-kennedy-space-center.png"};
        return fallback[Math.floorMod(fallbackIndex, fallback.length)];
    }

    private List<MugenCharacterInfo> parseCharacters(List<String> lines) {
        List<MugenCharacterInfo> result = new ArrayList<>();
        for (String raw : lines) {
            String entry = clean(raw);
            if (entry.isBlank()) continue;
            String[] options = entry.split(",");
            String spec = options[0].trim();
            int order = optionInt(options, "order", 1);
            int slash = Math.max(spec.lastIndexOf('/'), spec.lastIndexOf('\\'));
            String folder = slash < 0 ? spec : spec.substring(0, slash).trim();
            String defName = slash < 0 ? spec : spec.substring(slash + 1).trim();
            Path folderPath = home.resolve("chars").resolve(folder).normalize();
            Path def = findDef(folderPath, defName);
            Map<String, List<String>> ini = parseSectionsQuietly(def);
            Map<String, String> info = keyValues(ini.getOrDefault("info", List.of()));
            Map<String, String> fileValues = keyValues(ini.getOrDefault("files", List.of()));
            Map<String, Path> files = new LinkedHashMap<>();
            fileValues.forEach((key, value) -> files.put(key, folderPath.resolve(unquote(value)).normalize()));
            result.add(new MugenCharacterInfo(entry, folder, defName, info.getOrDefault("name", defName),
                    info.getOrDefault("displayname", info.getOrDefault("name", defName)), info.getOrDefault("author", ""),
                    order, def, Map.copyOf(files)));
        }
        return result;
    }

    private List<MugenStageInfo> parseStages(List<String> lines) {
        List<MugenStageInfo> result = new ArrayList<>();
        for (String raw : lines) {
            String entry = clean(raw);
            if (entry.isBlank() || !entry.toLowerCase().contains(".def")) continue;
            Path def = home.resolve(entry.replace('/', java.io.File.separatorChar)).normalize();
            Map<String, List<String>> ini = parseSectionsQuietly(def);
            Map<String, String> info = keyValues(ini.getOrDefault("info", List.of()));
            Map<String, String> bg = keyValues(ini.getOrDefault("bgdef", List.of()));
            Map<String, String> music = keyValues(ini.getOrDefault("music", List.of()));
            Path sprite = resolveHomeFile(bg.get("spr"));
            Path bgm = resolveHomeFile(music.get("bgmusic"));
            result.add(new MugenStageInfo(entry, info.getOrDefault("name", fileStem(def)), def, sprite, bgm,
                    Map.copyOf(keyValues(ini.getOrDefault("camera", List.of()))),
                    Map.copyOf(keyValues(ini.getOrDefault("stageinfo", List.of())))));
        }
        return result;
    }

    private Path configuredHome() {
        String property = System.getProperty("mugen.home");
        if (property != null && !property.isBlank()) return Path.of(property);
        String environment = System.getenv("MUGEN_HOME");
        return environment == null || environment.isBlank() ? DEFAULT_HOME : Path.of(environment);
    }

    private Path resolveHomeFile(String value) {
        if (value == null || value.isBlank()) return null;
        Path path = Path.of(unquote(value.replace('\\', java.io.File.separatorChar)));
        return (path.isAbsolute() ? path : home.resolve(path)).normalize();
    }

    private static Path findDef(Path folder, String defName) {
        Path expected = folder.resolve(defName.toLowerCase().endsWith(".def") ? defName : defName + ".def");
        if (Files.isRegularFile(expected)) return expected;
        try (var files = Files.list(folder)) {
            return files.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().toLowerCase().endsWith(".def"))
                    .findFirst().orElse(expected);
        } catch (IOException ignored) { return expected; }
    }

    private static Map<String, List<String>> parseSectionsQuietly(Path file) {
        try { return parseSections(file); } catch (Exception ignored) { return Map.of(); }
    }

    private static Map<String, List<String>> parseSections(Path file) throws IOException {
        Map<String, List<String>> sections = new LinkedHashMap<>();
        String current = "";
        for (String raw : readText(file).split("\\R")) {
            String line = raw.strip();
            if (line.startsWith("[") && line.contains("]")) {
                current = line.substring(1, line.indexOf(']')).trim().toLowerCase();
                sections.computeIfAbsent(current, ignored -> new ArrayList<>());
            } else if (!current.isEmpty()) sections.get(current).add(raw);
        }
        return sections;
    }

    private static String readText(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException ignored) {
            return Charset.forName("GB18030").decode(ByteBuffer.wrap(bytes)).toString();
        }
    }

    private static Map<String, String> keyValues(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = clean(raw);
            int equals = line.indexOf('=');
            if (equals > 0) values.put(line.substring(0, equals).trim().toLowerCase(), unquote(line.substring(equals + 1).trim()));
        }
        return values;
    }

    private static int optionInt(String[] options, String name, int fallback) {
        for (int i = 1; i < options.length; i++) {
            String[] pair = options[i].split("=", 2);
            if (pair.length == 2 && pair[0].trim().equalsIgnoreCase(name)) {
                try { return Integer.parseInt(pair[1].trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return fallback;
    }

    private static String clean(String value) {
        int comment = value.indexOf(';');
        return (comment >= 0 ? value.substring(0, comment) : value).trim();
    }
    private static String unquote(String value) { return value.replaceAll("^[\\\"']|[\\\"']$", "").trim(); }
    private static String fileStem(Path path) { String n = path.getFileName().toString(); return n.replaceFirst("(?i)\\.def$", ""); }
    private static String normalize(String value) { return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fff]", ""); }
}
