package com.example.bongocat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

/**
 * 前台服务：负责把 CatView 挂到 WindowManager 上,成为悬浮宠物。
 * 寿命伴随通知常驻（避免被系统回收）,App 关闭后猫咪仍在。
 */
public class PetService extends Service implements CatView.DragListener {

    private static final int NOTIFY_ID = 1;
    private static final String CHANNEL_ID = "pet_channel";

    // 供 UI 简单判断当前是否在运行（demo 用静态标记）
    private static boolean sRunning = false;

    public static boolean isRunning() {
        return sRunning;
    }

    private WindowManager windowManager;
    private CatView catView;
    private WindowManager.LayoutParams layoutParams;

    /* ---------------- 生命周期 ---------------- */

    @Override
    public void onCreate() {
        super.onCreate();
        sRunning = true;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        createNotificationChannel();
        startForeground(NOTIFY_ID, buildNotification());
        addCatToWindow();
    }

    @Override
    public void onDestroy() {
        sRunning = false;
        if (catView != null && windowManager != null) {
            try {
                windowManager.removeView(catView);
            } catch (IllegalArgumentException ignored) {
                // 视图可能已不在窗口上
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /* ---------------- 悬浮窗 ---------------- */

    private void addCatToWindow() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int size = Math.round(110f * dm.density); // 110dp 的猫

        catView = new CatView(this);
        catView.setDragListener(this);

        layoutParams = new WindowManager.LayoutParams(
                size,
                size,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = dm.widthPixels - size - Math.round(16f * dm.density);
        layoutParams.y = Math.round(200f * dm.density);

        windowManager.addView(catView, layoutParams);
    }

    /** 拖动回调：更新窗口位置,实现"猫跟着走"。 */
    @Override
    public void onDrag(float dxPx, float dyPx) {
        if (layoutParams != null && catView != null && catView.isAttachedToWindow()) {
            layoutParams.x += Math.round(dxPx);
            layoutParams.y += Math.round(dyPx);
            // 别把猫拖出屏幕
            layoutParams.x = Math.max(0, layoutParams.x);
            layoutParams.y = Math.max(0, layoutParams.y);
            try {
                windowManager.updateViewLayout(catView, layoutParams);
            } catch (IllegalArgumentException ignored) {
                // 窗口已被移除
            }
        }
    }

    /* ---------------- 通知 ---------------- */

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "悬浮宠物",
                    NotificationManager.IMPORTANCE_MIN);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("邦戈猫在陪你")
                .setContentText("点它一下会打鼓,拖动可以移动")
                .setOngoing(true)
                .build();
    }

    /* ---------------- 便捷启动/停止 ---------------- */

    public static void startPet(Context ctx) {
        ctx.startForegroundService(new Intent(ctx, PetService.class));
    }

    public static void stopPet(Context ctx) {
        ctx.stopService(new Intent(ctx, PetService.class));
    }
}