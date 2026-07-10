package com.fighter.model;

import com.fighter.character.CharacterProfile;
import com.fighter.character.CharacterProfileLoader;
import com.fighter.util.SpriteLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import java.util.*;

/**
 * 对战角色 —— 每条命100HP + 集气技能
 * 
 * 【角色】
 * 蓝方(液氮:无伤+高减速+不能跳) | 红方(燃烧弹:2HP/s DOT)
 * 黄方(手榴弹:30HP瞬伤) | 白方(烟雾弹:减伤+禁气)
 * 绿方(回血:20HP/s×3s,受伤打断) | 粉方(无敌3s,不能攻击,禁气)
 * 黑方(时间停止1.5s,万物冻结)
 * 
 * @author 课程设计
 * @version 7.0
 */
public class Fighter {

    public enum CharType {
        BLUE("jotaro", "Jotaro", "白金之星·欧拉连打", Color.rgb(14,165,233), Color.rgb(125,211,252)),
        RED("dio", "Dio", "世界·时间停止", Color.rgb(234,179,8), Color.rgb(253,224,71)),
        YELLOW("giorno", "Giorno", "黄金体验·生命治愈", Color.rgb(217,70,239), Color.rgb(240,171,252)),
        WHITE("pucci", "Pucci", "白蛇迷雾", Color.rgb(226,232,240), Color.rgb(255,255,255)),
        GREEN("polnareff", "Polnareff", "银色战车·烈焰剑舞", Color.rgb(34,197,94), Color.rgb(134,239,172)),
        PINK("dojo-jim", "Dojo Jim", "寒冰封锁", Color.rgb(244,114,182), Color.rgb(251,207,232)),
        BLACK("diavolo", "Diavolo", "绯红之王·时间删除", Color.rgb(225,29,72), Color.rgb(192,132,252)),
        EXAID("exaid", "Kamen Rider Ex-Aid", "Hyper Muteki", Color.rgb(255,44,156), Color.rgb(184,255,44));
        public final String profileId;
        public final String label;
        public final String skillName;
        public final Color body;
        public final Color bullet;
        public final CharacterProfile profile;

        CharType(String id, String label, String skillName, Color body, Color bullet) {
            this.profileId = id;
            this.label = label;
            this.skillName = skillName;
            this.body = body;
            this.bullet = bullet;
            this.profile = CharacterProfileLoader.load(id);
        }
    }

    public final CharType charType;
    private final CharacterProfile profile;

    // 物理
    private double x, y, velX, velY;
    private boolean facingRight = true, isOnGround = false;
    public double width = 44, height = 80;

    // 生命: 回到 fight 旧版规则，每人3条命，每条命100HP。
    private static final int HP_PER_LIFE = 100;
    private int hp = HP_PER_LIFE;
    private int lives = 3;
    private boolean dead = false;
    private boolean invincible = false;
    private int invincibleTimer = 0;

    // 武器
    private Weapon currentWeapon = new Weapon(Weapon.Type.PISTOL);
    private long weaponPickupTime = 0;
    private long lastShotNanoTime = 0; // System.nanoTime() 强制冷却(毫秒级)
    private boolean meleeHitConsumed = false;
    private final List<Projectile> bullets = new ArrayList<>();
    
    // 下平台
    boolean dropRequested = false;

    // 集气 & 技能
    private int charge = 0;
    private static final int MAX_CHARGE = 100;
    private boolean isCharging = false;
    private int skillCooldown = 0;
    private SkillEffect activeSkill = null;

    // 状态效果
    private boolean slowed = false;
    private int slowTimer = 0;
    private boolean noJump = false;   // 液氮: 不能跳跃
    private int noJumpTimer = 0;
    private boolean burning = false;
    private int burnTimer = 0;
    private int burnTick = 0;
    public boolean burnDamageDealt = false; // 本帧是否触发灼烧伤害(FightScene读取)
    // 烟雾弹
    private boolean smokeBuff = false;      // 烟雾减伤
    private int smokeBuffTimer = 0;
    public boolean noCharge = false;        // 烟中无法集气
    private int noChargeTimer = 0;

    // 绿方: 回血
    private boolean healing = false;
    private int healingTimer = 0;
    private int healingTick = 0;

