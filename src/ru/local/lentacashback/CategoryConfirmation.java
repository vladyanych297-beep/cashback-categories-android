package ru.local.lentacashback;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

final class CategoryConfirmation {
    interface Approved { void accept(JSONArray names); }
    static String signature(JSONArray categories, String period, int limit, java.time.LocalDate checked) {
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < categories.length(); i++) {
            JSONObject row = categories.optJSONObject(i);
            if (row != null) rows.add(row.optString("name").replace('\u00a0', ' ').trim() + "\t" + row.optDouble("rate"));
        }
        Collections.sort(rows);
        return CashbackPeriod.key(period, checked) + ":" + limit + ":" + rows.toString();
    }
    static void show(Activity activity, String store, String period, List<RecommendationEngine.Recommendation> ranked,
                     int limit, int required, Approved approved) {
        if (ranked.isEmpty()) { Toast.makeText(activity, "Нет доступных категорий. Обновите данные.", Toast.LENGTH_LONG).show(); return; }
        String[] labels = new String[ranked.size()]; boolean[] selected = new boolean[ranked.size()];
        int initial = 0;
        for (int i = 0; i < ranked.size(); i++) {
            RecommendationEngine.Recommendation row = ranked.get(i);
            labels[i] = String.format(new Locale("ru", "RU"), "%s · %.0f%%\nПокупки %.2f ₽ · оценка кешбэка %.2f ₽", row.name, row.rate, row.spend, row.score);
            selected[i] = row.spend > 0 && initial < limit;
            if (selected[i]) initial++;
        }
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(store + " — " + period + "\nПроверьте выбор: " + (required == limit ? "ровно " : "до ") + limit + " категорий")
                .setMultiChoiceItems(labels, selected, (d, index, checked) -> selected[index] = checked)
                .setNegativeButton("Отмена", null).setPositiveButton("Подтвердить выбор", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            JSONArray names = new JSONArray();
            for (int i = 0; i < selected.length; i++) if (selected[i]) names.put(ranked.get(i).name);
            if (names.length() < required || names.length() > limit) {
                Toast.makeText(activity, required == limit ? "Выберите ровно " + limit + " категорий." : "Выберите от " + required + " до " + limit + " категорий.", Toast.LENGTH_LONG).show(); return;
            }
            dialog.dismiss(); approved.accept(names);
        }));
        dialog.show();
    }
}
