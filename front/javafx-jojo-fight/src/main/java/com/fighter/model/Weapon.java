package com.fighter.model;

/**
 * 武器系统 —— 所有武器类型和属性定义
 * 
 * 【武器属性】(伤害, 冷却帧, 弹速, 散射, 弹丸数, 射程)
 * 手枪   (5,  15, 8,  0,  1, 300)  近距慢速
 * 冲锋枪 (5,  4,  9,  3,  1, 300)  近距快速
 * 步枪   (10, 10, 12, 0,  1, 500)  中距中速
 * 狙击枪 (30, 35, 16, 0,  1, 800)  远距极慢
 * 霰弹枪 (10, 22, 6,  12, 3, 200)  极近慢速三发
 * 
 * 空投武器持续7秒后自动切回手枪
 * 
 * @author 课程设计
 * @version 6.1
 */
public class Weapon {

    public enum Type {
        PISTOL("手枪", 0),
        SHOTGUN("霰弹枪", 7000),
        SMG("冲锋枪", 7000),
        RIFLE("步枪", 7000),
        SNIPER("狙击枪", 7000);

        public final String label;
        public final long durationMs;

        Type(String label, long durationMs) {
            this.label = label;
            this.durationMs = durationMs;
        }
    }

    public final Type type;
    public final int damage;          // 单发伤害
    public final int fireCooldown;    // 射击间隔(帧)
    public final double bulletSpeed;  // 子弹速度
    public final double spread;       // 散射角度
    public final int pelletCount;     // 弹丸数
    public final int range;           // 射程(像素)

    private static final java.util.Map<Type, int[]> STATS = new java.util.HashMap<>();
    static {
        //               damage, 冷却, 弹速, 散射, 弹丸, 射程
        STATS.put(Type.PISTOL,   new int[]{5,  15, 8,  0,  1, 300});
        STATS.put(Type.SMG,      new int[]{5,  4,  9,  3,  1, 300});
        STATS.put(Type.RIFLE,    new int[]{10, 10, 12, 0,  1, 500});
        STATS.put(Type.SNIPER,   new int[]{30, 35, 16, 0,  1, 800});
        STATS.put(Type.SHOTGUN,  new int[]{10, 22, 6,  12, 3, 200});
    }

    public Weapon(Type type) {
        this.type = type;
        int[] s = STATS.get(type);
        this.damage = s[0];
        this.fireCooldown = s[1];
        this.bulletSpeed = s[2];
        this.spread = s[3];
        this.pelletCount = s[4];
        this.range = s[5];
    }

    public boolean isPistol() { return type == Type.PISTOL; }
    public String getLabel() { return type.label; }
}
