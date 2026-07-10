package com.fighter.scenes;

import com.fighter.App;
import com.fighter.model.Fighter;
import com.fighter.mugen.MugenCatalog;
import com.fighter.mugen.MugenCharacterInfo;
import com.fighter.util.ResponsiveView;
import com.fighter.util.SpriteLoader;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/** Paginated runtime selector backed by all characters parsed from MUGEN select.def. */
public final class CharacterSelectScene {
    private static final int COLS = 10, ROWS = 5, PAGE_SIZE = COLS * ROWS;
    private static final double START_X = 35, START_Y = 82, CARD_W = 85, CARD_H = 82, GAP_X = 7, GAP_Y = 8;
    private final App app;
    private final List<MugenCharacterInfo> roster = new ArrayList<>();
    private int cursor;
    private boolean p1Done;
    private MugenCharacterInfo p1Choice;
    private double time;

    public CharacterSelectScene(App app) { this.app = app; refreshRoster(); }

    public void reset() { refreshRoster(); cursor = 0; p1Done = false; p1Choice = null; }

    private void refreshRoster() {
        roster.clear();
        roster.addAll(MugenCatalog.getInstance().characters());
    }

    public Scene create() {
        boolean single = app.getGameMode() == App.GameMode.SINGLE_PLAYER;
        Canvas canvas = new Canvas(960, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        Scene scene = new Scene(ResponsiveView.wrap(canvas, 960, 600), 960, 600);
        AnimationTimer timer = new AnimationTimer() {
            @Override public void handle(long now) { time += .016; draw(gc, single); }
        };
        timer.start();

        scene.setOnKeyPressed(event -> {
            int old = cursor;
            if (event.getCode() == KeyCode.LEFT || event.getCode() == KeyCode.A) cursor--;
            if (event.getCode() == KeyCode.RIGHT || event.getCode() == KeyCode.D) cursor++;
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.W) cursor -= COLS;
            if (event.getCode() == KeyCode.DOWN || event.getCode() == KeyCode.S) cursor += COLS;
            if (event.getCode() == KeyCode.PAGE_UP || event.getCode() == KeyCode.Q) cursor -= PAGE_SIZE;
            if (event.getCode() == KeyCode.PAGE_DOWN || event.getCode() == KeyCode.E) cursor += PAGE_SIZE;
            if (!roster.isEmpty()) cursor = Math.floorMod(cursor, roster.size()); else cursor = old;
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE || event.getCode() == KeyCode.J) {
                confirm(timer, single);
            }
            if (event.getCode() == KeyCode.ESCAPE) {
                timer.stop(); app.showScreen(App.GameScreen.MENU);
            }
        });
        canvas.setOnMouseMoved(event -> { int hit = cardAt(event.getX(), event.getY()); if (hit >= 0) cursor = hit; });
        canvas.setOnMouseClicked(event -> {
            int hit = cardAt(event.getX(), event.getY());
            if (hit >= 0) { cursor = hit; confirm(timer, single); }
        });
        return scene;
    }

    private void confirm(AnimationTimer timer, boolean single) {
        if (roster.isEmpty()) return;
        if (!p1Done) {
            p1Choice = roster.get(cursor); p1Done = true;
            if (single) {
                MugenCharacterInfo opponent = roster.get((cursor + 1 + (int) (Math.random() * Math.max(1, roster.size() - 1))) % roster.size());
                timer.stop(); app.startFight(p1Choice, opponent);
            }
        } else {
            timer.stop(); app.startFight(p1Choice, roster.get(cursor));
        }
    }

    private int cardAt(double x, double y) {
        int page = cursor / PAGE_SIZE;
        int first = page * PAGE_SIZE;
        for (int local = 0; local < PAGE_SIZE && first + local < roster.size(); local++) {
            int col = local % COLS, row = local / COLS;
            double cx = START_X + col * (CARD_W + GAP_X), cy = START_Y + row * (CARD_H + GAP_Y);
            if (x >= cx && x <= cx + CARD_W && y >= cy && y <= cy + CARD_H) return first + local;
        }
        return -1;
    }

    private void draw(GraphicsContext gc, boolean single) {
        gc.setFill(Color.rgb(4, 7, 20)); gc.fillRect(0, 0, 960, 600);
        for (int y = 0; y < 600; y += 3) {
            gc.setStroke(Color.color(.03, .05, .12 + y / 9000.0)); gc.strokeLine(0, y, 960, y);
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.CYAN); gc.setFont(Font.font("Impact", 27));
        String side = !p1Done ? "P1" : "P2";
        gc.fillText(single ? "选择角色 · AI 自动匹配" : side + " 选择 MUGEN 角色", 480, 36);
        gc.setFill(Color.rgb(145, 165, 200)); gc.setFont(Font.font("Arial", 11));
        gc.fillText("← → ↑ ↓ / WASD 移动 · Q/E 翻页 · ENTER 确认", 480, 58);

        if (roster.isEmpty()) {
            gc.setFill(Color.ORANGE); gc.setFont(Font.font("Arial", 18)); gc.fillText("未解析到 MUGEN 人物", 480, 300); return;
        }
        int page = cursor / PAGE_SIZE, pages = (roster.size() + PAGE_SIZE - 1) / PAGE_SIZE, first = page * PAGE_SIZE;
        for (int local = 0; local < PAGE_SIZE && first + local < roster.size(); local++) {
            int index = first + local, col = local % COLS, row = local / COLS;
            double x = START_X + col * (CARD_W + GAP_X), y = START_Y + row * (CARD_H + GAP_Y);
            drawCard(gc, roster.get(index), x, y, index == cursor);
        }
        MugenCharacterInfo selected = roster.get(cursor);
        gc.setFill(Color.rgb(12, 18, 38, .95)); gc.fillRoundRect(110, 540, 740, 42, 12, 12);
        gc.setFill(Color.GOLD); gc.setFont(Font.font("Arial", 13));
        gc.fillText(selected.displayName() + "  ·  " + (selected.author().isBlank() ? "作者未注明" : selected.author())
                + "  ·  ORDER " + selected.order(), 480, 558);
        gc.setFill(Color.rgb(125, 150, 190)); gc.setFont(Font.font("Arial", 10));
        gc.fillText("第 " + (page + 1) + "/" + pages + " 页  ·  共 " + roster.size() + " 人  ·  " + selected.folder(), 480, 576);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private void drawCard(GraphicsContext gc, MugenCharacterInfo character, double x, double y, boolean selected) {
        Fighter.CharType archetype = MugenCatalog.getInstance().archetypeFor(character);
        gc.setFill(selected ? Color.rgb(35, 52, 88) : Color.rgb(15, 22, 43));
        gc.fillRoundRect(x, y, CARD_W, CARD_H, 9, 9);
        gc.setStroke(selected ? Color.GOLD : Color.rgb(55, 75, 115)); gc.setLineWidth(selected ? 2.2 : 1);
        gc.strokeRoundRect(x, y, CARD_W, CARD_H, 9, 9);
        var runtimeProfile = MugenCatalog.getInstance().runtimeProfileFor(character);
        Image avatar = runtimeProfile == null ? null : SpriteLoader.loadImage(runtimeProfile.avatar());
        if (avatar != null) {
            double h = 48, w = Math.min(48, h * avatar.getWidth() / Math.max(1, avatar.getHeight()));
            gc.drawImage(avatar, x + CARD_W / 2 - w / 2, y + 5, w, h);
        } else {
            gc.setFill(archetype.body.deriveColor(0, 1, 1, .75)); gc.fillOval(x + 28, y + 8, 29, 29);
            gc.setFill(Color.rgb(225, 235, 250)); gc.setFont(Font.font("Impact", 16)); gc.setTextAlign(TextAlignment.CENTER);
            String name = character.displayName().isBlank() ? character.folder() : character.displayName();
            gc.fillText(name.substring(0, 1).toUpperCase(), x + CARD_W / 2, y + 29);
        }
        String name = character.displayName().isBlank() ? character.folder() : character.displayName();
        if (name.length() > 13) name = name.substring(0, 12) + "…";
        gc.setTextAlign(TextAlignment.CENTER); gc.setFill(selected ? Color.GOLD : Color.WHITE);
        gc.setFont(Font.font("Arial", selected ? 10 : 9)); gc.fillText(name, x + CARD_W / 2, y + 66);
        gc.setFill(Color.rgb(95, 220, 165)); gc.setFont(Font.font("Arial", 8)); gc.fillText("DEF ✓", x + CARD_W / 2, y + 78);
        gc.setTextAlign(TextAlignment.LEFT);
    }
}
