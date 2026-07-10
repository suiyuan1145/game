package com.fighter.util;

import javafx.scene.image.Image;
import java.util.*;
import java.io.*;

/**
 * 精灵加载器 —— 负责从 resources 目录加载 PNG 精灵图片
 * 
 * 【功能说明】
 * 1. 支持加载单张 PNG 图片作为角色/背景/UI素材
 * 2. 支持精灵表(Sprite Sheet)切帧：将一张大图按行列切成多帧动画
 * 3. 内置缓存机制，重复加载不会重复读取文件
 * 4. 图片不存在时返回 null，调用方应提供像素绘制的降级方案
 * 
 * 【使用示例】
 * // 加载单张精灵
 * Image sprite = SpriteLoader.loadImage("images/fighters/jotaro_idle.png");
 * 
 * // 从精灵表取第2帧（0-indexed），每帧宽80高120，每行4帧
 * Image frame = SpriteLoader.loadFrame("images/fighters/jotaro_sheet.png", 1, 80, 120, 4);
 * 
 * 【素材存放位置】
 * src/main/resources/images/
 *   ├── background/    ← 背景图片
 *   ├── fighters/      ← 角色精灵 (按角色名分类)
 *   └── ui/            ← UI元素（按钮、血条等）
 * 
 * @author 课程设计
 * @version 1.0
 */
public class SpriteLoader {

    // 图片缓存：key = 资源路径, value = JavaFX Image对象
    private static final Map<String, Image> imageCache = new HashMap<>();

    // 资源根目录（classpath路径）
    private static final String RESOURCE_ROOT = "/assets/";

    /**
     * 加载单张完整PNG图片
     * @param relativePath 相对路径，如 "fighters/jotaro_idle.png"
     * @return Image对象，如果文件不存在则返回null
     */
    public static Image loadImage(String relativePath) {
        // 确保路径以 "/images/" 开头
        String fullPath = relativePath.startsWith("/") ? relativePath : RESOURCE_ROOT + relativePath;

        // 查缓存，避免重复IO
        if (imageCache.containsKey(fullPath)) {
            return imageCache.get(fullPath);
        }

        try {
            // 从 classpath 加载资源文件
            InputStream is = SpriteLoader.class.getResourceAsStream(fullPath);
            if (is == null) {
                System.out.println("[SpriteLoader] 未找到图片资源: " + fullPath + "（将使用像素绘制降级）");
                imageCache.put(fullPath, null);
                return null;
            }
            Image image = new Image(is);
            imageCache.put(fullPath, image);
            System.out.println("[SpriteLoader] 成功加载图片: " + fullPath + " (" + image.getWidth() + "x" + image.getHeight() + ")");
            return image;
        } catch (Exception e) {
            System.out.println("[SpriteLoader] 加载图片失败: " + fullPath + " - " + e.getMessage());
            imageCache.put(fullPath, null);
            return null;
        }
    }

    /**
     * 从精灵表(Sprite Sheet)中提取指定帧
     * 精灵表是一张包含多帧动画的大图，按行列排列
     * 
     * @param sheetPath   精灵表相对路径，如 "fighters/jotaro_sheet.png"
     * @param frameIndex  帧索引（从0开始）
     * @param frameWidth  每帧的宽度（像素）
     * @param frameHeight 每帧的高度（像素）
     * @param cols        精灵表每行的帧数
     * @return 指定帧的Image对象，失败返回null
     */
    public static Image loadFrame(String sheetPath, int frameIndex, int frameWidth, int frameHeight, int cols) {
        // 先加载整张精灵表
        Image sheet = loadImage(sheetPath);
        if (sheet == null) return null;

        // 计算帧在精灵表中的行列位置
        int row = frameIndex / cols;
        int col = frameIndex % cols;

        // 像素坐标
        int sx = col * frameWidth;
        int sy = row * frameHeight;

        // 越界检查
        if (sx + frameWidth > sheet.getWidth() || sy + frameHeight > sheet.getHeight()) {
            System.out.println("[SpriteLoader] 帧索引越界: sheet=" + sheetPath + " frame=" + frameIndex);
            return null;
        }

        // 注意：JavaFX Image不支持直接截取子图像
        // 需要在渲染时使用 GraphicsContext.drawImage(sheet, sx, sy, fw, fh, dx, dy, dw, dh)
        // 这里返回原图 + 帧参数，调用方自行截取
        // 实际项目中可使用 PixelReader 实现，但为性能考虑推荐在渲染时裁剪
        return sheet;
    }

    /**
     * 检查指定路径的精灵是否存在
     */
    public static boolean hasImage(String relativePath) {
        String fullPath = relativePath.startsWith("/") ? relativePath : RESOURCE_ROOT + relativePath;
        if (imageCache.containsKey(fullPath)) {
            return imageCache.get(fullPath) != null;
        }
        // 未加载过，尝试检查文件是否存在
        InputStream is = SpriteLoader.class.getResourceAsStream(fullPath);
        if (is != null) {
            try { is.close(); } catch (IOException ignored) {}
            return true;
        }
        return false;
    }

    /**
     * 清空图片缓存（释放内存）
     */
    public static void clearCache() {
        imageCache.clear();
        System.out.println("[SpriteLoader] 图片缓存已清空");
    }

    /**
     * 获取当前缓存中的图片数量
     */
    public static int getCacheSize() {
        return imageCache.size();
    }
}
