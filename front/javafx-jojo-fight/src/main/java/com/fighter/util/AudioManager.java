package com.fighter.util;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.*;

/**
 * Audio Manager - load and play sounds from M.U.G.E.N game directory.
 * Supports WAV and MP3 files. Falls back silently if game not found.
 */
public class AudioManager {

    // 单例模式
    private static final AudioManager INSTANCE = new AudioManager();

    private String gameDataPath;                     // 游戏资源根路径
    private final Map<String, Media> soundCache = new HashMap<>();  // 音频缓存
    private MediaPlayer bgmPlayer;                  // BGM播放器
    private boolean initialized = false;            // 是否初始化成功

    // 已知的菜单音效文件路径（相对于游戏目录）
    private static final String[] MENU_SOUNDS = {
        "data/Menu.mp3", "data/Select.mp3", "data/Title.mp3",
        "data/win.mp3", "data/vs.mp3", "data/start.mp3"
    };

    // 已知的攻击音效文件路径（相对于游戏目录）
    private static final String[] HIT_SOUNDS = {
        "chars/Giorno3/Muda1.wav",
        "chars/Giorno3/Muda2.wav",
        "chars/Giorno3/muda3.wav",
        "chars/Diavolo/118.wav",
        "chars/Diego Brando AU/punch.wav",
        "chars/Diego Brando AU/endpunch.wav"
    };

    private AudioManager() {}

    public static AudioManager getInstance() { return INSTANCE; }

    public void configure(Path home) {
        if (home != null) gameDataPath = home.toAbsolutePath().normalize().toString();
    }

    /**
     * 初始化 —— 扫描磁盘寻找原游戏目录并加载音频资源
     * 如果未找到游戏目录，静默失败（不影响游戏运行）
     */
    public void init() {
        if (gameDataPath != null) {
            File configured = new File(gameDataPath);
            if (configured.isDirectory()) {
                initialized = true;
                preloadSounds();
                System.out.println("AudioManager: 使用解析器目录 " + gameDataPath);
                return;
            }
        }
        // 可能的游戏目录路径列表
        String[] possiblePaths = {
            "C:\\Users\\hjk07\\Desktop\\JOJO精致整合V6（主程序）",
            "D:\\JOJO精致整合V6（主程序）",
            "E:\\JOJO精致整合V6（主程序）",
            "..\\JOJO精致整合V6（主程序）",
            "..\\..\\JOJO精致整合V6（主程序）"
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                gameDataPath = path;
                System.out.println("AudioManager: 找到游戏目录 " + path);
                initialized = true;
                preloadSounds();
                return;
            }
        }

        System.out.println("AudioManager: 未找到游戏目录，音频功能禁用");
    }

    /** 预加载所有已知的音效到缓存 */
    private void preloadSounds() {
        for (String soundPath : MENU_SOUNDS) {
            loadSound(soundPath, "menu_" + new File(soundPath).getName().replace(".mp3", ""));
        }
        for (int i = 0; i < HIT_SOUNDS.length; i++) {
            loadSound(HIT_SOUNDS[i], "hit_" + i);
        }
        System.out.println("AudioManager: 已加载 " + soundCache.size() + " 个音效");
    }

    /** 加载单个音效文件到缓存 */
    private void loadSound(String relativePath, String key) {
        try {
            File file = new File(gameDataPath, relativePath);
            if (file.exists()) {
                URI uri = file.toURI();
                Media media = new Media(uri.toString());
                soundCache.put(key, media);
            }
        } catch (Exception e) {
            // 文件不存在或格式不支持时静默忽略
        }
    }

    /**
     * 播放菜单音效
     * @param name 音效名（如 "start", "win"）
     */
    public void playMenuSound(String name) {
        if (!initialized) return;
        String key = "menu_" + name;
        Media media = soundCache.get(key);
        if (media != null) {
            MediaPlayer mp = new MediaPlayer(media);
            mp.setVolume(0.3);
            mp.play();
            mp.setOnEndOfMedia(mp::dispose);
        }
    }

    /** 播放攻击命中音效 */
    public void playHitSound() {
        if (!initialized) return;
        // 播放第一个可用的打击音效
        for (int i = 0; i < HIT_SOUNDS.length; i++) {
            String key = "hit_" + i;
            Media media = soundCache.get(key);
            if (media != null) {
                MediaPlayer mp = new MediaPlayer(media);
                mp.setVolume(0.2);
                mp.play();
                mp.setOnEndOfMedia(mp::dispose);
                break;
            }
        }
    }

    /**
     * 播放背景音乐（循环）
     * @param name 音乐名（如 "Menu"）
     */
    public void playBGM(String name) {
        if (!initialized) return;
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
        }
        String key = "menu_" + name;
        Media media = soundCache.get(key);
        if (media != null) {
            bgmPlayer = new MediaPlayer(media);
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);  // 无限循环
            bgmPlayer.setVolume(0.2);
            bgmPlayer.play();
        }
    }

    public void playFile(Path file) {
        if (file == null || !java.nio.file.Files.isRegularFile(file)) return;
        try {
            if (bgmPlayer != null) { bgmPlayer.stop(); bgmPlayer.dispose(); }
            bgmPlayer = new MediaPlayer(new Media(file.toUri().toString()));
            bgmPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            bgmPlayer.setVolume(0.2);
            bgmPlayer.play();
        } catch (Exception exception) {
            System.err.println("AudioManager: 无法播放场景音乐 " + file + ": " + exception.getMessage());
        }
    }

    /** 释放音频资源 */
    public void shutdown() {
        if (bgmPlayer != null) {
            bgmPlayer.stop();
            bgmPlayer.dispose();
        }
    }
}