    // 粉方: 无敌(技能)
    private boolean skillInvincible = false;
    private int skillInvincibleTimer = 0;
    private boolean cannotAttack = false;   // 无敌期间不能攻击

    // 状态机
    public enum State { IDLE, WALKING, JUMPING, SHOOTING, HIT, DEAD, WIN, LOSE }
    private State state = State.IDLE;
    private int stateTimer = 0, animFrame = 0;
    private final Map<String, Image> spriteCache = new HashMap<>();

    public Fighter(CharType type, boolean facingRight) {
        this(type, facingRight, type.profile);
    }

    public Fighter(CharType type, boolean facingRight, CharacterProfile profile) {
        this.charType = type; this.facingRight = facingRight;
        this.profile = profile == null ? type.profile : profile;
    }

    // ====================================================================
    // 更新
    // ====================================================================

    public void update(double groundY, double leftBound, double rightBound, double[][] platforms) {
        burnDamageDealt = false; // 每帧重置灼烧标记
        stateTimer++; animFrame++;
        if (skillCooldown > 0) skillCooldown--;

        if (invincible) { invincibleTimer--; if (invincibleTimer <= 0) invincible = false; }
        if (!currentWeapon.isPistol() && System.currentTimeMillis()-weaponPickupTime > 7000)
            setWeapon(new Weapon(Weapon.Type.PISTOL));

        // 状态效果
        if (slowed) { slowTimer--; if (slowTimer <= 0) slowed = false; }
        if (noJump) { noJumpTimer--; if (noJumpTimer <= 0) noJump = false; }
        if (burning) {
            burnTick++;
            if (burnTick >= 30) { burnTick = 0; hp = Math.max(0, hp - 2); burnDamageDealt = true; }
            burnTimer--;
            if (burnTimer <= 0) { burning = false; }
        }
        if (smokeBuff) { smokeBuffTimer--; if (smokeBuffTimer <= 0) smokeBuff = false; }
        if (noCharge) { noChargeTimer--; if (noChargeTimer <= 0) noCharge = false; }

        // 绿方: 回血(每60帧恢复20HP)
        if (healing) {
            healingTick++;
            if (healingTick >= 60) {
                healingTick = 0;
                hp = Math.min(HP_PER_LIFE, hp + 20);
            }
            healingTimer--;
            if (healingTimer <= 0) { healing = false; }
        }

        // 粉方: 无敌状态计时
        if (skillInvincible) {
            skillInvincibleTimer--;
            if (skillInvincibleTimer <= 0) {
                skillInvincible = false;
                cannotAttack = false;
            }
        }

        if (activeSkill != null && !activeSkill.active) activeSkill = null;

        // 物理
        double spd = slowed ? 1.5 : 4.0;
        velY += 0.55; // 重力恒定作用(修复: 之前isOnGround时跳过重力导致踏空飞行)
        x += velX; y += velY;
        isOnGround = false; // 每帧重置, 由下方碰撞检测重新设置
        // 下平台: 按S/下方向时临时忽略平台
        boolean dropDown = dropRequested;
        dropRequested = false;
        if (y >= groundY - height) { y = groundY - height; velY = 0; isOnGround = true; }
        if (platforms != null && velY >= 0) {
            for (double[] p : platforms) {
                if (dropDown && y+height >= p[0]-2 && y+height <= p[0]+4) continue; // 穿过平台
                if (y+height >= p[0] && y+height <= p[0]+14 && x+width > p[1] && x < p[2]) {
                    y = p[0]-height; velY = 0; isOnGround = true; break;
                }
            }
        }
        x = Math.max(leftBound, Math.min(rightBound - width, x));
        if (isOnGround) velX *= 0.85;

        // 状态
        switch (state) {
            case SHOOTING -> { if (stateTimer > 18) setIdle(); }
            case HIT -> { if (stateTimer > 10) { if (dead) setState(State.DEAD); else setIdle(); } }
            case DEAD -> {
                if (stateTimer > 40) {
                    x = (charType == CharType.BLUE || charType == CharType.YELLOW) ? 180 : 720;
                    y = groundY - height; velX = velY = 0;
                    lives--; dead = false; invincible = true; invincibleTimer = 60;
                    hp = HP_PER_LIFE;
                    setWeapon(new Weapon(Weapon.Type.PISTOL));
                    charge = 0; activeSkill = null;
                    slowed = false; noJump = false; burning = false; smokeBuff = false; noCharge = false;
                    healing = false; healingTimer = 0; healingTick = 0;
                    skillInvincible = false; skillInvincibleTimer = 0; cannotAttack = false;
                    setIdle();
                }
            }
            default -> {}
        }
        if (isCharging && charge < MAX_CHARGE && animFrame % 3 == 0 && !healing && !skillInvincible) charge += 1;
        // 粉方无敌期间+时间停止期间不能集气, 在FightScene控制noCharge
        bullets.removeIf(b -> { b.update(); return !b.isActive(); });
    }

