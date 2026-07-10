package com.fighter.model;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * 空投实体 —— 每10秒从天空降落的武器补给箱
 * 
 * 【行为说明】
 * 1. 每隔10秒在随机位置（平台或地面）生成
 * 2. 从屏幕上方带降落伞缓缓落下
 * 3. 落地后停留5秒，玩家可走近捡取
 * 4. 捡取后获得随机武器（手枪除外），持续7秒
 * 5. 超时无人捡取则自动消失
 * 
 * @author 课程设计
 * @version 3.0
 */
public class Airdrop {

    public double x, y;           // 当前位置
    public final double landY;    // 落地Y坐标
    public final Weapon.Type weaponType;  // 内含武器
    public boolean active = true; // 是否活跃
    public boolean landed = false;// 是否已落地
    private int groundTimer = 0;  // 落地后计时

    private final double fallSpeed = 1.5;
    private static final int MAX_GROUND_TIME = 300; // 落地后停留5秒（300帧）

    public Airdrop(double x, double landY, Weapon.Type weaponType) {
        this.x = x;
        this.y = -40;  // 从屏幕外开始掉落
        this.landY = landY;
        this.weaponType = weaponType;
    }

    /** 逐帧更新 */
    public void update() {
        if (!active) return;

        if (!landed) {
            // 降落中
            y += fallSpeed;
            if (y >= landY) {
                y = landY;
                landed = true;
                groundTimer = 0;
            }
        } else {
            // 落地等待
            groundTimer++;
            if (groundTimer > MAX_GROUND_TIME) {
                active = false;  // 超时消失
            }
        }
    }

    /** 捡取武器，返回false表示已过期 */
    public boolean pickup() {
        if (!active || !landed) return false;
        active = false;
        return true;
    }

    /** 绘制空投箱 */
    public void draw(GraphicsContext gc) {
        if (!active) return;

        double w = 28, h = 24;

        if (!landed) {
            // 降落伞
            gc.setStroke(Color.rgb(255, 200, 100));
            gc.setLineWidth(1.5);
            gc.strokeLine(x + w/2, y, x + w/2, y - 20);
            gc.setFill(Color.rgb(255, 200, 100, 0.7));
            gc.fillArc(x - 2, y - 32, w + 8, 18, 0, 180, javafx.scene.shape.ArcType.ROUND);
        }

        // 箱子
        gc.setFill(Color.rgb(139, 90, 43));
        gc.fillRoundRect(x, y, w, h, 3, 3);
        gc.setStroke(Color.rgb(100, 60, 20));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 3, 3);

        // 箱体横带
        gc.setFill(Color.rgb(180, 120, 60));
        gc.fillRect(x, y + h/2 - 2, w, 4);

        // 武器图标（用字母标识）
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", 10));
        String icon = switch (weaponType) {
            case SHOTGUN -> "S";
            case SMG -> "M";
            case RIFLE -> "R";
            case SNIPER -> "N";
            default -> "?";
        };
        gc.fillText(icon, x + w/2 - 4, y + h/2 + 4);

        // 落地后闪烁提示
        if (landed) {
            double alpha = 0.3 + 0.3 * Math.sin(groundTimer * 0.1);
            gc.setGlobalAlpha(alpha);
            gc.setStroke(Color.YELLOW);
            gc.setLineWidth(1);
            gc.strokeOval(x - 5, y - 5, w + 10, h + 10);
            gc.setGlobalAlpha(1.0);
        }
    }

    /** 碰撞检测 */
    public boolean contains(double px, double py, double pw, double ph) {
        return active && landed
                && px + pw > x && px < x + 28
                && py + ph > y && py < y + 24;
    }
}
