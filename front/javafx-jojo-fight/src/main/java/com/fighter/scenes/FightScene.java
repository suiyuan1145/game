package com.fighter.scenes;

import com.fighter.App;
import com.fighter.model.*;
import com.fighter.util.SpriteLoader;
import com.fighter.util.AudioManager;
import com.fighter.util.ResponsiveView;
import com.fighter.mugen.MugenCatalog;
import com.fighter.mugen.MugenStageInfo;
import com.fighter.mugen.MugenCharacterInfo;
import javafx.animation.AnimationTimer;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import java.util.*;

/**
 * 对战场景 —— MUGEN近战攻击+集气技能；仅DIO使用飞刀
 * 
 * 【修改记录】
 * v6.2 修复：跳跃高度、空投位置、按住射击速度
 * 
 * P1: W跳跃 A左 D右  J攻击 K集气(按住) L技能
 * P2: ↑跳跃 ←左 →右  1攻击 2集气(按住) 3技能
 * 
 * @author 课程设计
 * @version 6.2
 */
public class FightScene {

    private final App app;
    private final Fighter.CharType p1Char, p2Char;
    private final boolean singlePlayer;
    private Fighter p1, p2;

    private final double W = 960, H = 600, GY = 540, LB = 15, RB = 945;
    private final double[][] platforms;
    private final String mapName;
    private final Color[] palette; // [skyR,skyG,skyB, ground, groundLine, platformFill, platformStroke]
    private final Image stageBackground;
    private final Image dioKnifeImage = SpriteLoader.loadImage("projectiles/dio-knife.png");
    private final MugenStageInfo mugenStage;
    private final MugenCharacterInfo p1Mugen, p2Mugen;
    private static final String[] STAGE_IMAGES = {
        "mugen/stages/22-dio-s-mansion.png",
        "mugen/stages/36-final-battle-under-the-red-moonlight.png",
        "mugen/stages/47-italy-stage.png",
        "mugen/stages/50-kennedy-space-center.png"
    };

    // ---- 地图预设 ----
    private static final Random mapRnd = new Random();
    private static Object[][] MAPS;

    static {
        MAPS = new Object[][]{
            // 0: 星夜都市(默认)
            new Object[]{"星夜都市", new double[][]{{440,80,300},{440,650,870},{380,350,600}},
                new Color[]{Color.rgb(15,18,50),Color.rgb(30,35,70),Color.rgb(40,40,60),Color.rgb(60,60,90),
                            Color.rgb(60,60,100),Color.rgb(80,80,120)}},
            // 1: 熔岩洞穴
            new Object[]{"熔岩洞穴", new double[][]{{450,50,220},{430,300,500},{470,580,800},{390,700,900}},
                new Color[]{Color.rgb(60,10,10),Color.rgb(100,30,15),Color.rgb(80,25,20),Color.rgb(120,60,40),
                            Color.rgb(140,50,30),Color.rgb(180,90,50)}},
            // 2: 翠绿森林
            new Object[]{"翠绿森林", new double[][]{{460,100,350},{420,400,650},{450,680,900},{370,200,500}},
                new Color[]{Color.rgb(10,40,15),Color.rgb(25,65,30),Color.rgb(30,55,25),Color.rgb(50,80,40),
                            Color.rgb(40,75,35),Color.rgb(60,100,50)}},
            // 3: 电子都市(霓虹)
            new Object[]{"电子都市", new double[][]{{430,60,280},{450,500,700},{380,200,500},{410,720,920}},
                new Color[]{Color.rgb(10,5,30),Color.rgb(30,10,50),Color.rgb(20,15,40),Color.rgb(60,40,80),
                            Color.rgb(80,30,90),Color.rgb(100,60,120)}},
        };
    }

    private static double[][] pickPlatforms(int idx) { return (double[][]) MAPS[idx % MAPS.length][1]; }
    private static String pickName(int idx) { return (String) MAPS[idx % MAPS.length][0]; }
    private static Color[] pickPalette(int idx) { return (Color[]) MAPS[idx % MAPS.length][2]; }

    // ---- 空投(从天而降) ----
    private int airdropTimer = 0;
    private static final int AIRDROP_INTERVAL = 900; // 15秒
    private final List<FallingAirdrop> airdrops = new ArrayList<>();
    private static final Weapon.Type[] AIRDROP_GUNS = {
        Weapon.Type.SHOTGUN, Weapon.Type.SMG, Weapon.Type.RIFLE, Weapon.Type.SNIPER
    };
    private final Random rnd = new Random();

    // ---- 比赛 ----
    private boolean gameOver = false;
    private String winner = "";

    // ---- 输入 & 冷却 ----
    private final Set<KeyCode> keys = new HashSet<>();
    private boolean p1ShootWasHeld = false, p2ShootWasHeld = false;
    private boolean p1SkillWasHeld = false, p2SkillWasHeld = false;

    // ---- 固定60fps步进(防止高刷屏速度翻倍) ----
    private long lastFrameTime = 0;
    private static final long FRAME_NANOS = 16_666_667L; // ≈60fps

    // ---- 技能抛射物 ----
    private final List<SkillThrowable> skillThrows = new ArrayList<>();

