package com.fighter.scenes;

import com.fighter.App;
import com.fighter.mugen.MugenCatalog;
import com.fighter.util.AudioManager;
import com.fighter.util.ResponsiveView;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * 主菜单场景 —— 游戏入口界面
 * 
 * 【功能说明】
 * 1. 显示游戏标题和操作说明
 * 2. 提供"开始游戏"按钮（ENTER/SPACE）
 * 3. 提供"退出游戏"按钮（ESC）
 * 4. 动态背景：渐变夜空 + 金色装饰粒子
 * 
 * 【修改指南】
 * - 要修改标题文字，调整 draw() 中的 fillText 参数
 * - 要修改背景颜色，调整 draw() 中的 Color.color() 数值
 * - 要添加更多菜单选项，仿照现有按钮样式绘制
 * 
 * @author 课程设计
 * @version 2.0
 */
public class MenuScene {

    private final App app;           // 主应用引用，用于切换场景
    private double time = 0;         // 时间累积器（用于动画）
    private int selectedOption = 0;

    /**
     * 构造方法
     * @param app 主应用实例（用于场景切换）
     */
    public MenuScene(App app) {
        this.app = app;
    }

    /**
     * 创建菜单场景
     * @return Scene 对象
     */
    public Scene create() {
        AudioManager.getInstance().playBGM("Menu");
        // 创建画布（960x600，与游戏内其他场景保持一致）
        Canvas canvas = new Canvas(960, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        StackPane root = ResponsiveView.wrap(canvas, 960, 600);

        Scene scene = new Scene(root, 960, 600);

        // ===== 游戏主循环（AnimationTimer） =====
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                time += 0.016;  // 每帧约16ms，推进动画时间
                draw(gc);
            }
        };
        timer.start();

