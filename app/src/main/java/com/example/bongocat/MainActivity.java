package com.example.bongocat;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * 入口页：引导授权悬浮窗权限 + 通知权限,并提供开启/关闭宠物的开关。
 */
public class MainActivity extends AppCompatActivity {

    private Button toggleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        // Android 13+ 需要通知权限,否则前台服务通知不显示
        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshButtonText();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));
        int padV = dp(10), padH = dp(8);

        TextView title = new TextView(this);
        title.setText(getString(R.string.app_name));
        title.setTextSize(26f);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView hint = new TextView(this);
        hint.setText(getString(R.string.drag_hint));
        hint.setTextSize(15f);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(padH, dp(20), padH, padV);
        root.addView(hint);

        toggleButton = new Button(this);
        toggleButton.setPadding(padH, padV, padH, padV);
        toggleButton.setOnClickListener(v -> onTogglePet());
        root.addView(toggleButton);

        TextView touchTitle = new TextView(this);
        touchTitle.setText(getString(R.string.global_touch_title));
        touchTitle.setTextSize(18f);
        touchTitle.setPadding(padH, dp(40), padH, padV);
        root.addView(touchTitle);

        TextView touchMsg = new TextView(this);
        touchMsg.setText(getString(R.string.global_touch_msg));
        touchMsg.setTextSize(14f);
        touchMsg.setPadding(padH, 0, padH, padV);
        root.addView(touchMsg);

        Button touchBtn = new Button(this);
        touchBtn.setText(getString(R.string.enable_global_touch));
        touchBtn.setPadding(padH, padV, padH, padV);
        touchBtn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(touchBtn);

        setContentView(root);
        refreshButtonText();
    }

    private void onTogglePet() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (PetService.isRunning()) {
            PetService.stopPet(this);
            Toast.makeText(this, getString(R.string.pet_stopped), Toast.LENGTH_SHORT).show();
        } else {
            PetService.startPet(this);
            Toast.makeText(this, getString(R.string.pet_running), Toast.LENGTH_SHORT).show();
        }
        refreshButtonText();
    }

    private void requestOverlayPermission() {
        Toast.makeText(this, R.string.overlay_permission_msg, Toast.LENGTH_LONG).show();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        refreshButtonText();
    }

    private void refreshButtonText() {
        if (toggleButton == null) return;
        if (!Settings.canDrawOverlays(this)) {
            // 还没授权：点击会跳去系统设置授权
            toggleButton.setEnabled(true);
            toggleButton.setText(R.string.open_settings);
        } else if (PetService.isRunning()) {
            toggleButton.setText(R.string.stop_pet);
        } else {
            toggleButton.setText(R.string.start_pet);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}