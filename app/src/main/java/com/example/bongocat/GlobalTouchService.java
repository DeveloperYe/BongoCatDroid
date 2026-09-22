package com.example.bongocat;

import android.accessibilityservice.AccessibilityService;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.util.WeakHashMap;

/**
 * 全局触摸感知（可选,无需 root）。
 * 通过 AccessibilityService 接收全局触摸事件（开始/结束）,让猫咪知道你
 * 正在屏幕哪个角落忙活。不读取任何窗口内容,只"听"触摸的存在。
 */
public class GlobalTouchService extends AccessibilityService {

    private static final String TAG = "GlobalTouch";

    // 持有当前存活 CatView 的弱引用,服务死了不影响猫咪自身
    private static final WeakHashMap<CatView, Boolean> cats = new WeakHashMap<>();

    /** 由 CatView.onAttachedToWindow 注册。 */
    public static void registerCat(CatView v) {
        cats.put(v, Boolean.TRUE);
    }

    public static void unregisterCat(CatView v) {
        cats.remove(v);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int type = event.getEventType();
        // 只要感知"你开始/结束了在某处触摸",就让它好奇一下
        if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START
                || type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END) {
            boolean handled = false;
            for (CatView v : cats.keySet()) {
                if (v != null && v.isShown()) {
                    v.onGlobalTouch();
                    handled = true;
                }
            }
            if (handled) Log.v(TAG, "小猫感知到全局触摸");
        }
    }

    @Override
    public void onInterrupt() {
        // 服务被中断,暂无需处理
    }
}