        // ===== 键盘事件处理 =====
        scene.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ENTER:
                case SPACE:
                    // 开始游戏 → 切换到角色选择界面
                    timer.stop();
                    if (selectedOption == 0) app.selectGameMode(App.GameMode.SINGLE_PLAYER);
                    else if (selectedOption == 1) app.selectGameMode(App.GameMode.LOCAL_VERSUS);
                    else Platform.exit();
                    break;
                case ESCAPE:
                    // 退出游戏 → 关闭窗口
                    timer.stop();
                    Platform.exit();
                    break;
                case DOWN:
                case S:
                    // 下方向键切换选项
                    selectedOption = Math.min(2, selectedOption + 1);
                    break;
                case UP:
                case W:
                    // 上方向键切换选项
                    selectedOption = Math.max(0, selectedOption - 1);
                    break;
                default:
                    break;
            }
        });
        canvas.setOnMouseMoved(e -> {
            int option = optionAt(e.getX(), e.getY());
            if (option >= 0) selectedOption = option;
        });
        canvas.setOnMouseClicked(e -> {
            int option = optionAt(e.getX(), e.getY());
            if (option >= 0) {
                selectedOption = option;
                timer.stop();
                activateSelected();
            }
        });

        return scene;
    }

    /**
     * 每帧绘制方法 —— 渲染菜单界面
     * @param gc GraphicsContext
     */
    private void draw(GraphicsContext gc) {
        double t = time * 0.35;
        for (int y = 0; y < 600; y++) {
            double p = y / 600.0;
            double r = 0.018 + p * 0.035 + 0.012 * Math.sin(t + p * 4);
            double g = 0.025 + p * 0.025;
            double b = 0.075 + p * 0.09 + 0.018 * Math.sin(t * 0.7 + p * 5);
            gc.setStroke(Color.color(r, g, b));
            gc.strokeLine(0, y, 960, y);
        }
        for (int i = 0; i < 55; i++) {
            double x = Math.floorMod(i * 173, 950) + Math.sin(time * .3 + i) * 4;
            double y = Math.floorMod(i * 97, 560);
            double alpha = .18 + .32 * (.5 + .5 * Math.sin(time * 1.4 + i));
            gc.setFill(Color.color(.55, .78, 1, alpha));
            gc.fillOval(x, y, i % 7 == 0 ? 2.5 : 1.2, i % 7 == 0 ? 2.5 : 1.2);
        }
        gc.setGlobalAlpha(.16 + .04 * Math.sin(time));
        gc.setFill(Color.CYAN); gc.fillOval(205, 10, 550, 220);
        gc.setGlobalAlpha(1.0);
        gc.setFill(Color.rgb(7, 12, 30, .88));
        gc.fillRoundRect(145, 45, 670, 155, 24, 24);
        gc.setStroke(Color.rgb(65, 180, 255, .45)); gc.setLineWidth(1.5);
        gc.strokeRoundRect(145, 45, 670, 155, 24, 24);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.rgb(255, 205, 72)); gc.setFont(Font.font("Impact", 16));
        gc.fillText("黄金精神 · STAND PROUD", 480, 78);
        gc.setFill(Color.CYAN);
        gc.setFont(Font.font("Impact", 48)); gc.fillText("JOJO STAR BATTLE", 480, 132);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Impact", 23));
        gc.fillText("A R E N A", 480, 166);
        gc.setFill(Color.rgb(155, 180, 215)); gc.setFont(Font.font("Arial", 12));
        gc.fillText("MUGEN ATTACK ANIMATION · MELEE COMBAT · DIO KNIVES", 480, 188);

        drawButton(gc, 0, 260, "单人挑战", "玩家 VS 智能 AI", Color.CYAN);
        drawButton(gc, 1, 330, "本地双人", "同屏键盘对战", Color.rgb(255, 198, 70));
        drawButton(gc, 2, 400, "退出游戏", "返回桌面", Color.rgb(255, 90, 120));

        gc.setFill(Color.rgb(10, 16, 34, .82)); gc.fillRoundRect(210, 480, 540, 72, 14, 14);
        gc.setStroke(Color.rgb(80, 110, 160, .35)); gc.strokeRoundRect(210, 480, 540, 72, 14, 14);
        gc.setFill(Color.rgb(185, 200, 225)); gc.setFont(Font.font("Arial", 12));
        gc.fillText("↑ ↓ / W S 选择    ENTER 确认    鼠标点击", 480, 505);
        gc.setFill(Color.rgb(120, 150, 190)); gc.setFont(Font.font("Arial", 11));
        gc.fillText("窗口可拖动缩放或最大化 · 画面自动保持比例", 480, 528);
        gc.setFill(MugenCatalog.getInstance().available() ? Color.rgb(95, 225, 165) : Color.ORANGE);
        gc.setFont(Font.font("Arial", 11));
        gc.fillText("● MUGEN " + MugenCatalog.getInstance().status() + " · "
                + MugenCatalog.getInstance().runtimeProfileCount() + " 动画已转换", 480, 578);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawButton(GraphicsContext gc, int index, double y, String title, String subtitle, Color accent) {
        boolean selected = selectedOption == index;
        double pulse = selected ? .75 + .15 * Math.sin(time * 4) : .28;
        gc.setFill(selected ? Color.rgb(22, 35, 66, .96) : Color.rgb(11, 18, 39, .88));
        gc.fillRoundRect(260, y, 440, 56, 14, 14);
        gc.setStroke(Color.color(accent.getRed(), accent.getGreen(), accent.getBlue(), pulse));
        gc.setLineWidth(selected ? 2.4 : 1.0); gc.strokeRoundRect(260, y, 440, 56, 14, 14);
        if (selected) { gc.setFill(accent); gc.fillRoundRect(260, y + 8, 5, 40, 4, 4); }
        gc.setTextAlign(TextAlignment.LEFT); gc.setFill(selected ? Color.WHITE : Color.rgb(190, 202, 225));
        gc.setFont(Font.font("Arial", selected ? 19 : 17)); gc.fillText(title, 292, y + 25);
        gc.setFill(Color.rgb(115, 140, 180)); gc.setFont(Font.font("Arial", 11));
        gc.fillText(subtitle, 292, y + 44);
        gc.setTextAlign(TextAlignment.RIGHT); gc.setFill(accent); gc.setFont(Font.font("Impact", 18));
        gc.fillText(selected ? "▶" : "·", 670, y + 35);
        gc.setTextAlign(TextAlignment.CENTER);
    }

    private int optionAt(double x, double y) {
        if (x < 260 || x > 700) return -1;
        if (y >= 260 && y <= 316) return 0;
        if (y >= 330 && y <= 386) return 1;
        if (y >= 400 && y <= 456) return 2;
        return -1;
    }

    private void activateSelected() {
        if (selectedOption == 0) app.selectGameMode(App.GameMode.SINGLE_PLAYER);
        else if (selectedOption == 1) app.selectGameMode(App.GameMode.LOCAL_VERSUS);
        else Platform.exit();
    }
}
