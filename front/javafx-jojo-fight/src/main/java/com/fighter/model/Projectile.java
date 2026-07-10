package com.fighter.model;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

/**
 * 子弹/飞行道具 —— 带射程限制
 * 
 * @author 课程设计
 * @version 2.0
 */
public class Projectile {

    private double x, y;
    private final double velX, velY;
    private double distanceTraveled = 0;
    private final double maxDistance;
    private final int damage;
    private boolean active = true;
    private double size = 6;
    private boolean isAOE = false;
    private boolean frozen = false; // 时间停止冻结

    private static final int MAX_LIFETIME = 300; // 最长存活帧数
    private int lifetime = 0;

    /**
     * @param x 起始X
     * @param y 起始Y
     * @param vx X速度
     * @param vy Y速度
     * @param damage 伤害
     * @param maxDistance 最大射程(像素)
     */
    public Projectile(double x, double y, double vx, double vy, int damage, double maxDistance) {
        this.x = x; this.y = y;
        this.velX = vx; this.velY = vy;
        this.damage = damage;
        this.maxDistance = maxDistance;
    }

    /** 默认无限射程(兼容旧代码) */
    public Projectile(double x, double y, double vx, double vy, int damage) {
        this(x, y, vx, vy, damage, 99999);
    }

    public void update() {
        if (!active) return;
        if (frozen) return; // 时间停止时冻结
        double step = Math.sqrt(velX * velX + velY * velY);
        x += velX;
        y += velY;
        distanceTraveled += step;
        lifetime++;

        if (distanceTraveled >= maxDistance || lifetime >= MAX_LIFETIME) {
            active = false;
        }
    }

    public Bounds getBounds() {
        return new BoundingBox(x - size/2, y - size/2, size, size);
    }

    public void setAOE(boolean aoe) { this.isAOE = aoe; }
    public boolean isAOE() { return isAOE; }
    public void setSize(double s) { this.size = s; }
    public boolean isActive() { return active; }
    public void deactivate() { active = false; }
    public int getDamage() { return damage; }
    public double getVelX() { return velX; }
    /** 时间停止冻结 */
    public void setFrozen(boolean f) { this.frozen = f; }
    public boolean isFrozen() { return frozen; }
}