    // ====================================================================
    // 移动
    // ====================================================================

    public void walk(double dir) {
        if (dead) return;
        setState(State.WALKING);
        double speed = slowed ? 1.5 : (isOnGround ? 4.0 : 2.5); // 空中速度减半
        velX = dir * speed;
        if (dir > 0) facingRight = true; else if (dir < 0) facingRight = false;
    }
    public void jump() {
        if (noJump) return;
        if (!isOnGround || dead) return;
        velY = -11.0; isOnGround = false;
    }
    /** 请求下平台(按S/下方向时触发, 下一帧生效) */
    public void requestDrop() { dropRequested = true; }

    // ====================================================================
    // 射击
    // ====================================================================

    /** 检查是否冷却完毕(基于纳秒时间，不受帧率影响) */
    public boolean canShoot() {
        if (dead || cannotAttack) return false;
        long elapsed = System.nanoTime() - lastShotNanoTime;
        long cooldownNs = currentWeapon.fireCooldown * 16_666_667L; // 1帧≈16.67ms
        return elapsed >= cooldownNs;
    }

    public boolean isDio() { return charType == CharType.RED; }

    public boolean attack() {
        if (dead || cannotAttack || state == State.SHOOTING) return false;
        setState(State.SHOOTING);
        meleeHitConsumed = false;
        return true;
    }

    public boolean checkMeleeHit(Fighter opponent) {
        if (isDio() || state != State.SHOOTING || meleeHitConsumed || stateTimer < 5 || stateTimer > 12) return false;
        double myCenter = x + width / 2;
        double targetCenter = opponent.x + opponent.width / 2;
        boolean correctSide = facingRight ? targetCenter >= myCenter : targetCenter <= myCenter;
        boolean inRange = Math.abs(targetCenter - myCenter) <= 92 && Math.abs(opponent.y - y) <= 70;
        if (!correctSide || !inRange) return false;
        meleeHitConsumed = true;
        opponent.takeMeleeDamage(16, facingRight ? 5 : -5);
        return true;
    }

    /** 获取剩余冷却帧数(用于UI显示) */
    public int getShootCooldownRemaining() {
        long elapsed = System.nanoTime() - lastShotNanoTime;
        long cooldownNs = currentWeapon.fireCooldown * 16_666_667L;
        long remaining = cooldownNs - elapsed;
        if (remaining <= 0) return 0;
        return (int)(remaining / 16_666_667L) + 1;
    }

    public boolean shoot() {
        if (dead || !isDio()) return false;
        setState(State.SHOOTING);
        lastShotNanoTime = System.nanoTime();
        double bx = facingRight ? x + width : x - 36;
        Projectile knife = new Projectile(bx, y + 24, facingRight ? 9 : -9, 0, 14, 680);
        knife.setSize(28);
        bullets.add(knife);
        return true;
    }

    public void setWeapon(Weapon w) { currentWeapon = w; weaponPickupTime = System.currentTimeMillis(); }

    // ====================================================================
    // 集气 & 技能
    // ====================================================================

    public void startCharge() { if (!dead && !noCharge && charge < MAX_CHARGE) isCharging = true; }
    public void stopCharge() { isCharging = false; }