    // ---- 时间停止(黑方技能) ----
    private boolean timeStopped = false;
    private int timeStopTimer = 0;
    private boolean timeStopFromP1 = true; // 谁释放的

    // ---- 特效 ----
    private final List<HitEffect> effects = new ArrayList<>();
    private final List<FloatingText> floatTexts = new ArrayList<>();
    private final List<GroundEffect> groundEffects = new ArrayList<>();
    private final List<SmokeCloud> smokeClouds = new ArrayList<>(); // 烟雾弹(跟随释放者)

    // ---- AnimationTimer 引用（防止重启时旧timer泄漏） ----
    private AnimationTimer animationTimer;

    public FightScene(App app, Fighter.CharType p1, Fighter.CharType p2, App.GameMode gameMode, MugenStageInfo selectedStage,
                      MugenCharacterInfo p1Mugen, MugenCharacterInfo p2Mugen) {
        this.app = app; this.p1Char = p1; this.p2Char = p2;
        this.singlePlayer = gameMode == App.GameMode.SINGLE_PLAYER;
        this.p1Mugen = p1Mugen; this.p2Mugen = p2Mugen;
        // 随机选地图
        int mapIdx = mapRnd.nextInt(MAPS.length);
        this.platforms = pickPlatforms(mapIdx);
        this.mugenStage = selectedStage;
        this.mapName = mugenStage == null ? pickName(mapIdx) : mugenStage.name();
        this.palette = pickPalette(mapIdx);
        String preview = mugenStage == null ? STAGE_IMAGES[mapIdx % STAGE_IMAGES.length]
                : MugenCatalog.getInstance().previewFor(mugenStage, mapIdx);
        this.stageBackground = SpriteLoader.loadImage(preview);
    }

