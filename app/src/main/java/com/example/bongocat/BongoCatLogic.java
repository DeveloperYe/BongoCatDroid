package com.example.bongocat;

import android.os.SystemClock;

/**
 * 猫咪的小脑瓜：管理"打鼓/发呆/好奇/拖动"等状态的切换与计时。
 * 用 SystemClock 计算时间，保证跨绘制帧稳定。
 */
public class BongoCatLogic {

    public enum Mood { IDLE, TAP, CURIOUS, DRAG }

    private Mood mood = Mood.IDLE;

    // 动画时间戳（相对系统开机时间，单位 ms）
    private long moodStart = now();

    // 发呆时的抽筋/眨眼计时
    private long idleStart = now();
    private long blinkAt = now() + 1500;

    // 发呆小动作：尾巴左右摆、身体轻轻上下起伏
    private float bobOffset = 0f;

    private static long now() {
        return SystemClock.elapsedRealtime();
    }

    public Mood getMood() {
        return mood;
    }

    public long elapsedMs() {
        return now() - moodStart;
    }

    /** 触发"敲鼓"：两只小爪爪往下拍。 */
    public void triggerTap() {
        mood = Mood.TAP;
        moodStart = now();
        idleStart = now();
    }

    /** 全局触摸反应：猫咪好奇地抬一下头。 */
    public void triggerCurious() {
        if (mood == Mood.TAP || mood == Mood.DRAG) {
            return; // 正忙,不打断
        }
        mood = Mood.CURIOUS;
        moodStart = now();
        idleStart = now();
    }

    public void startDrag() {
        mood = Mood.DRAG;
        moodStart = now();
    }

    public void update(float dtSeconds) {
        long t = now();

        // 独立的"发呆"循环：抽筋、眨眼、随机 bob
        if (mood == Mood.IDLE) {
            bobOffset = (float) Math.sin((t - idleStart) / 1000.0 * 4.0) * 3f;
        } else {
            bobOffset = 0f;
            // 非 IDLE 状态到期后回到发呆
            long dur = mood == Mood.TAP ? 420 : (mood == Mood.CURIOUS ? 900 : 5000);
            if (t - moodStart > dur) {
                mood = Mood.IDLE;
                idleStart = now();
                blinkAt = now() + 400 + (long) (Math.random() * 2000);
            }
        }

        // 眨眼预算：每隔 2~4 秒眨一次
        if (t >= blinkAt) {
            blinkAt = t + 200 + (long) (Math.random() * 3200);
        }
    }

    /** 是否为"正在眨眼"，用于绘制半闭合的眼皮。 */
    public boolean isBlinking(long tickMs) {
        long t = now();
        if (mood == Mood.TAP) {
            return true; // 打鼓时眯眼
        }
        return Math.abs(t - blinkAt) < 160;
    }

    /** 打鼓压力：0~1，1 = 爪爪拍在鼓上最用力的一刻。 */
    public float tapProgress() {
        if (mood != Mood.TAP) return 0f;
        long ms = now() - moodStart;
        if (ms > 360) return 0f;
        float p = ms / 360f;
        // 先快后慢地压到底，再回来
        return (float) (Math.sin(p * Math.PI) * 0.9);
    }

    public float getBobOffset() {
        return bobOffset;
    }
}