    /** 消耗集气使用技能 —— 仅扣除消耗, 不创建效果(由FightScene抛射物负责) */
    public SkillEffect.Type useSkill() {
        if (dead || charge < MAX_CHARGE || skillCooldown > 0) return null;
        charge = 0;
        skillCooldown = 120;
        return switch (charType) {
            case BLUE -> SkillEffect.Type.NITROGEN;
            case RED -> SkillEffect.Type.FIRE;
            case YELLOW -> SkillEffect.Type.EXPLOSION;
            case WHITE -> SkillEffect.Type.SMOKE;
            case GREEN -> SkillEffect.Type.HEAL;
            case PINK -> SkillEffect.Type.INVINCIBLE;
            case BLACK -> SkillEffect.Type.TIMESTOP;
            case EXAID -> SkillEffect.Type.INVINCIBLE;
        };
    }

    /** 启动绿方回血 */
    public void startHealing() {
        healing = true;
        healingTimer = 180; // 3秒
        healingTick = 0;
    }

    /** 中断绿方回血 */
    public void interruptHealing() {
        if (healing) {
            healing = false;
            healingTimer = 0;
            healingTick = 0;
        }
    }

    /** 启动粉方无敌 */
    public void startInvincible() {
        skillInvincible = true;
        skillInvincibleTimer = 240; // 4秒
        cannotAttack = true;
    }

    /** 是否无敌(技能无敌, 非重生无敌) */
    public boolean isSkillInvincible() { return skillInvincible; }

    /** 技能命中检测 */
    public boolean checkSkillHit(SkillEffect se) {
        if (dead || invincible || skillInvincible) return false;
        boolean inRange = Math.abs(x+width/2 - se.x) < se.radius && Math.abs(y+height/2 - se.y) < se.radius;
        if (!inRange) return false;
        // 绿方: 吃到技能效果中断回血
        interruptHealing();
        switch (se.type) {
            case NITROGEN -> { slowed = true; slowTimer = 240; noJump = true; noJumpTimer = 240; } // 4秒减速+不能跳
            case FIRE -> { burning = true; burnTimer = 600; burnTick = 0; } // 10秒灼烧
            case EXPLOSION -> { hp = Math.max(0, hp - 30); checkLifeLost(); } // 一次性30伤害
            case SMOKE -> { noCharge = true; noChargeTimer = 300; } // 5秒无法集气
            default -> {}
        }
        return true;
    }

    // ====================================================================
    // 受击(子弹伤害)
    // ====================================================================

    /** 子弹命中: 扣HP(烟雾减伤50%), 按射击方向击退 */
    public void takeBulletDamage(int damage, double knockbackDir) {
        if (invincible || dead || skillInvincible) return;
        if (smokeBuff) damage = Math.max(1, damage / 2); // 烟雾减伤50%
        // 绿方: 受到伤害中断回血
        interruptHealing();
        hp = Math.max(0, hp - damage);
        setState(State.HIT);
        velX = knockbackDir;
        checkLifeLost();
    }

    public void takeMeleeDamage(int damage, double knockbackDir) {
        takeBulletDamage(damage, knockbackDir);
    }

    /** HP归零时丢一条命 */
    private void checkLifeLost() {
        if (hp <= 0 && !dead) {
            dead = true;
            setState(State.DEAD);
            velX = facingRight ? -5 : 5;
            velY = -4;
            isOnGround = false;
            slowed = false; noJump = false; burning = false;
        }
    }

    public boolean hasLivesLeft() { return lives > 0; }

    // ====================================================================
    // 子弹碰撞
    // ====================================================================

    public int checkBulletHit(Fighter opponent) {
        int hits = 0;
        for (Projectile b : bullets) {
            if (!b.isActive()) continue;
            double bx = b.getBounds().getMinX(), by = b.getBounds().getMinY(), bw = b.getBounds().getWidth();
            if (bx+bw > opponent.x && bx < opponent.x+opponent.width
                    && by+bw > opponent.y && by < opponent.y+opponent.height) {
                b.deactivate();
                // 击退方向: 从射击者指向被击者
                double backDir = (x < opponent.x) ? 4 : -4;
                opponent.takeBulletDamage(b.getDamage(), backDir);
                hits++;
            }
        }
        return hits;
    }

    // ====================================================================
    // 绘制
    // ====================================================================

