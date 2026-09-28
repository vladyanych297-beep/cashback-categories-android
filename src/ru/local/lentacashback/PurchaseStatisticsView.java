package ru.local.lentacashback;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class PurchaseStatisticsView {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    static void append(Activity activity, LinearLayout parent, JSONArray receipts, JSONArray categories,
                       SharedPreferences prefs, LocalDate defaultStart, LocalDate defaultEnd) {
        LinearLayout panel = new LinearLayout(activity);
        panel.setOrientation(LinearLayout.VERTICAL);
        parent.addView(panel);
        render(activity, panel, receipts, categories, prefs, defaultStart, defaultEnd);
    }

    private static void render(Activity activity, LinearLayout panel, JSONArray receipts, JSONArray categories,
                               SharedPreferences prefs, LocalDate defaultStart, LocalDate defaultEnd) {
        panel.removeAllViews();
        LocalDate savedStart = defaultStart, savedEnd = defaultEnd;
        try {
            savedStart = LocalDate.parse(prefs.getString("purchase_report_start", defaultStart.toString()));
            savedEnd = LocalDate.parse(prefs.getString("purchase_report_end", defaultEnd.toString()));
            if (savedEnd.isAfter(LocalDate.now())) savedEnd = LocalDate.now();
            if (savedStart.isAfter(savedEnd)) { savedStart = defaultStart; savedEnd = defaultEnd; }
        } catch (Exception invalidPeriod) { savedStart = defaultStart; savedEnd = defaultEnd; }
        final LocalDate start = savedStart, end = savedEnd;
        text(activity, panel, "Покупки по категориям кешбэка", 21, true);
        text(activity, panel, "Период отчёта: " + DATE.format(start) + " — " + DATE.format(end), 15, true);
        LinearLayout dates = new LinearLayout(activity);
        panel.addView(dates);
        button(activity, dates, "С " + DATE.format(start), () -> pickDate(activity, start, null, end, chosen -> {
            prefs.edit().putString("purchase_report_start", chosen.toString()).putString("purchase_report_end", end.toString()).apply();
            render(activity, panel, receipts, categories, prefs, defaultStart, defaultEnd);
        }), true);
        button(activity, dates, "По " + DATE.format(end), () -> pickDate(activity, end, start, LocalDate.now(), chosen -> {
            prefs.edit().putString("purchase_report_start", start.toString()).putString("purchase_report_end", chosen.toString()).apply();
            render(activity, panel, receipts, categories, prefs, defaultStart, defaultEnd);
        }), true);
        button(activity, panel, "Сбросить период", () -> {
            prefs.edit().remove("purchase_report_start").remove("purchase_report_end").apply();
            render(activity, panel, receipts, categories, prefs, defaultStart, defaultEnd);
        }, false);

        List<PurchaseTotals.Receipt> parsed = new ArrayList<>();
        for (int r = 0; r < receipts.length(); r++) {
            JSONObject receipt = receipts.optJSONObject(r);
            if (receipt == null) continue;
            List<PurchaseTotals.Item> items = new ArrayList<>();
            JSONArray rows = receipt.optJSONArray("items");
            if (rows != null) for (int i = 0; i < rows.length(); i++) {
                JSONObject item = rows.optJSONObject(i);
                if (item != null) items.add(new PurchaseTotals.Item(item.optString("name"), item.optDouble("paid", 0)));
            }
            String id = receipt.optString("url");
            try {
                String popupId = android.net.Uri.parse(id).getQueryParameter("popup_id");
                if (popupId != null && !popupId.isEmpty()) id = "pyaterochka:" + popupId;
            } catch (Exception ignored) {}
            parsed.add(new PurchaseTotals.Receipt(id, receipt.optString("date"), items));
        }
        List<String> names = new ArrayList<>();
        for (int i = 0; i < categories.length(); i++) {
            JSONObject category = categories.optJSONObject(i);
            if (category != null) names.add(category.optString("name"));
        }
        PurchaseTotals.Report report = PurchaseTotals.calculate(parsed, names, start, end);
        text(activity, panel, "Сумма товаров: " + money(report.totalCents), 20, true);
        text(activity, panel, "Чеков за период: " + report.receiptCount + " · позиций: " + report.itemCount, 14, false);
        if (report.earliest != null) text(activity, panel, "Сохранённые чеки: " + DATE.format(report.earliest) + " — " + DATE.format(report.latest), 13, false);
        text(activity, panel, "Расчёт по сохранённым чекам и категориям кешбэка этого магазина. Для актуальных данных нажмите «Обновить».", 13, false);
        if (report.undatedReceipts > 0) text(activity, panel, "Чеки без корректной даты исключены: " + report.undatedReceipts + ".", 13, false);
        if (report.receiptCount == 0) text(activity, panel, "В выбранном периоде нет сохранённых чеков.", 15, false);
        if (report.categories.isEmpty()) text(activity, panel, "Категории кешбэка пока не загружены. Обновите данные магазина.", 15, false);
        List<PurchaseTotals.CategoryTotal> empty = new ArrayList<>();
        for (PurchaseTotals.CategoryTotal category : report.categories) {
            if (category.items == 0) empty.add(category);
            else categoryRow(activity, panel, category);
        }
        if (report.unmatchedItems > 0) {
            text(activity, panel, "Без подходящей категории кешбэка — " + money(report.unmatchedCents), 16, true);
            text(activity, panel, "Позиций: " + report.unmatchedItems + ". Название товара не соответствует доступным категориям кешбэка по правилам приложения.", 13, false);
            products(activity, panel, report.unmatchedProducts);
        }
        if (report.overlappingItems > 0) text(activity, panel, "Есть товары, подходящие к нескольким категориям. Суммы строк не складываются; общий итог учитывает каждый товар один раз.", 13, false);
        if (!empty.isEmpty()) {
            LinearLayout zeroRows = new LinearLayout(activity);
            zeroRows.setOrientation(LinearLayout.VERTICAL);
            zeroRows.setVisibility(View.GONE);
            button(activity, panel, "Категории без покупок (" + empty.size() + ")", () ->
                    zeroRows.setVisibility(zeroRows.getVisibility() == View.GONE ? View.VISIBLE : View.GONE), false);
            panel.addView(zeroRows);
            for (PurchaseTotals.CategoryTotal category : empty) categoryRow(activity, zeroRows, category);
        }
    }

    private interface ChosenDate { void accept(LocalDate date); }
    private static void pickDate(Activity activity, LocalDate initial, LocalDate min, LocalDate max, ChosenDate chosen) {
        DatePickerDialog dialog = new DatePickerDialog(activity, (view, year, month, day) ->
                chosen.accept(LocalDate.of(year, month + 1, day)), initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        if (min != null) dialog.getDatePicker().setMinDate(min.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.getDatePicker().setMaxDate(max.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli());
        dialog.show();
    }

    private static void categoryRow(Activity activity, LinearLayout panel, PurchaseTotals.CategoryTotal category) {
        text(activity, panel, category.name + " — " + money(category.cents), 16, true);
        text(activity, panel, "Позиций в чеках: " + category.items, 13, false);
        if (!category.products.isEmpty()) products(activity, panel, category.products);
    }

    private static void products(Activity activity, LinearLayout panel, java.util.Map<String, PurchaseTotals.ProductTotal> products) {
        LinearLayout rows = new LinearLayout(activity);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setVisibility(View.GONE);
        button(activity, panel, "Товары (" + products.size() + ")", () ->
                rows.setVisibility(rows.getVisibility() == View.GONE ? View.VISIBLE : View.GONE), false);
        panel.addView(rows);
        for (PurchaseTotals.ProductTotal product : PurchaseTotals.sortedProducts(products)) {
            text(activity, rows, product.name + " — " + money(product.cents), 14, false);
            text(activity, rows, "Позиций в чеках: " + product.items, 12, false);
        }
    }

    private static String money(long cents) { return String.format(new Locale("ru", "RU"), "%,.2f ₽", cents / 100.0); }
    private static void text(Activity activity, LinearLayout parent, String value, int size, boolean strong) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(strong ? Color.rgb(15, 44, 88) : Color.rgb(54, 65, 82));
        view.setPadding(0, dp(activity, strong ? 12 : 4), 0, dp(activity, 4));
        parent.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    private static void button(Activity activity, LinearLayout parent, String label, Runnable action, boolean weighted) {
        Button button = new Button(activity);
        button.setText(label);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setOnClickListener(v -> action.run());
        parent.addView(button, weighted ? new LinearLayout.LayoutParams(0, -2, 1) : new LinearLayout.LayoutParams(-1, -2));
    }

    private static int dp(Activity activity, int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }
}
