package com.shooter;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.TextInputDialog;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class GameEngine {

    private final GraphicsContext gc;
    private final Canvas canvas;
    private final double W, H;

    private AnimationTimer loop;
    private boolean running = false;
    private boolean victory = false;
    private boolean paused = false; // 新增：暂停标志

    // ===== 游戏状态 =====
    private int score = 0;
    private int lives = 100;
    private int level = 1;
    private long frame = 0;
    private boolean autoFire = false;
    private long lastShotFrame = 0;
    private int fireRate = 12;
    private int enemySpawnRate = 60;
    private int spawnCounter = 0;
    private int enemiesKilled = 0;
    private int killsForNextLevel = 10;
    private int killsToNextLevel = 10;

    // ===== 增益系统 =====
    private int bulletSplitLevel = 0;
    private double bulletSizeMult = 1.0;
    private int extraShipCount = 0;
    private boolean pendingBoost = false;

    // 增益动画显示
    private String boostMessage = "";
    private int boostMessageTimer = 0;

    // 按键
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean spacePressed = false;

    // 作弊按键序列
    private StringBuilder cheatBuffer = new StringBuilder();

    // 实体
    private Player player;
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<Star> stars = new ArrayList<>();
    private final List<Enemy> pendingEnemyBullets = new ArrayList<>();

    private boolean showMenu = true;
    private boolean gameOver = false;

    public GameEngine(GraphicsContext gc, Canvas canvas) {
        this.gc = gc;
        this.canvas = canvas;
        this.W = canvas.getWidth();
        this.H = canvas.getHeight();
    }

    public void init() {
        initStars();
        showMenu = true;
        gameOver = false;
        paused = false; // 重置暂停
    }

    // ================ 星空 ================
    private void initStars() {
        stars.clear();
        for (int i = 0; i < 120; i++)
            stars.add(new Star(Math.random() * W, Math.random() * H, Math.random() * 2.5 + 0.5,
                    Math.random() * 1.5 + 0.3, Math.random() * 0.5 + 0.5));
    }

    private void updateStars() {
        for (Star s : stars) {
            s.y += s.speed;
            if (s.y > H) {
                s.y = 0;
                s.x = Math.random() * W;
            }
        }
    }

    private void drawStars() {
        for (Star s : stars) {
            double alpha = s.brightness * (0.7 + 0.3 * Math.sin(frame * 0.02 + s.x));
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.WHITE);
            gc.fillOval(s.x - s.size / 2, s.y - s.size / 2, s.size, s.size);
        }
        gc.setGlobalAlpha(1.0);
    }

    // ================ 玩家 ================
    private void createPlayer() {
        player = new Player(W / 2, H - 70, 40, 40, 5);
    }

    private void drawPlayer() {
        if (player == null)
            return;
        if (player.invincible > 0 && (player.invincible / 4) % 2 == 0)
            return;

        drawShip(player.x, player.y, 1.0);

        if (extraShipCount > 0) {
            for (int i = 1; i <= extraShipCount; i++) {
                double offset = 28 + (i - 1) * 12;
                drawShip(player.x - offset, player.y + 8, 0.65);
                drawShip(player.x + offset, player.y + 8, 0.65);
            }
        }
    }

    private void drawShip(double cx, double cy, double s) {
        gc.save();
        gc.translate(cx, cy);
        gc.scale(s, s);
        gc.translate(-cx, -cy);

        double[] xp = { cx, cx - 22, cx - 8, cx, cx + 8, cx + 22 };
        double[] yp = { cy - 25, cy + 18, cy + 8, cy + 14, cy + 8, cy + 18 };
        gc.setFill(Color.rgb(0, 180, 255, 0.9));
        gc.fillPolygon(xp, yp, 6);
        gc.setStroke(Color.rgb(0, 247, 255));
        gc.setLineWidth(2);
        gc.strokePolygon(xp, yp, 6);
        gc.setEffect(new javafx.scene.effect.DropShadow(15, 0, 0, Color.rgb(0, 247, 255, 0.5)));
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillOval(cx - 6, cy - 13, 12, 10);
        gc.setEffect(null);
        double fl = 10 + Math.random() * 12;
        gc.setFill(Color.rgb(255, 170, 0, 0.8));
        gc.fillPolygon(new double[] { cx - 10, cx - 4, cx, cx + 4, cx + 10 },
                new double[] { cy + 16, cy + 16 + fl * 0.7, cy + 16 + fl + 4, cy + 16 + fl * 0.7, cy + 16 }, 5);
        gc.setEffect(new javafx.scene.effect.DropShadow(20, 0, 0, Color.rgb(255, 102, 0, 0.6)));
        gc.setFill(Color.rgb(255, 100, 0, 0.4));
        gc.fillPolygon(new double[] { cx - 7, cx, cx + 7 }, new double[] { cy + 16, cy + 16 + fl + 8, cy + 16 }, 3);
        gc.setEffect(null);
        gc.restore();
    }

    private void updatePlayer() {
        if (player == null)
            return;
        double speed = player.speed;
        if (level >= 3)
            speed += 1;
        if (level >= 6)
            speed += 1;
        if (level >= 10)
            speed += 1;
        if (level >= 20)
            speed += 1;
        if (leftPressed)
            player.x -= speed;
        if (rightPressed)
            player.x += speed;
        player.x = Math.max(24, Math.min(W - 24, player.x));
        if (player.invincible > 0)
            player.invincible--;
    }

    // ================ 子弹 ================
    public void fireBullet() {
        if (player == null)
            return;
        if (frame - lastShotFrame < fireRate)
            return;
        lastShotFrame = frame;

        double bw = 4 * bulletSizeMult;
        double bh = 14 * bulletSizeMult;
        double bs = 9;

        int count = Math.min(1 + (level - 1) / 3, 5);
        double spread = 15;
        emitBullets(player.x, player.y - 25, count, spread, bw, bh, bs);

        if (extraShipCount > 0) {
            int eCount = Math.max(1, count / 2);
            for (int i = 1; i <= extraShipCount; i++) {
                double off = 28 + (i - 1) * 12;
                emitBullets(player.x - off, player.y - 15, eCount, spread * 0.6, bw * 0.7, bh * 0.7, bs);
                emitBullets(player.x + off, player.y - 15, eCount, spread * 0.6, bw * 0.7, bh * 0.7, bs);
            }
        }
    }

    private void emitBullets(double x, double y, int count, double spread, double w, double h, double speed) {
        double startOff = (count - 1) * spread / 2;
        for (int i = 0; i < count; i++) {
            Bullet b = new Bullet(x - startOff + i * spread, y, Math.max(2, w), Math.max(4, h), speed, 1);
            b.splitLevel = bulletSplitLevel;
            bullets.add(b);
        }
    }

    private void drawBullets() {
        for (Bullet b : bullets) {
            gc.setEffect(new javafx.scene.effect.DropShadow(15, 0, 0, Color.rgb(0, 255, 255, 0.7)));
            double intens = Math.min(1.0, b.w / 8);
            gc.setFill(Color.rgb(0, (int) (200 + 55 * (1 - intens)), 255));
            if (b.w > 6)
                gc.fillRoundRect(b.x - b.w / 2, b.y, b.w, b.h, 4, 4);
            else
                gc.fillRect(b.x - b.w / 2, b.y, b.w, b.h);
            gc.setFill(Color.rgb(150, 230, 255, 0.5));
            double gs = Math.max(4, b.w);
            gc.fillOval(b.x - gs / 2, b.y - gs / 4, gs, gs / 2);
            gc.setEffect(null);
        }
    }

    private void updateBullets() {
        Iterator<Bullet> it = bullets.iterator();
        while (it.hasNext()) {
            Bullet b = it.next();
            if (b.vx != 0 || b.vy != 0) {
                b.x += b.vx;
                b.y += b.vy;
            } else {
                b.y -= b.speed;
            }
            if (b.y + b.h < 0 || b.y > H + 50)
                it.remove();
        }
    }

    // ================ 敌人 ================
    private void spawnEnemy() {
        double[][] types = { { 34, 34, 1, 1.2, 10 }, { 40, 40, 2, 0.8, 25 }, { 48, 48, 3, 0.6, 50 } };
        Color[] colors = { Color.rgb(255, 68, 102), Color.rgb(255, 136, 0), Color.rgb(204, 68, 255) };
        int maxType = 0;
        if (level >= 3)
            maxType = 1;
        if (level >= 5)
            maxType = 2;
        int ti = (int) (Math.random() * (maxType + 1));
        double[] t = types[ti];
        int hpAdd = level / 10;
        enemies.add(new Enemy(Math.random() * (W - 60) + 30, -30, t[0], t[1], (int) t[2] + hpAdd, (int) t[2],
                t[3] + (level - 1) * 0.08, colors[ti], (int) t[4], Math.random() * Math.PI * 2, ti));
    }

    private void drawEnemies() {
        for (Enemy e : enemies) {
            if (e.isEnemyBullet) {
                gc.setEffect(new javafx.scene.effect.DropShadow(12, 0, 0, Color.rgb(255, 0, 0, 0.7)));
                gc.setFill(Color.rgb(255, 50, 50));
                gc.fillOval(e.x - e.w / 2, e.y - e.h / 2, e.w, e.h);
                gc.setEffect(null);
                continue;
            }
            double cx = e.x, cy = e.y, hw = e.w / 2, hh = e.h / 2;
            gc.setEffect(new javafx.scene.effect.DropShadow(15, 0, 0, e.color));
            gc.setFill(Color.rgb((int) (e.color.getRed() * 255), (int) (e.color.getGreen() * 255),
                    (int) (e.color.getBlue() * 255), 0.2));
            gc.fillOval(cx - hw, cy - hh, e.w, e.h);
            gc.setFill(e.color);
            gc.fillOval(cx - hw * 0.7, cy - hh * 0.5 - 4, e.w * 0.7, e.h * 0.5);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(cx - hw * 0.7, cy - hh * 0.5 - 4, e.w * 0.7, e.h * 0.5);
            gc.setFill(Color.rgb(255, 255, 255, 0.15));
            gc.fillArc(cx - hw * 0.4, cy - hh * 0.35 - 6, e.w * 0.4, e.h * 0.35, 180, 180,
                    javafx.scene.shape.ArcType.OPEN);
            gc.setEffect(new javafx.scene.effect.DropShadow(8, 0, 0, Color.WHITE));
            for (int i = -1; i <= 1; i++) {
                gc.setFill(Color.rgb(255, 255, 255, 0.6));
                gc.fillOval(cx + i * 10 - 3, cy + 2 - 3, 6, 6);
            }
            gc.setEffect(null);
            if (e.hp < e.maxHp) {
                double barW = e.w + 10, barH = 4, bx = cx - barW / 2, by = cy - hh - 12;
                gc.setFill(Color.rgb(255, 0, 0, 0.3));
                gc.fillRect(bx, by, barW, barH);
                gc.setFill(Color.rgb(255, 68, 68));
                gc.fillRect(bx, by, barW * ((double) e.hp / e.maxHp), barH);
            }
        }
    }

    private void updateEnemies() {
        if (!pendingEnemyBullets.isEmpty()) {
            enemies.addAll(pendingEnemyBullets);
            pendingEnemyBullets.clear();
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            e.y += e.speed;
            if (!e.isEnemyBullet) {
                e.x += Math.sin(frame * 0.02 + e.wobblePhase) * 0.4;
                e.x = Math.max(e.w / 2 + 5, Math.min(W - e.w / 2 - 5, e.x));
            }
            if (!e.isEnemyBullet && e.y + e.h / 2 > H) {
                lives--;
                updateHUD();
                it.remove();
                if (lives <= 0)
                    endGame();
                continue;
            }
            if (e.isEnemyBullet && e.y > H + 20) {
                it.remove();
                continue;
            }
            if (!e.isEnemyBullet && player != null && rectCollide(e, player)) {
                if (player.invincible <= 0) {
                    lives--;
                    updateHUD();
                    player.invincible = 60;
                    spawnParticles(player.x, player.y, Color.rgb(0, 247, 255), 20);
                    it.remove();
                    if (lives <= 0)
                        endGame();
                    continue;
                }
            }
            if (e.isEnemyBullet && player != null && rectCollide(e, player)) {
                if (player.invincible <= 0) {
                    lives--;
                    updateHUD();
                    player.invincible = 60;
                    spawnParticles(player.x, player.y, Color.rgb(255, 50, 50), 15);
                    it.remove();
                    if (lives <= 0)
                        endGame();
                    continue;
                }
            }
            if (!e.isEnemyBullet && level >= 2 && Math.random() < 0.002 * Math.min(level, 30)) {
                Enemy eb = new Enemy(e.x + (Math.random() - 0.5) * 20, e.y + e.h / 2, 6, 6, 1, 1, 3.5,
                        Color.rgb(255, 50, 50), 0, 0, -1);
                eb.isEnemyBullet = true;
                pendingEnemyBullets.add(eb);
            }
        }
    }

    // ================ 碰撞 ================
    private boolean rectCollide(Entity a, Entity b) {
        return a.x - a.w / 2 < b.x + b.w / 2 && a.x + a.w / 2 > b.x - b.w / 2 && a.y - a.h / 2 < b.y + b.h / 2
                && a.y + a.h / 2 > b.y - b.h / 2;
    }

    private void checkCollisions() {
        Iterator<Bullet> bit = bullets.iterator();
        while (bit.hasNext()) {
            Bullet b = bit.next();
            Iterator<Enemy> eit = enemies.iterator();
            while (eit.hasNext()) {
                Enemy e = eit.next();
                if (e.isEnemyBullet)
                    continue;
                if (rectCollide(b, e)) {
                    e.hp -= b.damage;
                    spawnParticles(b.x, b.y, e.color, 8);
                    bit.remove();
                    if (b.splitLevel > 0) {
                        int sc = b.splitLevel + 1;
                        for (int i = 0; i < sc; i++) {
                            double angle = -Math.PI / 4 + (Math.PI / 2) * i / Math.max(1, sc - 1);
                            Bullet sb = new Bullet(b.x, b.y, b.w * 0.6, b.h * 0.6, b.speed + 2, 1);
                            sb.vx = Math.sin(angle) * 3;
                            sb.vy = -Math.cos(angle) * b.speed;
                            sb.splitLevel = b.splitLevel - 1;
                            bullets.add(sb);
                        }
                    }
                    if (e.hp <= 0) {
                        score += e.score;
                        if (score >= 9999 && !victory) {
                            victory = true;
                            score = 9999;
                            enemies.clear();
                            pendingEnemyBullets.clear();
                            bullets.clear();
                            spawnParticles(player.x, player.y - 20, Color.GOLD, 200);
                            spawnParticles(player.x, player.y - 20, Color.rgb(255, 215, 0), 150);
                            spawnParticles(player.x, player.y - 20, Color.rgb(255, 0, 255), 100);
                            updateHUD();
                            return;
                        }
                        enemiesKilled++;
                        spawnParticles(e.x, e.y, e.color, 25);
                        eit.remove();
                        updateHUD();
                        checkLevelUp();
                    }
                    break;
                }
            }
        }
    }

    // ================ 作弊模式 ================
    public void handleCheatInput(int digit) {
        cheatBuffer.append(digit);
        if (cheatBuffer.length() > 3) {
            cheatBuffer.deleteCharAt(0);
        }
        if (cheatBuffer.toString().equals("999")) {
            cheatBuffer.setLength(0);
            if (isRunning()) {
                Platform.runLater(() -> showCheatDialog());
            }
        }
    }

    private void showCheatDialog() {
        paused = true; // 作弊弹窗时暂停
        boolean wasAuto = autoFire;
        autoFire = false;

        String[] options = { "无限生命", "设置等级", "取消" };
        javafx.scene.control.ChoiceDialog<String> cd = new javafx.scene.control.ChoiceDialog<>("无限生命",
                Arrays.asList(options));
        cd.setTitle("🛡️ 开挂模式");
        cd.setHeaderText("输入 999 启动！");
        cd.setContentText("选择作弊项:");
        Optional<String> result = cd.showAndWait();
        if (result.isPresent()) {
            if (result.get().equals("无限生命")) {
                lives = 9999;
                updateHUD();
                showBoostMessage("🛡️ 无限生命 9999", 120);
                spawnParticles(player.x, player.y - 20, Color.GOLD, 60);
            } else if (result.get().equals("设置等级")) {
                TextInputDialog td = new TextInputDialog(String.valueOf(level));
                td.setTitle("设置等级");
                td.setHeaderText("输入目标等级（1-1000）");
                td.setContentText("等级:");
                Optional<String> lr = td.showAndWait();
                if (lr.isPresent()) {
                    try {
                        int newLv = Integer.parseInt(lr.get());
                        if (newLv >= 1 && newLv <= 1000) {
                            level = newLv;
                            enemiesKilled = (level - 1) * 10;
                            int boostCount = level / 5;
                            recalcBoosts(boostCount);
                            fireRate = Math.max(6, 12 - (level - 1) / 2);
                            enemySpawnRate = Math.max(20, 60 - (level - 1) * 5);
                            updateHUD();
                            showBoostMessage("⚡ 等级已设为 " + level, 120);
                            spawnParticles(player.x, player.y - 20, Color.rgb(255, 0, 255), 50);
                        }
                    } catch (Exception ex) {
                    }
                }
            }
        }
        autoFire = wasAuto;
        paused = false; // 作弊弹窗关闭后恢复
    }

    private void recalcBoosts(int totalBoostCount) {
        bulletSplitLevel = 0;
        bulletSizeMult = 1.0;
        extraShipCount = 0;
        for (int i = 1; i <= totalBoostCount; i++) {
            applyBoostByIndex(i - 1);
        }
    }

    private void applyBoostByIndex(int idx) {
        int type = idx % 3;
        switch (type) {
            case 0:
                bulletSplitLevel++;
                break;
            case 1:
                bulletSizeMult += 1.0;
                break;
            case 2:
                extraShipCount++;
                break;
        }
    }

    // ================ 粒子 ================
    private void spawnParticles(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double a = Math.random() * Math.PI * 2, sp = Math.random() * 5 + 2;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp, 30 + Math.random() * 30, 60,
                    Math.random() * 4 + 2, color));
        }
    }

    private void drawParticles() {
        for (Particle p : particles) {
            double a = p.life / p.maxLife;
            gc.setGlobalAlpha(a);
            gc.setEffect(new javafx.scene.effect.DropShadow(8, 0, 0, p.color));
            gc.setFill(p.color);
            gc.fillOval(p.x - p.size * a / 2, p.y - p.size * a / 2, p.size * a, p.size * a);
            gc.setEffect(null);
        }
        gc.setGlobalAlpha(1.0);
    }

    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.05;
            p.life--;
            if (p.life <= 0)
                it.remove();
        }
    }

    // ================ 等级 & 增益系统 ================
    private void checkLevelUp() {
        int newLevel = enemiesKilled / 10 + 1;
        if (newLevel > level) {
            level = newLevel;
            fireRate = Math.max(6, 12 - (level - 1) / 2);
            enemySpawnRate = Math.max(20, 60 - (level - 1) * 5);

            if (level % 5 == 0 && !pendingBoost) {
                pendingBoost = true;
                paused = true; // 暂停游戏，准备弹出增益选择
                int finalLevel = level;
                Platform.runLater(() -> showBoostChoiceDialog(finalLevel));
            }

            if (player != null) {
                spawnParticles(player.x, player.y - 20, Color.GOLD, 20);
                spawnParticles(player.x, player.y - 20, Color.rgb(0, 247, 255), 15);
            }
            updateHUD();
        }
    }

    // ================ 增益选择弹窗（暂停游戏） ================
    private void showBoostChoiceDialog(int currentLevel) {
        // 注意：此时 paused 已经被设置为 true，游戏循环已暂停
        String boostA = "💥 子弹分裂 Lv." + (bulletSplitLevel + 1);
        String boostB = "🔵 子弹大小 x" + String.format("%.1f", bulletSizeMult + 1.0);
        String boostC = "🛩️ 僚机 +2 架 (当前+" + (extraShipCount * 2) + ")";

        List<String> choices = Arrays.asList(boostA, boostB, boostC);
        ChoiceDialog<String> dialog = new ChoiceDialog<>(boostA, choices);
        dialog.setTitle("🎯 增益选择！");
        dialog.setHeaderText("🎉 恭喜达到 Lv." + currentLevel + "！选择一个增益：");
        dialog.setContentText("增益选项：");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            String choice = result.get();
            if (choice.equals(boostA)) {
                bulletSplitLevel++;
                showBoostMessage("💥 子弹分裂 Lv." + bulletSplitLevel, 150);
            } else if (choice.equals(boostB)) {
                bulletSizeMult += 1.0;
                showBoostMessage("🔵 子弹大小 x" + String.format("%.1f", bulletSizeMult), 150);
            } else {
                extraShipCount++;
                showBoostMessage("🛩️ 僚机 +2 架", 150);
            }
            if (player != null) {
                spawnParticles(player.x, player.y - 20, Color.GOLD, 50);
                spawnParticles(player.x, player.y - 20, Color.rgb(0, 247, 255), 30);
                spawnParticles(player.x, player.y - 20, Color.rgb(255, 0, 255), 20);
            }
        } else {
            // 取消则自动选第一个
            bulletSplitLevel++;
            showBoostMessage("💥 子弹分裂 Lv." + bulletSplitLevel, 150);
        }
        pendingBoost = false;
        paused = false; // 恢复游戏
    }

    private void showBoostMessage(String msg, int duration) {
        boostMessage = msg;
        boostMessageTimer = duration;
    }

    // ================ HUD ================
    private void updateHUD() {
    }

    private void drawHUD() {
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 18));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(10, 10, 150, 34, 8, 8);
        gc.setStroke(Color.rgb(0, 247, 255, 0.3));
        gc.setLineWidth(1);
        gc.strokeRoundRect(10, 10, 150, 34, 8, 8);
        gc.setFill(Color.WHITE);
        String lifeStr = lives >= 9999 ? "∞" : String.valueOf(lives);
        gc.fillText("❤ 生命: " + lifeStr, 20, 34);
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(170, 10, 150, 34, 8, 8);
        gc.setStroke(Color.rgb(0, 247, 255, 0.3));
        gc.strokeRoundRect(170, 10, 150, 34, 8, 8);
        gc.setFill(Color.WHITE);
        gc.fillText("🎯 得分: " + score, 180, 34);
        gc.setFill(Color.rgb(0, 0, 0, 0.5));
        gc.fillRoundRect(330, 10, 130, 34, 8, 8);
        gc.setStroke(Color.rgb(0, 247, 255, 0.3));
        gc.strokeRoundRect(330, 10, 130, 34, 8, 8);
        gc.setFill(Color.WHITE);
        gc.fillText("⚡ Lv." + level, 340, 34);

        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 13));
        gc.setFill(Color.rgb(0, 0, 0, 0.4));
        gc.fillRoundRect(10, 50, 420, 22, 6, 6);
        gc.setFill(Color.rgb(200, 200, 200));
        String info = "💥" + bulletSplitLevel + "  🔵" + String.format("%.1f", bulletSizeMult) + "  🛩️"
                + (extraShipCount * 2);
        gc.fillText("增益: " + info + "  |  生命: " + lives, 18, 66);
        int nextBoost = ((level / 5) + 1) * 5;
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.setFill(Color.rgb(0, 247, 255, 0.5));
        gc.fillText("下次增益选择: Lv." + nextBoost, W - 15, 66);

        if (boostMessageTimer > 0) {
            boostMessageTimer--;
            double a = Math.min(1.0, boostMessageTimer / 30.0);
            double sc = 1.0 + (1.0 - boostMessageTimer / 150.0) * 0.4;
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setGlobalAlpha(a);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 36 * sc));
            gc.setEffect(new javafx.scene.effect.DropShadow(25, 0, 0, Color.GOLD));
            gc.setFill(Color.GOLD);
            gc.fillText("✨ " + boostMessage + " ✨", W / 2, H / 2 - 50);
            gc.setEffect(new javafx.scene.effect.DropShadow(15, 0, 0, Color.rgb(0, 247, 255)));
            gc.setFill(Color.rgb(255, 255, 150));
            gc.fillText("✨ " + boostMessage + " ✨", W / 2, H / 2 - 50);
            gc.setGlobalAlpha(1.0);
            gc.setEffect(null);
        }

        if (autoFire) {
            gc.setTextAlign(TextAlignment.RIGHT);
            gc.setFill(Color.rgb(0, 247, 255, 0.6));
            gc.fillText("🔁 自动射击", W - 15, H - 15);
        }
    }

    // ================ 菜单 ================
    private void drawMenu() {
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRect(0, 0, W, H);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        gc.setFill(Color.rgb(0, 247, 255));
        gc.setEffect(new javafx.scene.effect.DropShadow(30, 0, 0, Color.rgb(0, 247, 255, 0.6)));
        gc.fillText("🚀 太空射击", W / 2, 210);
        gc.setEffect(null);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 16));
        gc.setFill(Color.rgb(170, 170, 170));
        gc.fillText("消灭入侵者的太空大战", W / 2, 245);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        gc.setFill(Color.GOLD);
        gc.fillText("🏆 每10级获得一次强力增益!", W / 2, 280);
        gc.setFill(Color.rgb(180, 180, 180));
        gc.fillText("💥 子弹分裂  |  🔵 子弹变大  |  🛩️ 增加僚机", W / 2, 302);
        double bx = W / 2 - 120, by = 330, bw = 240, bh = 56;
        gc.setFill(Color.rgb(0, 198, 255));
        gc.fillRoundRect(bx, by, bw, bh, 28, 28);
        gc.setFill(Color.rgb(0, 114, 255));
        gc.fillRoundRect(bx + 2, by + 2, bw - 4, bh - 4, 26, 26);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        gc.setFill(Color.WHITE);
        gc.fillText("开始游戏", W / 2, by + 36);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 14));
        gc.setFill(Color.rgb(136, 136, 136));
        gc.fillText("← → 或 A D 移动  |  空格射击  |  F 切换自动射击", W / 2, 450);
        gc.fillText("鼠标移动也能控制飞机位置", W / 2, 472);
    }

    private void drawGameOver() {
        gc.setFill(Color.rgb(0, 0, 0, 0.75));
        gc.fillRect(0, 0, W, H);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 52));
        gc.setFill(Color.rgb(0, 247, 255));
        gc.setEffect(new javafx.scene.effect.DropShadow(30, 0, 0, Color.rgb(0, 247, 255, 0.6)));
        gc.fillText("💥 游戏结束", W / 2, 200);
        gc.setEffect(null);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 28));
        gc.setFill(Color.WHITE);
        gc.fillText("最终得分: ", W / 2, 260);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 50));
        gc.setFill(Color.GOLD);
        gc.fillText(String.valueOf(score), W / 2, 320);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 18));
        gc.setFill(Color.rgb(180, 180, 180));
        gc.fillText("到达 Lv." + level + "  |  获得增益 " + ((level - 1) / 10) + " 次", W / 2, 358);
        double bx = W / 2 - 120, by = 390, bw = 240, bh = 56;
        gc.setFill(Color.rgb(0, 198, 255));
        gc.fillRoundRect(bx, by, bw, bh, 28, 28);
        gc.setFill(Color.rgb(0, 114, 255));
        gc.fillRoundRect(bx + 2, by + 2, bw - 4, bh - 4, 26, 26);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        gc.setFill(Color.WHITE);
        gc.fillText("再来一局", W / 2, by + 36);
    }

    // ================ 胜利画面 ================
    private void drawVictoryScreen() {
        gc.setFill(Color.rgb(0, 0, 0, 0.7));
        gc.fillRect(0, 0, W, H);

        double glowSize = (Math.sin(frame * 0.08) * 0.3 + 0.7) * 40;
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 56));
        gc.setEffect(new javafx.scene.effect.DropShadow(glowSize, 0, 0, Color.rgb(255, 215, 0, 0.9)));
        gc.setFill(Color.GOLD);
        gc.fillText("🏆 胜利！ 🏆", W / 2, 190);
        gc.setEffect(new javafx.scene.effect.DropShadow(20, 0, 0, Color.WHITE));
        gc.setFill(Color.rgb(255, 255, 200));
        gc.fillText("🏆 胜利！ 🏆", W / 2, 190);
        gc.setEffect(null);

        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 28));
        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        gc.fillText("你成功达到了 9999 分！", W / 2, 250);
        gc.fillText("你拯救了银河系！🌟🌟🌟", W / 2, 290);

        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 42));
        gc.setFill(Color.GOLD);
        gc.fillText("最终得分: 9999", W / 2, 350);
        gc.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 22));
        gc.setFill(Color.rgb(180, 180, 255));
        gc.fillText("到达 Lv." + level + "  |  " + extraShipCount + " 架僚机  |  子弹分裂 " + bulletSplitLevel + " 级", W / 2,
                390);

        double bx = W / 2 - 120, by = 420, bw = 240, bh = 56;
        gc.setFill(Color.rgb(255, 215, 0));
        gc.fillRoundRect(bx, by, bw, bh, 28, 28);
        gc.setFill(Color.rgb(200, 170, 0));
        gc.fillRoundRect(bx + 2, by + 2, bw - 4, bh - 4, 26, 26);
        gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        gc.setFill(Color.WHITE);
        gc.fillText("再来一局", W / 2, by + 36);

        gc.setFill(Color.rgb(255, 215, 0, 0.4 + 0.3 * Math.sin(frame * 0.1)));
        for (int i = 0; i < 20; i++) {
            double sx = W * (0.5 + 0.5 * Math.sin(frame * 0.02 + i * 2.1));
            double sy = H * (0.5 + 0.5 * Math.cos(frame * 0.025 + i * 1.7));
            double sz = 3 + Math.sin(frame * 0.05 + i * 3.0) * 2;
            gc.fillOval(sx - sz / 2, sy - sz / 2, sz, sz);
        }
    }

    // ================ 主循环 ================
    private void render() {
        gc.setFill(Color.rgb(15, 15, 58));
        gc.fillRect(0, 0, W, H);
        gc.setStroke(Color.rgb(0, 247, 255, 0.03));
        gc.setLineWidth(1);
        for (double x = 0; x < W; x += 40)
            gc.strokeLine(x, 0, x, H);
        for (double y = 0; y < H; y += 40)
            gc.strokeLine(0, y, W, y);
        drawStars();
        if (showMenu) {
            drawMenu();
            return;
        }
        if (victory) {
            drawStars();
            drawVictoryScreen();
            return;
        }
        if (gameOver) {
            drawStars();
            drawGameOver();
            return;
        }
        // 暂停时绘制游戏画面 + 暂停提示
        if (paused) {
            // 绘制半透明遮罩和暂停文字
            gc.setFill(Color.rgb(0, 0, 0, 0.4));
            gc.fillRect(0, 0, W, H);
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setFont(Font.font("Segoe UI", FontWeight.BOLD, 44));
            gc.setEffect(new javafx.scene.effect.DropShadow(25, 0, 0, Color.GOLD));
            gc.setFill(Color.GOLD);
            gc.fillText("⏸️ 暂停中...", W / 2, H / 2);
            gc.setEffect(null);
            return;
        }
        drawParticles();
        drawBullets();
        drawEnemies();
        drawPlayer();
        drawHUD();
    }

    private void update() {
        if (paused || showMenu || gameOver || victory)
            return;
        frame++;
        updateStars();
        updatePlayer();
        spawnCounter++;
        if (spawnCounter >= enemySpawnRate) {
            spawnCounter = 0;
            spawnEnemy();
            if (level >= 4 && Math.random() < 0.3)
                spawnEnemy();
            if (level >= 7 && Math.random() < 0.3)
                spawnEnemy();
        }
        if (autoFire || spacePressed)
            fireBullet();
        updateBullets();
        updateEnemies();
        checkCollisions();
        updateParticles();
    }

    public void startLoop() {
        loop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
                render();
            }
        };
        loop.start();
    }

    public void stopLoop() {
        if (loop != null)
            loop.stop();
    }

    public void startGame() {
        if (showMenu || gameOver || victory) {
            resetGame();
            showMenu = false;
            gameOver = false;
            victory = false;
        }
    }

    private void resetGame() {
        score = 0;
        lives = 100;
        level = 1;
        frame = 0;
        autoFire = false;
        victory = false;
        paused = false; // 重置暂停标志
        lastShotFrame = 0;
        fireRate = 12;
        enemySpawnRate = 60;
        spawnCounter = 0;
        enemiesKilled = 0;
        killsForNextLevel = 10;
        killsToNextLevel = 10;
        bulletSplitLevel = 0;
        bulletSizeMult = 1.0;
        extraShipCount = 0;
        pendingBoost = false;
        boostMessage = "";
        boostMessageTimer = 0;
        cheatBuffer.setLength(0);
        player = null;
        bullets.clear();
        enemies.clear();
        particles.clear();
        pendingEnemyBullets.clear();
        createPlayer();
        initStars();
    }

    private void endGame() {
        running = false;
        gameOver = true;
        paused = false; // 确保游戏结束时不处于暂停状态
    }

    public boolean isRunning() {
        return !showMenu && !gameOver && !victory;
    }

    public void pressLeft(boolean v) {
        leftPressed = v;
    }

    public void pressRight(boolean v) {
        rightPressed = v;
    }

    public void pressSpace(boolean v) {
        spacePressed = v;
        if (v && isRunning())
            fireBullet();
    }

    public void toggleAutoFire() {
        autoFire = !autoFire;
    }

    public void mouseMove(double x) {
        if (player != null && isRunning())
            player.x = Math.max(24, Math.min(W - 24, x));
    }

    public void handleKeyTyped(char c) {
        if (c >= '0' && c <= '9') {
            handleCheatInput(c - '0');
        }
    }

    // ================ 实体类 ================
    static class Entity {
        double x, y, w, h;

        Entity(double x, double y, double w, double h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }
    }

    static class Player extends Entity {
        double speed;
        int invincible = 0;

        Player(double x, double y, double w, double h, double speed) {
            super(x, y, w, h);
            this.speed = speed;
        }
    }

    static class Bullet extends Entity {
        double speed;
        int damage;
        int splitLevel = 0;
        double vx = 0, vy = 0;

        Bullet(double x, double y, double w, double h, double speed, int damage) {
            super(x, y, w, h);
            this.speed = speed;
            this.damage = damage;
        }
    }

    static class Enemy extends Entity {
        int hp, maxHp;
        double speed;
        Color color;
        int score;
        double wobblePhase;
        int type;
        boolean isEnemyBullet = false;

        Enemy(double x, double y, double w, double h, int hp, int maxHp, double speed, Color color, int score,
                double wobblePhase, int type) {
            super(x, y, w, h);
            this.hp = hp;
            this.maxHp = maxHp;
            this.speed = speed;
            this.color = color;
            this.score = score;
            this.wobblePhase = wobblePhase;
            this.type = type;
        }
    }

    static class Particle extends Entity {
        double vx, vy, life, maxLife, size;
        Color color;

        Particle(double x, double y, double vx, double vy, double life, double maxLife, double size, Color color) {
            super(x, y, 0, 0);
            this.vx = vx;
            this.vy = vy;
            this.life = life;
            this.maxLife = maxLife;
            this.size = size;
            this.color = color;
        }
    }

    static class Star {
        double x, y, size, speed, brightness;

        Star(double x, double y, double size, double speed, double brightness) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speed = speed;
            this.brightness = brightness;
        }
    }
}