    public void draw(GraphicsContext gc, double offsetX, double groundY) {
        double dx = x+offsetX, dy = y;
        if (dead && stateTimer > 10) return;
        gc.setFill(Color.rgb(0,0,0,0.3)); gc.fillOval(dx+2, groundY-4, 40, 8);
        if (invincible && animFrame%6<3) gc.setGlobalAlpha(0.4);
        gc.save();
        if (!facingRight) { gc.translate(dx+width/2,0); gc.scale(-1,1); gc.translate(-(dx+width/2),0); }
        Image img = currentSprite();
        if (img != null) {
            double targetH = Math.min(profile.imageHeight(), height * 2.15);
            double targetW = targetH * img.getWidth() / Math.max(1, img.getHeight());
            gc.drawImage(img, dx + width / 2 - targetW / 2, dy + height - targetH, targetW, targetH);
        } else {
            drawPixel(gc, dx, dy);
        }
        gc.restore();
        gc.setGlobalAlpha(1.0);
        if (!currentWeapon.isPistol()) {
            gc.setFill(Color.YELLOW); gc.setFont(javafx.scene.text.Font.font("Arial",9));
            long left = 7-(System.currentTimeMillis()-weaponPickupTime)/1000;
            gc.fillText(currentWeapon.getLabel()+" "+left+"s", dx, dy-14);
        }
        // 换弹夹提示(基于纳秒冷却, 不受帧率影响)
        int cdRemain = getShootCooldownRemaining();
        if (cdRemain > 0) {
            double pct = 1.0 - (double)cdRemain / currentWeapon.fireCooldown;
            gc.setFill(Color.rgb(255,200,100)); gc.setFont(javafx.scene.text.Font.font("Arial",8));
            gc.fillText("换弹", dx+width/2-8, dy-3);
            gc.setFill(Color.rgb(40,40,50)); gc.fillRect(dx, dy-8, width, 4);
            gc.setFill(Color.rgb(255,200,100)); gc.fillRect(dx+1, dy-7, (width-2)*pct, 2);
        }
        if (noJump || slowed) {
            gc.setFill(Color.rgb(100,200,255,0.4)); gc.fillRoundRect(dx-2,dy-2,width+4,height+4,4,4);
        }
        if (burning) {
            gc.setFill(Color.rgb(255,100,0,0.4)); gc.fillRoundRect(dx-2,dy-2,width+4,height+4,4,4);
        }
        if (smokeBuff) {
            gc.setFill(Color.rgb(200,200,220,0.25)); gc.fillRoundRect(dx-4,dy-4,width+8,height+8,4,4);
            gc.setFill(Color.rgb(255,255,255,0.15)); gc.fillText("减伤", dx+width/2-8, dy-20);
        }
    }

    private Image currentSprite() {
        if (profile == null) return null;
        String action = switch (state) {
            case SHOOTING -> animFrame % 18 < 10 ? "punch" : "kick";
            case HIT, DEAD, LOSE -> "hit";
            case JUMPING -> "kick";
            default -> "idle";
        };
        String[] frames = profile.framesFor(action);
        if (frames.length == 0) return null;
        int frameIndex = Math.floorMod(animFrame / 6, frames.length);
        String path = frames[frameIndex];
        return spriteCache.computeIfAbsent(path, SpriteLoader::loadImage);
    }