    public Scene create() {
        if (mugenStage != null && mugenStage.musicFile() != null
                && java.nio.file.Files.isRegularFile(mugenStage.musicFile())) {
            AudioManager.getInstance().playFile(mugenStage.musicFile());
        } else {
            AudioManager.getInstance().playBGM("vs");
        }
        p1 = new Fighter(p1Char, true, MugenCatalog.getInstance().runtimeProfileFor(p1Mugen)); p1.setX(180); p1.setY(GY - p1.height);
        p2 = new Fighter(p2Char, false, MugenCatalog.getInstance().runtimeProfileFor(p2Mugen)); p2.setX(720); p2.setY(GY - p2.height);

        Canvas cv = new Canvas(W, H);
        GraphicsContext gc = cv.getGraphicsContext2D();
        StackPane root = ResponsiveView.wrap(cv, W, H);
        Scene scene = new Scene(root, W, H);

        // ---- 键盘按下 ---
        scene.setOnKeyPressed(e -> {
            keys.add(e.getCode()); // 记录持续按键(用于帧级检测)
            // 结算: 重新开始/返回菜单前先停止旧timer防止内存泄漏
            if (gameOver && (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE)) {
                if (animationTimer != null) animationTimer.stop();
                app.restartFight();
            }
            if (e.getCode() == KeyCode.ESCAPE) {
                if (animationTimer != null) animationTimer.stop();
                app.showScreen(App.GameScreen.MENU);
            }
        });
        scene.setOnKeyReleased(e -> keys.remove(e.getCode()));

        animationTimer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastFrameTime == 0) lastFrameTime = now;
                if (now - lastFrameTime >= FRAME_NANOS) {
                    lastFrameTime += FRAME_NANOS; // 固定每次前进1帧
                    if (!gameOver) update();
                }
                draw(gc); // 画面依然每帧刷新(平滑)
            }
        };
        animationTimer.start();
        return scene;
    }

    // ====================================================================
    // 更新
    // ====================================================================

    private void update() {
        // 时间停止处理
        if (timeStopped) {
            handleTimeStopInput();
            timeStopTimer--;
            // 只有释放者可以更新
            if (timeStopFromP1) {
                p1.update(GY, LB, RB, platforms);
            } else {
                p2.update(GY, LB, RB, platforms);
            }
            // 特效照常更新
            updateEffects();
            updateFloatTexts();
            if (timeStopTimer <= 0) {
                timeStopped = false;
                freezeAllProjectiles(false);
                for (SkillThrowable st : skillThrows) st.frozen = false;
            }
            return;
        }
        handleInput();
        updateFighters();
        checkBurnDamage();
        updateCollision();
        updateSkillThrows();
        updateSmokeClouds();
        updateGroundEffects();
        updateEffects();
        updateFloatTexts();
        checkDeath();
    }

    /** 时间停止期间: 只处理释放者输入 */
    private void handleTimeStopInput() {
        if (timeStopFromP1) {
            // P1(黑方)可以移动和射击, 不能集气
            if (keys.contains(KeyCode.W)) p1.jump();
            if (keys.contains(KeyCode.A)) p1.walk(-1);
            else if (keys.contains(KeyCode.D)) p1.walk(1);
            else if (p1.getState() == Fighter.State.WALKING) p1.setIdle();
            if (keys.contains(KeyCode.S)) p1.requestDrop();
            // 时间停止期间不可集气
            p1.stopCharge();
            boolean p1JNow = keys.contains(KeyCode.J);
            if (p1JNow && !p1ShootWasHeld) {
                if (p1.isDio() && p1.canShoot()) p1.shoot(); else p1.attack();
                // 新射出的子弹也冻结
                for (Projectile b : p1.getBullets()) b.setFrozen(true);
            }
            p1ShootWasHeld = p1JNow;
        } else {
            if (singlePlayer) {
                handleAiInput();
                return;
            }
            // P2(黑方)可以移动和射击, 不能集气
            if (keys.contains(KeyCode.UP) || keys.contains(KeyCode.NUMPAD8)) p2.jump();
            if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.NUMPAD4)) p2.walk(-1);
            else if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.NUMPAD6)) p2.walk(1);
            else if (p2.getState() == Fighter.State.WALKING) p2.setIdle();
            if (keys.contains(KeyCode.DOWN)) p2.requestDrop();
            p2.stopCharge();
            boolean p2OneNow = keys.contains(KeyCode.NUMPAD1) || keys.contains(KeyCode.DIGIT1);
            if (p2OneNow && !p2ShootWasHeld) {
                if (p2.isDio() && p2.canShoot()) p2.shoot(); else p2.attack();
                for (Projectile b : p2.getBullets()) b.setFrozen(true);
            }
            p2ShootWasHeld = p2OneNow;
        }
    }

    /**
     * 输入处理
     * 移动/集气: 按住持续
     * 射击: 边缘检测(按一下射一发) + 场景冷却(防止超速连点)
     * 技能: 边缘检测(按一下扔一个) + 抛射落地
     */
    private void handleInput() {
        if (gameOver) return;

        // ---- P1 ----
        if (keys.contains(KeyCode.W)) p1.jump();
        if (keys.contains(KeyCode.A)) p1.walk(-1);
        else if (keys.contains(KeyCode.D)) p1.walk(1);
        else if (p1.getState() == Fighter.State.WALKING) p1.setIdle();
        if (keys.contains(KeyCode.S)) p1.requestDrop(); // 下平台
        if (keys.contains(KeyCode.K)) p1.startCharge(); else p1.stopCharge();

        // 普通角色近战；只有 DIO 发射飞刀。
        boolean p1JNow = keys.contains(KeyCode.J);
        if (p1JNow && !p1ShootWasHeld) {
            if (p1.isDio() && p1.canShoot()) p1.shoot(); else p1.attack();
        }
        p1ShootWasHeld = p1JNow;

        // 技能: 边缘检测(按一下扔一个)
        boolean p1LNow = keys.contains(KeyCode.L);
        if (p1LNow && !p1SkillWasHeld) throwSkill(p1);
        p1SkillWasHeld = p1LNow;

        if (singlePlayer) {
            handleAiInput();
            return;
        }

        // ---- P2 ----
        if (keys.contains(KeyCode.UP) || keys.contains(KeyCode.NUMPAD8)) p2.jump();
        if (keys.contains(KeyCode.LEFT) || keys.contains(KeyCode.NUMPAD4)) p2.walk(-1);
        else if (keys.contains(KeyCode.RIGHT) || keys.contains(KeyCode.NUMPAD6)) p2.walk(1);
        else if (p2.getState() == Fighter.State.WALKING) p2.setIdle();
        if (keys.contains(KeyCode.DOWN)) p2.requestDrop(); // 下平台(仅方向键↓)
        if (keys.contains(KeyCode.NUMPAD2) || keys.contains(KeyCode.DIGIT2)) p2.startCharge(); else p2.stopCharge();

        boolean p2OneNow = keys.contains(KeyCode.NUMPAD1) || keys.contains(KeyCode.DIGIT1);
        if (p2OneNow && !p2ShootWasHeld) {
            if (p2.isDio() && p2.canShoot()) p2.shoot(); else p2.attack();
        }
        p2ShootWasHeld = p2OneNow;

        boolean p2ThreeNow = keys.contains(KeyCode.NUMPAD3) || keys.contains(KeyCode.DIGIT3);
        if (p2ThreeNow && !p2SkillWasHeld) throwSkill(p2);
        p2SkillWasHeld = p2ThreeNow;
    }

    private void handleAiInput() {
        double dx = p1.getX() - p2.getX();
        double distance = Math.abs(dx);

        if (distance > 260) p2.walk(Math.signum(dx));
        else if (distance < 105) p2.walk(-Math.signum(dx));
        else if (p2.getState() == Fighter.State.WALKING) p2.setIdle();

        if (p1.getY() + 35 < p2.getY() && p2.isOnGround() && rnd.nextDouble() < 0.04) p2.jump();
        if (p2.getCharge() >= p2.getMaxCharge() && p2.getSkillCooldown() == 0) {
            p2.stopCharge();
            throwSkill(p2);
        } else if (distance > 180 && p2.getCharge() < p2.getMaxCharge() && rnd.nextDouble() < 0.08) {
            p2.startCharge();
        } else {
            p2.stopCharge();
        }

        if (p2.isDio()) {
            if (distance < 560 && p2.canShoot() && rnd.nextDouble() < 0.075) p2.shoot();
        } else if (distance < 96 && rnd.nextDouble() < 0.12) {
            p2.attack();
        }
    }

    /** 创建技能(抛射物或立即生效) */
    private void throwSkill(Fighter f) {
        Fighter.SkillEffect.Type type = f.useSkill();
        if (type == null) return;
        boolean fromP1 = (f == p1);
        switch (type) {
            case SMOKE -> {
                // 烟雾弹: 原地释放, 不跟随移动
                smokeClouds.add(new SmokeCloud(f.getX() + f.width/2, f.getY() + f.height/2, 300, fromP1));
                // 给自己加烟雾Buff(减伤50%) — 用专用方法, checkSkillHit只设noCharge
                f.applySmokeBuff();
            }
            case HEAL -> {
                // 绿方: 自疗, 每秒20HP×3秒
                f.startHealing();
            }
            case INVINCIBLE -> {
                // 粉方: 无敌3秒
                f.startInvincible();
            }
            case TIMESTOP -> {
                // fight 旧版黑方技能: 时间停止2.5秒。
                timeStopped = true;
                timeStopTimer = 150;
                timeStopFromP1 = fromP1;
                // 冻结所有子弹
                freezeAllProjectiles(true);
                // 冻结所有技能抛射物
                for (SkillThrowable st : skillThrows) st.frozen = true;
            }
            default -> {
                // 其他技能: 抛射物朝目标飞出
                Fighter target = fromP1 ? p2 : p1;
                double sx = f.getX() + (f.isFacingRight() ? f.width + 5 : -20);
                double sy = f.getY() + 20;
                double tx = target.getX() + target.width/2;
                double ty = target.getY() + target.height/2 - 30;
                double dx = tx - sx;
                double dy = ty - sy;
                double dist = Math.sqrt(dx*dx + dy*dy);
                double speed = 6.5;
                double vx = dx / dist * speed;
                double vy = dy / dist * speed - 1.5;
                skillThrows.add(new SkillThrowable(sx, sy, vx, vy, type, fromP1));
            }
        }
    }

    /** 冻结/解冻所有子弹(双方) */
    private void freezeAllProjectiles(boolean freeze) {
        for (Projectile b : p1.getBullets()) b.setFrozen(freeze);
        for (Projectile b : p2.getBullets()) b.setFrozen(freeze);
    }

    private void updateFighters() {
        p1.update(GY, LB, RB, platforms);
        p2.update(GY, LB, RB, platforms);
    }

    private void updateCollision() {
        hitCheck(p1, p2); hitCheck(p2, p1);
    }

    private void hitCheck(Fighter shooter, Fighter target) {
        if (shooter.checkMeleeHit(target)) {
            addSpark(target.getX() + target.width/2, target.getY() + 30, shooter.charType.body);
        }
        if (shooter.checkBulletHit(target) > 0) {
            addSpark(target.getX() + target.width/2, target.getY() + 25, shooter.getBulletColor());
        }
    }

    /** 技能抛射物更新 —— 飞出 → 落地/出屏 → 爆炸产生效果 + 地面区域 */
    private void updateSkillThrows() {
        for (SkillThrowable st : skillThrows) st.update();
        for (SkillThrowable st : skillThrows) {
            if (!st.exploded || st.effectApplied) continue;
            // 抛射物落地爆炸, 在落地位置生成技能效果
            Fighter target = st.fromP1 ? p2 : p1;
            Fighter.SkillEffect se = new Fighter.SkillEffect(st.type, st.x, st.y);
            target.checkSkillHit(se);
            // 添加地面持续区域效果
            Color zoneColor = switch (st.type) {
                case NITROGEN -> Color.rgb(150, 220, 255, 0.25);
                case FIRE -> Color.rgb(255, 100, 0, 0.3);
                case EXPLOSION -> Color.rgb(255, 200, 0, 0.35);
                case SMOKE -> Color.rgb(200, 200, 220, 0.2);
                default -> Color.TRANSPARENT;
            };
            int zoneRadius = switch (st.type) {
                case NITROGEN -> 140;
                case FIRE -> 70;
                case EXPLOSION -> 50;
                case SMOKE -> 55;
                default -> 0;
            };
            int zoneDuration = switch (st.type) {
                case NITROGEN -> 240;  // 4秒
                case FIRE -> 600;      // 10秒
                case EXPLOSION -> 20;  // 一次性
                case SMOKE -> 300;     // 5秒
                default -> 0;
            };
            if (zoneDuration > 0) {
                groundEffects.add(new GroundEffect(st.x, st.y, zoneRadius, zoneDuration, zoneColor));
            }
            // 添加爆炸特效粒子
            Color boom = switch (st.type) {
                case NITROGEN -> Color.rgb(150, 220, 255);
                case FIRE -> Color.ORANGE;
                case EXPLOSION -> Color.RED;
                case SMOKE -> Color.rgb(200, 200, 220);
                default -> Color.WHITE;
            };
            for (int i = 0; i < 10; i++) {
                effects.add(new HitEffect(
                    st.x + rnd.nextDouble()*20-10, st.y + rnd.nextDouble()*20-10,
                    rnd.nextDouble()*4-2, rnd.nextDouble()*4-3, 4+rnd.nextInt(8), boom));
            }
            st.effectApplied = true;
        }
        skillThrows.removeIf(st -> st.exploded && st.effectApplied);
    }

    /** 烟雾云更新(原地固定, 检测敌人) */
    private void updateSmokeClouds() {
        int smokeRadius = 140;
        for (SmokeCloud sc : smokeClouds) {
            sc.life--;
            if (sc.life <= 0) { sc.active = false; continue; }
            // 只给对手加noCharge, 释放者自己已通过applySmokeBuff拿了减伤
            Fighter enemy = sc.fromP1 ? p2 : p1;
            boolean inSmoke = Math.abs(enemy.getX() + enemy.width/2 - sc.x) < smokeRadius
                && Math.abs(enemy.getY() + enemy.height/2 - sc.y) < smokeRadius;
            if (inSmoke) enemy.checkSkillHit(new Fighter.SkillEffect(Fighter.SkillEffect.Type.SMOKE, sc.x, sc.y));
        }
        smokeClouds.removeIf(sc -> !sc.active);
    }

    /** 地面效果更新(技能落点区域) */
    private void updateGroundEffects() {
        for (GroundEffect g : groundEffects) g.life--;
        groundEffects.removeIf(g -> g.life <= 0);
    }

    /** 空投从天而降 */
    private void updateAirdrops() {
        airdropTimer++;
        if (airdropTimer >= AIRDROP_INTERVAL) {
            airdropTimer = 0;
            // 随机X位置(避开边缘)
            double lx = 60 + rnd.nextDouble() * 820;
            // 从天空掉落
            airdrops.add(new FallingAirdrop(lx, -50, GY, AIRDROP_GUNS[rnd.nextInt(AIRDROP_GUNS.length)]));
        }
        airdrops.removeIf(a -> !a.active);
        for (FallingAirdrop a : airdrops) a.update();
    }

    private void checkDeath() {
        if (!p1.hasLivesLeft()) { gameOver = true; winner = fighterName(p2Mugen, p2Char) + " 获胜!"; p1.setState(Fighter.State.LOSE); p2.setState(Fighter.State.WIN); }
        if (!p2.hasLivesLeft()) { gameOver = true; winner = fighterName(p1Mugen, p1Char) + " 获胜!"; p2.setState(Fighter.State.LOSE); p1.setState(Fighter.State.WIN); }
    }

    // ====================================================================
    // 特效
    // ====================================================================

    private void addSpark(double x, double y, Color c) {
        for (int i = 0; i < 6; i++) effects.add(new HitEffect(
                x+rnd.nextDouble()*6-3, y+rnd.nextDouble()*6-3,
                rnd.nextDouble()*3-1.5, rnd.nextDouble()*3-1.5-1, 4+rnd.nextInt(6), c));
    }

    private void updateEffects() {
        effects.removeIf(e -> e.life <= 0);
        for (HitEffect e : effects) { e.life--; e.x += e.vx; e.y += e.vy; }
    }

    /** 检测灼烧伤害并显示浮动数字 */
    private void checkBurnDamage() {
        checkBurnFor(p1);
        checkBurnFor(p2);
    }
    private void checkBurnFor(Fighter f) {
        if (f.burnDamageDealt) {
            f.burnDamageDealt = false;
            floatTexts.add(new FloatingText(
                f.getX() + f.width/2, f.getY() - 10, "-2", Color.ORANGE));
        }
    }

    /** 更新浮动文字 */
    private void updateFloatTexts() {
        floatTexts.removeIf(t -> t.life <= 0);
        for (FloatingText t : floatTexts) { t.life--; t.y -= 1.5; }
    }

    // ====================================================================
    // 绘制
    // ====================================================================

    private void draw(GraphicsContext gc) {
        drawBackground(gc);
        drawPlatforms(gc);

        // 地面效果(技能落地区域)
        for (GroundEffect g : groundEffects) {
            gc.setGlobalAlpha((double)g.life / g.totalLife * 0.8 + 0.2);
            gc.setFill(g.color); gc.fillOval(g.x - g.radius, g.y - g.radius, g.radius*2, g.radius*2);
        }
        gc.setGlobalAlpha(1.0);

        // 技能抛射物(飞行中的)
        for (SkillThrowable st : skillThrows) st.draw(gc);

        // 烟雾云(跟随释放者)
        for (SmokeCloud sc : smokeClouds) {
            double alpha = (double)sc.life / sc.maxLife * 0.35 + 0.1;
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.rgb(190, 190, 210));
            gc.fillOval(sc.x - 140, sc.y - 140, 280, 280);
            gc.setFill(Color.rgb(210, 210, 230, 0.5));
            gc.fillOval(sc.x - 100, sc.y - 100, 200, 200);
        }
        gc.setGlobalAlpha(1.0);

        drawDioKnives(gc, p1);
        drawDioKnives(gc, p2);

        // 角色
        p1.draw(gc, 0, GY); p2.draw(gc, 0, GY);

        // 特效粒子
        for (HitEffect e : effects) { gc.setGlobalAlpha(e.life/12.0); gc.setFill(e.color); gc.fillOval(e.x-e.size/2, e.y-e.size/2, e.size, e.size); }
        gc.setGlobalAlpha(1.0);

        // 浮动文字(灼烧伤害)
        for (FloatingText t : floatTexts) {
            gc.setGlobalAlpha(t.life / 45.0);
            gc.setFill(t.color);
            gc.setFont(Font.font("Impact", 16));
            gc.fillText(t.text, t.x - 8, t.y);
        }
        gc.setGlobalAlpha(1.0);

        drawHUD(gc);

        // 时间停止效果
        if (timeStopped) {
            double alpha = 0.15 + 0.1 * Math.sin(System.currentTimeMillis() * 0.008);
            gc.setFill(Color.rgb(100, 100, 120, alpha));
            gc.fillRect(0, 0, W, H);
            gc.setFill(Color.rgb(180, 180, 200, 0.4));
            gc.setFont(Font.font("Impact", 24));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("⏱ TIME STOP", W / 2, 80);
            gc.setTextAlign(TextAlignment.LEFT);
            // 倒计时
            gc.setFill(Color.rgb(200, 200, 220, 0.6));
            gc.setFont(Font.font("Arial", 14));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText((timeStopTimer / 60 + 1) + "s", W / 2, 105);
            gc.setTextAlign(TextAlignment.LEFT);
        }

        // 结算
        if (gameOver) {
            gc.setFill(Color.rgb(0,0,0,0.65)); gc.fillRect(0,0,W,H);
            gc.setFill(Color.GOLD); gc.setFont(Font.font("Impact",50));
            gc.setTextAlign(TextAlignment.CENTER); gc.fillText("GAME OVER", W/2, 220);
            gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial",26));
            gc.fillText(winner, W/2, 280);
            double blink = Math.sin(System.currentTimeMillis()*0.005) > 0 ? 1 : 0.3;
            gc.setGlobalAlpha(blink); gc.setFill(Color.LIGHTGRAY); gc.setFont(Font.font("Arial",14));
            gc.fillText("按 ENTER 重新开始 | ESC 返回菜单", W/2, 350);
            gc.setGlobalAlpha(1.0); gc.setTextAlign(TextAlignment.LEFT);
        }
    }

    private void drawDioKnives(GraphicsContext gc, Fighter fighter) {
        if (!fighter.isDio()) return;
        for (Projectile knife : fighter.getBullets()) {
            if (!knife.isActive()) continue;
            double x = knife.getBounds().getMinX();
            double y = knife.getBounds().getMinY() + 3;
            gc.save();
            if (knife.getVelX() < 0) {
                gc.translate(x + 72, 0);
                gc.scale(-1, 1);
                x = 0;
            }
            if (dioKnifeImage != null) gc.drawImage(dioKnifeImage, x, y, 72, 22);
            else {
                gc.setFill(Color.SILVER);
                gc.fillPolygon(new double[]{x, x + 48, x + 70, x + 48},
                        new double[]{y + 11, y + 2, y + 11, y + 20}, 4);
                gc.setFill(Color.rgb(45, 25, 24));
                gc.fillRoundRect(x + 47, y + 5, 25, 12, 3, 3);
            }
            gc.restore();
        }
    }

    private void drawBackground(GraphicsContext gc) {
        if (stageBackground != null) {
            double scale = Math.max(W / stageBackground.getWidth(), GY / stageBackground.getHeight());
            double sourceW = W / scale;
            double sourceH = GY / scale;
            double sourceX = Math.max(0, (stageBackground.getWidth() - sourceW) / 2);
            double sourceY = Math.max(0, stageBackground.getHeight() - sourceH);
            gc.drawImage(stageBackground, sourceX, sourceY, sourceW, sourceH, 0, 0, W, GY);
            gc.setFill(Color.rgb(4, 8, 20, 0.32));
            gc.fillRect(0, 0, W, GY);
            gc.setFill(palette[2]);
            gc.fillRect(0, GY, W, H - GY);
            gc.setStroke(palette[3]);
            gc.setLineWidth(2);
            gc.strokeLine(0, GY, W, GY);
            gc.setFill(Color.rgb(255,255,255,0.65));
            gc.setFont(Font.font("Arial",10));
            gc.fillText("▸ " + mapName, 10, H - 22);
            return;
        }
        Color sr = palette[0], sg = palette[1];
        for (int y = 0; y < GY; y++) {
            double p = (double)y / GY;
            double r = sr.getRed()*(1-p) + sg.getRed()*p;
            double g = sr.getGreen()*(1-p) + sg.getGreen()*p;
            double b = sr.getBlue()*(1-p) + sg.getBlue()*p;
            gc.setStroke(Color.color(r,g,b));
            gc.strokeLine(0,y,W,y);
        }
        gc.setFill(Color.rgb(20,25,50,0.6));
        double[] hx={0,80,180,280,380,480,580,680,780,880,960}, hy={GY,GY-40,GY-70,GY-30,GY-90,GY-50,GY-80,GY-20,GY-100,GY-60,GY};
        gc.fillPolygon(hx, hy, hx.length);
        gc.setFill(palette[2]); gc.fillRect(0,GY,W,H-GY);
        gc.setStroke(palette[3]); gc.setLineWidth(2); gc.strokeLine(0,GY,W,GY);
        // 地图名
        gc.setFill(Color.rgb(255,255,255,0.3)); gc.setFont(Font.font("Arial",10));
        gc.fillText("▸ "+mapName, 10, H-22);
    }

    private void drawPlatforms(GraphicsContext gc) {
        for (double[] p : platforms) {
            gc.setFill(palette[4]); gc.fillRoundRect(p[1],p[0],p[2]-p[1],12,3,3);
            gc.setStroke(palette[5]); gc.setLineWidth(1); gc.strokeRoundRect(p[1],p[0],p[2]-p[1],12,3,3);
        }
    }

    /** HUD */
    private void drawHUD(GraphicsContext gc) {
        gc.setFill(p1Char.body); gc.setFont(Font.font("Arial",13));
        gc.fillText("P1 "+fighterName(p1Mugen, p1Char), 15, 22);
        drawLives(gc, 80, 10, p1.getLives(), true);
        drawHpBar(gc, 15, 35, 200, p1.getHp(), p1.getMaxHp(), p1Char.body);
        drawChargeBar(gc, 15, 53, 180, p1.getCharge(), p1.getMaxCharge(), p1Char.body);
        if (p1.getSkillCooldown() > 0) { gc.setFill(Color.ORANGE); gc.setFont(Font.font("Arial",9)); gc.fillText("技能冷却:"+(p1.getSkillCooldown()/60+1)+"s",15,70); }
        gc.setFill(p2Char.body); gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText((singlePlayer ? "AI " : "P2 ")+fighterName(p2Mugen, p2Char), 945, 22);
        drawLives(gc, 880, 10, p2.getLives(), false);
        drawHpBar(gc, 745, 35, 200, p2.getHp(), p2.getMaxHp(), p2Char.body);
        drawChargeBar(gc, 765, 53, 180, p2.getCharge(), p2.getMaxCharge(), p2Char.body);
        if (p2.getSkillCooldown() > 0) { gc.setFill(Color.ORANGE); gc.setFont(Font.font("Arial",9)); gc.fillText("技能冷却:"+(p2.getSkillCooldown()/60+1)+"s",945,70); }
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.rgb(180,180,190)); gc.setFont(Font.font("Arial",10));
        gc.fillText("普通角色近战 / DIO飞刀",420,25);
        gc.setFill(Color.rgb(120,120,120)); gc.setFont(Font.font("Arial",9));
        gc.fillText("W跳 A左 D右 J攻击 K集气(按住) L技能",15,H-8);
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("↑跳 ←左 →右 1攻击 2集气(按住) 3技能",945,H-8);
        gc.setTextAlign(TextAlignment.LEFT);
    }

    private static String fighterName(MugenCharacterInfo info, Fighter.CharType fallback) {
        return info == null || info.displayName().isBlank() ? fallback.label : info.displayName();
    }

    private void drawHpBar(GraphicsContext gc, double x, double y, double w, int hp, int max, Color c) {
        double r = (double) hp / max;
        gc.setFill(Color.rgb(40,40,50)); gc.fillRoundRect(x, y, w, 12, 3, 3);
        gc.setFill(r>0.5?c:(r>0.25?Color.ORANGE:Color.RED));
        gc.fillRoundRect(x+1, y+1, (w-2)*r, 10, 2, 2);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial",9)); gc.fillText(hp+"/"+max, x+w/2-12, y+10);
    }

    private void drawLives(GraphicsContext gc, double x, double y, int lives, boolean ltr) {
        for (int i = 0; i < lives; i++) {
            double lx = ltr ? x+i*20 : x-i*20;
            gc.setFill(Color.rgb(0,200,100)); gc.fillRoundRect(lx,y,15,15,3,3);
            gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial",9)); gc.fillText("♥",lx+3,y+12);
        }
    }

    private void drawChargeBar(GraphicsContext gc, double x, double y, double w, int charge, int max, Color c) {
        double r = (double) charge / max;
        gc.setFill(Color.rgb(30,30,40)); gc.fillRoundRect(x, y, w, 7, 3, 3);
        gc.setFill(c); gc.fillRoundRect(x+1, y+1, (w-2)*r, 5, 2, 2);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Arial",8)); gc.fillText("气 "+charge+"/"+max, x+w+4, y+6);
    }

    // ====================================================================
    // 技能抛射物
    // ====================================================================

    /**
     * 技能抛射物 —— 从释放者位置飞出, 落地/出屏后爆炸
     * 爆炸时在落地位置生成 SkillEffect 对目标生效
     */
    private static class SkillThrowable {
        double x, y, vx, vy;
        final Fighter.SkillEffect.Type type;
        final boolean fromP1;
        boolean exploded = false, effectApplied = false;
        boolean frozen = false; // 时间停止冻结
        int life = 0;

        SkillThrowable(double x, double y, double vx, double vy, Fighter.SkillEffect.Type type, boolean fromP1) {
            this.x = x; this.y = y; this.vx = vx; this.vy = vy;
            this.type = type; this.fromP1 = fromP1;
        }

        void update() {
            if (exploded || frozen) return;
            life++;
            vy += 0.45; // 重力
            x += vx; y += vy;
            // 落地或飞出屏幕时爆炸
            if (y >= 500 || x < -50 || x > 1010 || life > 50) exploded = true;
        }

        void draw(GraphicsContext gc) {
            if (exploded) return;
            Color c = switch (type) {
                case NITROGEN -> Color.rgb(150, 220, 255);
                case FIRE -> Color.rgb(255, 150, 50);
                case EXPLOSION -> Color.rgb(255, 220, 80);
                default -> Color.WHITE;
            };
            // 飞行弹体(发光球体)
            gc.setFill(c); gc.fillOval(x-8, y-8, 16, 16);
            gc.setFill(Color.rgb(255,255,255,0.4)); gc.fillOval(x-4, y-4, 8, 8);
            // 拖尾特效
            gc.setGlobalAlpha(0.3);
            gc.setFill(Color.rgb(255,255,200,0.2)); gc.fillOval(x-12-vx, y-12-vy, 24, 24);
            gc.setGlobalAlpha(1.0);
        }
    }

    // ====================================================================
    // 地面区域效果(技能落点)
    // ====================================================================

    private static class GroundEffect {
        double x, y; int radius, life, totalLife; Color color;
        GroundEffect(double x, double y, int r, int life, Color c) {
            this.x=x; this.y=y; this.radius=r; this.life=life; this.totalLife=life; this.color=c;
        }
    }

    // ====================================================================
    // 烟雾云(白色角色技能)
    // ====================================================================

    private static class SmokeCloud {
        final double x, y;
        final boolean fromP1; // 释放者, 不给其加noCharge
        int life, maxLife;
        boolean active = true;
        SmokeCloud(double x, double y, int duration, boolean fromP1) {
            this.x = x; this.y = y;
            this.life = duration; this.maxLife = duration;
            this.fromP1 = fromP1;
        }
    }

    // ====================================================================
    // 浮动伤害文字
    // ====================================================================

    private static class FloatingText {
        double x, y; String text; int life; Color color;
        FloatingText(double x, double y, String t, Color c) {
            this.x = x; this.y = y; this.text = t; this.color = c; this.life = 45;
        }
    }

    // ====================================================================
    // 从天而降的空投
    // ====================================================================

    private static class FallingAirdrop {
        double x, y;                // 当前位置
        final double targetY;       // 目标Y(地面)
        final Weapon.Type weaponType;
        boolean active = true;
        boolean landed = false;
        int landTimer = 0;          // 落地后存活帧数
        boolean pickedUp = false;   // 已被捡起(消失动画)
        int fallFrame = 0;

        FallingAirdrop(double x, double y, double targetY, Weapon.Type t) {
            this.x = x; this.y = y; this.targetY = targetY - 40; this.weaponType = t;
        }

        void update() {
            fallFrame++;
            if (!landed && !pickedUp) {
                // 降落伞: 缓慢下落
                if (y < targetY) y += 2.0;
                else { y = targetY; landed = true; landTimer = 600; } // 10秒后消失
            } else if (landed && !pickedUp) {
                landTimer--;
                if (landTimer <= 0) active = false;
            } else {
                // 被捡起: 快速消失
                active = false;
            }
        }

        boolean contains(double fx, double fy, double fw, double fh) {
            if (!landed || pickedUp) return false;
            return x+30 > fx && x < fx+fw && y+30 > fy && y < fy+fh;
        }

        void pickup() { pickedUp = true; }

        void draw(GraphicsContext gc) {
            if (!active) return;
            if (!landed) {
                // 降落伞
                double swing = Math.sin(fallFrame * 0.1) * 5;
                gc.setFill(Color.rgb(200,100,50));
                gc.fillRoundRect(x-4+swing, y-8, 8, 12, 2, 2);
                // 伞面
                gc.setFill(Color.rgb(255,200,150));
                gc.fillArc(x-18+swing, y-28, 36, 20, 180, 180, javafx.scene.shape.ArcType.ROUND);
                gc.setStroke(Color.rgb(180,80,30)); gc.setLineWidth(1);
                gc.strokeLine(x+swing, y-28, x-8+swing, y+4);
                gc.strokeLine(x+swing, y-28, x+8+swing, y+4);
                // 箱子
                drawBox(gc);
            } else {
                drawBox(gc);
                // 闪烁提示
                if (!pickedUp && landTimer < 180 && landTimer % 15 < 8) gc.setGlobalAlpha(0.5);
                drawBox(gc);
                gc.setGlobalAlpha(1.0);
                // 武器标签
                gc.setFill(Color.YELLOW); gc.setFont(Font.font("Arial",10));
                gc.fillText(weaponType.label, x-8, y-5);
            }
        }

        private void drawBox(GraphicsContext gc) {
            gc.setFill(Color.rgb(180,140,80)); gc.fillRoundRect(x, y, 30, 26, 4, 4);
            gc.setStroke(Color.rgb(120,80,40)); gc.setLineWidth(2); gc.strokeRoundRect(x, y, 30, 26, 4, 4);
            gc.setStroke(Color.rgb(160,120,60)); gc.setLineWidth(1);
            gc.strokeLine(x, y+13, x+30, y+13); gc.strokeLine(x+15, y, x+15, y+26);
            gc.setFill(Color.rgb(255,200,100)); gc.fillOval(x+10, y+8, 10, 10);
        }
    }

    // ====================================================================
    // 特效
    // ====================================================================

    private static class HitEffect {
        double x,y,vx,vy,size; int life; Color color;
        HitEffect(double x,double y,double vx,double vy,double size,Color c) {
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.size=size;life=12;color=c;
        }
    }
}
