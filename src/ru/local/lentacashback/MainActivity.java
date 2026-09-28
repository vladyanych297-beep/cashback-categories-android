package ru.local.lentacashback;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private AppUpdater updater;
    private TextView updateStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(Color.rgb(15, 68, 135));
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(20), dp(18), dp(28));
        root.setBackgroundColor(Color.rgb(246, 248, 252));
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(dp(18), dp(20) + insets.getSystemWindowInsetTop(), dp(18), dp(28) + insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.addView(root);
        setContentView(scroll);
        addText(root, "Категории кешбэка", 27, Color.rgb(15, 44, 88));
        addText(root, "Каждый магазин работает автономно: отдельный вход, история покупок, категории и рекомендации.", 15, Color.rgb(70, 78, 91));
        updateStatus = addText(root, "Проверяю обновления…", 13, Color.rgb(15, 68, 135));
        addStore(root, "Лента", "Чеки и категории Ленты", Color.rgb(0, 78, 170), LentaActivity.class);
        addStore(root, "Магнит", "История операций и категории Магнит Плюс", Color.rgb(218, 31, 38), MagnitActivity.class);
        addStore(root, "Пятёрочка", "Чеки X5 Клуба и персональные предложения", Color.rgb(22, 145, 72), PyaterochkaActivity.class);
        addText(root, "Неофициальное приложение. Не связано с торговыми сетями.", 12, Color.rgb(100, 105, 115));
        addText(root, "© 2026 ESI.Company", 12, Color.rgb(100, 105, 115));
        updater = new AppUpdater(this);
        updater.handleInstallStatus(getIntent());
        updater.checkAtLaunch();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (updater != null) updater.handleInstallStatus(intent);
    }

    @Override protected void onResume() {
        super.onResume();
        if (updater != null) updater.onResume();
    }

    void showUpdateStatus(String text) {
        if (updateStatus != null) updateStatus.setText(text);
    }

    private void addStore(LinearLayout root, String title, String subtitle, int color, Class<?> target) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(14), dp(18), dp(14));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(2), color);
        card.setBackground(bg);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, dp(12), 0, 0);
        root.addView(card, cardParams);
        addText(card, title, 22, color);
        addText(card, subtitle, 14, Color.rgb(68, 76, 88));
        Button open = new Button(this);
        open.setText("Открыть");
        open.setAllCaps(false);
        open.setTextSize(15);
        open.setOnClickListener(v -> startActivity(new Intent(this, target)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(52));
        p.setMargins(0, dp(8), 0, 0);
        card.addView(open, p);
    }

    private TextView addText(LinearLayout root, String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setPadding(0, dp(6), 0, dp(4));
        root.addView(view, new LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private int dp(int value) { return (int)(value * getResources().getDisplayMetrics().density + .5f); }
}