    private void drawPixel(GraphicsContext gc, double x, double y) {
        Color body = charType.body;
        Color accent = switch(charType) {
            case BLUE,RED -> Color.rgb(255,200,60);
            case YELLOW -> Color.rgb(255,255,200);
            case WHITE -> Color.rgb(160,160,180);
            case GREEN -> Color.rgb(100,255,150);
            case PINK, EXAID -> Color.rgb(255,200,220);
            case BLACK -> Color.rgb(160,160,170);
        };
        // 黑方: 灰色边框(绘制稍大的半透明底框)
        if (charType == Fighter.CharType.BLACK) {
            gc.setStroke(Color.rgb(160,160,170,0.6)); gc.setLineWidth(2);
            gc.strokeRoundRect(x+1, y+1, width-2, height-2, 6, 6);
        }
        gc.setFill(accent); gc.fillArc(x+2,y,40,32,180,180,javafx.scene.shape.ArcType.ROUND);
        gc.setFill(Color.WHITE); gc.fillRoundRect(x+7,y+12,30,10,3,3);
        gc.setFill(Color.BLACK); gc.fillOval(x+12,y+14,5,6); gc.fillOval(x+27,y+14,5,6);
        gc.setFill(body); gc.fillRoundRect(x+5,y+30,34,32,4,4);
        gc.setFill(accent); gc.fillRect(x+5,y+52,34,4);
        gc.setFill(body); gc.fillRect(x+9,y+62,10,16); gc.fillRect(x+25,y+62,10,16);
        gc.setFill(accent); gc.fillRoundRect(x+7,y+74,13,6,2,2); gc.fillRoundRect(x+24,y+74,13,6,2,2);
        if (state==State.SHOOTING) {
            gc.setFill(body); gc.fillRect(x+38,y+34,16,7);
            gc.setFill(accent); gc.fillRoundRect(x+52,y+32,8,11,3,3);
            if (stateTimer<4) { gc.setFill(Color.rgb(255,255,0,0.8)); gc.fillOval(x+58,y+33,8,8); }
        } else {
            gc.setFill(body); gc.fillRect(x-2,y+34,8,7); gc.fillRect(x+38,y+34,8,7);
            gc.setFill(accent); gc.fillRoundRect(x-3,y+40,7,7,2,2); gc.fillRoundRect(x+40,y+40,7,7,2,2);
        }
        // 绿方: 回血特效(绿光闪烁)
        if (healing) {
            gc.setFill(Color.rgb(0,255,100,0.2 + 0.15*Math.sin(System.currentTimeMillis()*0.01)));
            gc.fillRoundRect(x-3, y-3, width+6, height+6, 8, 8);
        }
        // 粉方: 无敌特效(粉色光环)
        if (skillInvincible) {
            gc.setStroke(Color.rgb(255,100,200,0.5)); gc.setLineWidth(2);
            gc.strokeRoundRect(x-4, y-4, width+8, height+8, 10, 10);
            gc.setLineWidth(1);
        }
    }

    public void setIdle() { setState(State.IDLE); }
    public void setState(State s) { state=s; stateTimer=0; if (s != State.SHOOTING) meleeHitConsumed = false; }

    // Getter
    public String getName() { return charType.label; }
    public double getX() { return x; } public double getY() { return y; }
    public void setX(double x) { this.x=x; } public void setY(double y) { this.y=y; }
    public int getHp() { return hp; } public int getMaxHp() { return HP_PER_LIFE; }
    public int getLives() { return lives; } public boolean isDead() { return dead; }
    public boolean isFacingRight() { return facingRight; } public boolean isOnGround() { return isOnGround; }
    public State getState() { return state; }
    public int getCharge() { return charge; } public int getMaxCharge() { return MAX_CHARGE; }
    public int getSkillCooldown() { return skillCooldown; }
    public Weapon getCurrentWeapon() { return currentWeapon; }
    public Color getBulletColor() { return charType.bullet; }
    public List<Projectile> getBullets() { return bullets; }
    public SkillEffect getActiveSkill() { return activeSkill; }
    public void clearSkill() { activeSkill = null; }
    public boolean isBurning() { return burning; }
    public boolean hasSmokeBuff() { return smokeBuff; }
    public boolean isNoCharge() { return noCharge; }
    public boolean isHealing() { return healing; }
    /** 烟雾弹: 给释放者加减伤Buff */
    public void applySmokeBuff() { smokeBuff = true; smokeBuffTimer = 300; }

    // 技能效果类
    public static class SkillEffect {
        public enum Type { NITROGEN, FIRE, EXPLOSION, SMOKE, HEAL, INVINCIBLE, TIMESTOP }
        public final Type type; public final double x, y;
        public int duration; public final int radius;
        public boolean active = true;
        public SkillEffect(Type t, double x, double y) {
            this.type=t; this.x=x; this.y=y;
            this.radius = switch(t) {
                case NITROGEN -> 170;  // 液氮(燃烧弹两倍大)
                case FIRE -> 85;       // 燃烧弹
                case EXPLOSION -> 60;  // 手榴弹
                case SMOKE -> 140;     // 比液氮(170)小一点
                default -> 50;
            };
            this.duration = switch(t) {
                case NITROGEN -> 240;  // 4秒
                case FIRE -> 600;      // 10秒
                case EXPLOSION -> 20;  // 一次性瞬发
                case SMOKE -> 300;     // 5秒
                default -> 60;
            };
        }
        public void update() { duration--; if (duration<=0) active=false; }
    }
}
