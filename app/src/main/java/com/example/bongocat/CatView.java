package com.example.bongocat;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

/**
 * 悬浮窗里的猫咪本体。
 * 用 Canvas 纯代码绘制一只"邦戈猫"，敲的是屏幕本身。
 * - 单击：小爪爪往下拍一下（左右交替,像打鼓）
 * - 长按拖动：猫咪跟着手指走
 * - 外部（无障碍服务）：随机好奇地抬下头
 */
public class CatView extends View {

    public interface DragListener {
        void onDrag(float dxPx, float dyPx);
    }

    private final BongoCatLogic logic = new BongoCatLogic();
    private DragListener dragListener;

    // 画笔（避免每帧重复创建）
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 上一帧时间，用于帧间隔
    private long lastFrame = 0;
    private boolean tapSide = false; // false=左爪,true=右爪

    // 触摸状态
    private final int touchSlop;
    private float downX, downY;
    private float lastX, lastY;
    private boolean dragging;

    public CatView(Context context) {
        super(context);
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(3f);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        lastFrame = System.currentTimeMillis();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 挂到窗口后,让无障碍服务能找到我
        if (isInEditMode()) return;
        GlobalTouchService.registerCat(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (isInEditMode()) return;
        GlobalTouchService.unregisterCat(this);
    }

    public void setDragListener(DragListener l) {
        this.dragListener = l;
    }

    /** 全局触摸：猫咪好奇。 */
    public void onGlobalTouch() {
        logic.triggerCurious();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        long now = System.currentTimeMillis();
        float dt = (now - lastFrame) / 1000f;
        if (dt > 0.05f) dt = 0.05f; // 帧间隔上限,避免卡顿后猛跳
        lastFrame = now;
        logic.update(dt);

        drawCat(canvas);

        // 常驻动画循环（vsync 对齐）。后续可优化为"空闲时停止自绘"以省电。
        postInvalidateOnAnimation();
    }

    private void drawCat(Canvas c) {
        float w = getWidth(), h = getHeight();
        float u = Math.min(w, h) / 100f; // 单位，基于短边
        float cx = w / 2f;

        float bob = logic.getBobOffset() * u;
        float headCy = 30f * u + bob;

        // ---- 尾巴（先画,垫在身体后面）----
        fill.setColor(0xFFE79A32);
        Path tail = new Path();
        tail.moveTo(cx - 22f * u, 66f * u);
        tail.cubicTo(cx - 34f * u, 62f * u, cx - 38f * u, 48f * u,
                cx - 30f * u, 46f * u);
        tail.cubicTo(cx - 22f * u, 44f * u, cx - 24f * u, 54f * u, cx - 18f * u, 56f * u);
        fill.setStyle(Paint.Style.STROKE);
        fill.setStrokeWidth(5f * u);
        fill.setStrokeCap(Paint.Cap.ROUND);
        c.drawPath(tail, fill);
        fill.setStyle(Paint.Style.FILL);

        // ---- 身体 ----
        fill.setColor(0xFFE79A32);
        c.drawRoundRect(new RectF(cx - 26f * u, 48f * u, cx + 26f * u, 74f * u),
                18f * u, 18f * u, fill);
        // 肚皮
        fill.setColor(0xFFFFF3D6);
        c.drawRoundRect(new RectF(cx - 15f * u, 54f * u, cx + 15f * u, 72f * u),
                10f * u, 10f * u, fill);

        // ---- 头 ----
        fill.setColor(0xFFE79A32);
        c.drawCircle(cx, headCy, 20f * u, fill);

        // 耳朵
        Path earL = new Path();
        earL.moveTo(cx - 18f * u, headCy - 8f * u);
        earL.lineTo(cx - 26f * u, headCy - 26f * u);
        earL.lineTo(cx - 6f * u, headCy - 15f * u);
        earL.close();
        c.drawPath(earL, fill);

        Path earR = new Path();
        earR.moveTo(cx + 18f * u, headCy - 8f * u);
        earR.lineTo(cx + 26f * u, headCy - 26f * u);
        earR.lineTo(cx + 6f * u, headCy - 15f * u);
        earR.close();
        c.drawPath(earR, fill);

        // 耳内粉色
        fill.setColor(0xFFF6C5C5);
        Path inL = new Path();
        inL.moveTo(cx - 16f * u, headCy - 8f * u);
        inL.lineTo(cx - 22f * u, headCy - 21f * u);
        inL.lineTo(cx - 8f * u, headCy - 13f * u);
        inL.close();
        c.drawPath(inL, fill);
        Path inR = new Path();
        inR.moveTo(cx + 16f * u, headCy - 8f * u);
        inR.lineTo(cx + 22f * u, headCy - 21f * u);
        inR.lineTo(cx + 8f * u, headCy - 13f * u);
        inR.close();
        c.drawPath(inR, fill);
        fill.setColor(0xFFE79A32);

        // 眼睛
        boolean blink = logic.isBlinking(System.currentTimeMillis());
        float eyeY = headCy + 2f * u;
        fill.setColor(Color.WHITE);
        c.drawCircle(cx - 9f * u, eyeY, 4.6f * u, fill);
        c.drawCircle(cx + 9f * u, eyeY, 4.6f * u, fill);
        fill.setColor(0xFF292929);
        if (blink) {
            // 眯成一条缝
            stroke.setColor(0xFF292929);
            stroke.setStrokeWidth(2.2f * u);
            c.drawLine(cx - 13f * u, eyeY, cx - 5f * u, eyeY, stroke);
            c.drawLine(cx + 5f * u, eyeY, cx + 13f * u, eyeY, stroke);
        } else {
            c.drawCircle(cx - 9f * u, eyeY, 2.4f * u, fill);
            c.drawCircle(cx + 9f * u, eyeY, 2.4f * u, fill);
        }

        // 鼻子 + 嘴
        fill.setColor(0xFF2E2E2E);
        Path nose = new Path();
        nose.moveTo(cx, headCy + 7f * u);
        nose.lineTo(cx - 3f * u, headCy + 10f * u);
        nose.lineTo(cx + 3f * u, headCy + 10f * u);
        nose.close();
        c.drawPath(nose, fill);
        stroke.setColor(0xFF2E2E2E);
        stroke.setStrokeWidth(1.8f * u);
        c.drawLine(cx - 6f * u, headCy + 12f * u, cx - 3f * u, headCy + 11f * u, stroke);
        c.drawLine(cx + 6f * u, headCy + 12f * u, cx + 3f * u, headCy + 11f * u, stroke);

        // 胡须
        stroke.setStrokeWidth(1.4f * u);
        float ws = 15f * u, wy = headCy + 6f * u;
        c.drawLine(cx - ws, wy, cx - ws - 8f * u, wy - 2f * u, stroke);
        c.drawLine(cx - ws, wy + 3f * u, cx - ws - 7f * u, wy + 5f * u, stroke);
        c.drawLine(cx + ws, wy, cx + ws + 8f * u, wy - 2f * u, stroke);
        c.drawLine(cx + ws, wy + 3f * u, cx + ws + 7f * u, wy + 5f * u, stroke);

        // ---- 鼓（两个鼓面,猫坐在后面拍）----
        drawDrum(c, cx - 16f * u, 82f * u, u);
        drawDrum(c, cx + 16f * u, 82f * u, u);

        // ---- 爪爪,会随着打鼓下压 ----
        float tap = logic.tapProgress();
        drawPaw(c, cx - 16f * u, 82f * u, u, tap, false);
        drawPaw(c, cx + 16f * u, 82f * u, u, tap, true);
    }

    private void drawDrum(Canvas c, float dx, float dy, float u) {
        fill.setColor(0xFFB5835A);
        c.drawOval(new RectF(dx - 12f * u, dy - 10f * u, dx + 12f * u, dy + 8f * u), fill);
        fill.setColor(0xFFE0C08C);
        c.drawOval(new RectF(dx - 12f * u, dy - 12f * u, dx + 12f * u, dy - 4f * u), fill);
    }

    /**
     * 画一只爪爪。某一只爪在打鼓那一下压到鼓面,另一只保持抬起。
     * @param side true=右爪
     */
    private void drawPaw(Canvas c, float dx, float drumY, float u, float tap, boolean side) {
        // 只有轮到这一侧的拍子才下压,另一侧保持拿起
        float lift = (tap > 0f && side == tapSide) ? tap : 0f;
        float py = drumY - 8f * u - lift * 14f * u; // 下压 = py 变大,盖住鼓面
        fill.setColor(0xFFF2A03D);
        c.drawOval(new RectF(dx - 7f * u, py - 4f * u, dx + 7f * u, py + 8f * u), fill);
        // 爪垫
        fill.setColor(0xFFF6C5C5);
        c.drawCircle(dx, py + 3f * u, 3f * u, fill);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = lastX = x;
                downY = lastY = y;
                dragging = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!dragging && (Math.abs(x - downX) > touchSlop || Math.abs(y - downY) > touchSlop)) {
                    dragging = true;
                    logic.startDrag();
                    if (dragListener != null) getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (dragging && dragListener != null) {
                    dragListener.onDrag(x - lastX, y - lastY);
                    lastX = x;
                    lastY = y;
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    logic.triggerCurious(); // 放下时好奇探头
                } else {
                    // 单击 = 打鼓（左右交替）
                    tapSide = !tapSide;
                    logic.triggerTap();
                }
                return true;
            default:
                return false;
        }
    }
}