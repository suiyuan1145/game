package com.fighter.scenes;

import com.fighter.App;
import com.fighter.model.Fighter;
import com.fighter.mugen.MugenCatalog;
import com.fighter.mugen.MugenCharacterInfo;
import com.fighter.mugen.MugenStageInfo;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import com.fighter.util.ResponsiveView;

import java.nio.file.Files;
import java.nio.file.Path;

/** Shows the exact parsed MUGEN assets that will be used before entering combat. */
public final class LoadingScene {
    private final App app;
    private final Fighter.CharType p1;
    private final Fighter.CharType p2;
    private final MugenStageInfo stage;
    private final MugenCharacterInfo p1Info, p2Info;

    public LoadingScene(App app, Fighter.CharType p1, Fighter.CharType p2,
                        MugenCharacterInfo p1Info, MugenCharacterInfo p2Info, MugenStageInfo stage) {
        this.app = app; this.p1 = p1; this.p2 = p2; this.p1Info = p1Info; this.p2Info = p2Info; this.stage = stage;
    }

    public Scene create() {
        Canvas canvas = new Canvas(960, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(ResponsiveView.wrap(canvas, 960, 600), 960, 600);
        AnimationTimer timer = new AnimationTimer() {
            private long started;
            @Override public void handle(long now) {
                if (started == 0) started = now;
                double progress = Math.min(1, (now - started) / 1_800_000_000.0);
                draw(gc, progress);
                if (progress >= 1) { stop(); app.enterFight(); }
            }
        };
        timer.start();
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER, SPACE -> { timer.stop(); app.enterFight(); }
                case ESCAPE -> { timer.stop(); app.showScreen(App.GameScreen.CHARACTER_SELECT); }
                default -> { }
            }
        });
        return scene;
    }

    private void draw(GraphicsContext gc, double progress) {
        gc.setFill(Color.rgb(5, 8, 22)); gc.fillRect(0, 0, 960, 600);
        gc.setStroke(Color.rgb(30, 65, 110));
        for (int y = 0; y < 600; y += 24) gc.strokeLine(0, y, 960, y);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.CYAN); gc.setFont(Font.font("Impact", 36));
        gc.fillText("MUGEN ASSET LOADING", 480, 72);

        drawCharacter(gc, p1, p1Info, 70, "P1");
        drawCharacter(gc, p2, p2Info, 530, app.getGameMode() == App.GameMode.SINGLE_PLAYER ? "AI" : "P2");

        gc.setFill(Color.GOLD); gc.setFont(Font.font("Arial", 18));
        gc.fillText(stage == null ? "程序内置场景" : stage.name(), 480, 355);
        gc.setFill(Color.LIGHTGRAY); gc.setFont(Font.font("Arial", 12));
        if (stage != null) {
            gc.fillText("DEF " + state(stage.defFile()) + "   SFF " + state(stage.spriteFile())
                    + "   BGM " + state(stage.musicFile()), 480, 382);
        }
        gc.setFill(Color.rgb(25, 30, 48)); gc.fillRoundRect(120, 465, 720, 20, 10, 10);
        gc.setFill(Color.CYAN); gc.fillRoundRect(122, 467, 716 * progress, 16, 8, 8);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial", 13));
        gc.fillText("解析人物与场景资源 " + (int) (progress * 100) + "%", 480, 520);
        gc.setFill(Color.GRAY); gc.setFont(Font.font("Arial", 11));
        gc.fillText("ENTER 跳过 · ESC 返回选角", 480, 558);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawCharacter(GraphicsContext gc, Fighter.CharType type, MugenCharacterInfo info, double x, String side) {
        gc.setFill(Color.rgb(20, 26, 48)); gc.fillRoundRect(x, 120, 360, 180, 14, 14);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(type.body); gc.setFont(Font.font("Impact", 24));
        gc.fillText(side + "  " + (info == null ? type.label : info.displayName()), x + 180, 158);
        gc.setFill(Color.LIGHTGRAY); gc.setFont(Font.font("Arial", 12));
        if (info == null) {
            gc.fillText("使用项目内置角色配置", x + 180, 198);
        } else {
            gc.fillText("作者: " + (info.author().isBlank() ? "未注明" : info.author()), x + 180, 190);
            gc.fillText("DEF " + state(info.defFile()) + "  SFF " + state(info.file("sprite"))
                    + "  AIR " + state(info.file("anim")), x + 180, 222);
            gc.fillText("CMD " + state(info.file("cmd")) + "  CNS " + state(info.file("cns"))
                    + "  SND " + state(info.file("sound")), x + 180, 250);
        }
        gc.setFill(Color.rgb(130, 150, 180)); gc.setFont(Font.font("Arial", 10));
        gc.fillText(info == null ? type.profileId : info.folder(), x + 180, 280);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static String state(Path file) { return file != null && Files.isRegularFile(file) ? "✓" : "—"; }
}
