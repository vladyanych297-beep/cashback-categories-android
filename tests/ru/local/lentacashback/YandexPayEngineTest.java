package ru.local.lentacashback;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public final class YandexPayEngineTest {
    public static void main(String[] args) {
        LocalDate today = LocalDate.of(2026, 9, 29);
        YandexPayEngine.Operation first = op("a", "2026-06-22", "Тестовый супермаркет", "Супермаркеты", 10010, true);
        YandexPayEngine.Offer all = offer("Все покупки", 1, "cashback", true);
        YandexPayEngine.Offer electronics = offer("Электроника", 5, "cashback", true);
        YandexPayEngine.Offer discount = offer("Еда и Деливери", 50, "discount", true);
        YandexPayEngine.Report report = YandexPayEngine.calculate(Arrays.asList(first, first,
                op("b", "2026-09-29", "Тестовая электроника", "Электроника и бытовая техника", 100000, true),
                op("c", "2026-09-28", "Другой магазин", "Электроника и бытовая техника", 50000, false),
                op("d", "2026-09-15", "Яндекс Еда", "Фастфуд", 20000, true),
                op("old", "2026-06-21", "Старый магазин", "Супермаркеты", 999999, true),
                op("future", "2026-09-30", "Будущий магазин", "Супермаркеты", 999999, true),
                op("transfer", "2026-09-28", "Перевод", "Переводы", 999999, true),
                op("refund", "2026-09-28", "Возврат", "Возврат", 999999, true),
                op("invalid", "", "Покупка", "Супермаркеты", 999999, true)),
                Arrays.asList(all, all, electronics, discount), today);
        check(report.total == 180010 && report.count == 4, "Wrong 100-day total, duplicates, or transfer filtering");
        check(report.groups.size() == 3 && report.groups.get(0).cents == 150000, "Bank category grouping failed");
        long groupTotal = 0;
        for (YandexPayEngine.Group group : report.groups) groupTotal += group.cents;
        check(groupTotal == report.total, "Bank categories double-count purchases");
        check(report.recommendations.size() == 3, "Duplicate offer retained");
        check(recommendation(report, "Все покупки").spend == 130010, "Non-Pay cards included in Pay-only offer");
        check(recommendation(report, "Электроника").spend == 100000, "Category alias or card restriction failed");
        check(report.recommendations.get(0).offer.name.equals("Электроника"), "Recommendations not ranked by expected benefit");
        check(recommendation(report, "Еда и Деливери").spend == 20000, "Delivery purchase not recognized");
        check(recommendation(report, "Еда и Деливери").estimate() == 0, "Delivery discount counted as cashback on whole order");
        check(!YandexPayEngine.matches(offer("Кинопоиск", 10, "cashback", true),
                op("sub", "2026-09-01", "Кинопоиск", "Кинопоиск", 1000, true)), "Subscription confused with cinema tickets");
        check(!YandexPayEngine.matches(offer("ОСАГО", 100, "cashback", false),
                op("insurance", "2026-09-01", "Другая страховая компания", "ОСАГО", 1000, false)), "Partner condition ignored");
        check(YandexPayEngine.matches(offer("ОСАГО", 100, "cashback", false),
                op("insurance", "2026-09-01", "ОСАГО Яндекс Забота", "Страхование", 1000, false)), "Any-card partner offer failed");
        check(YandexPayEngine.calculate(Collections.emptyList(), Collections.emptyList(), today).count == 0, "Empty history failed");
        for (LocalDate day = LocalDate.of(2024, 1, 1); day.isBefore(LocalDate.of(2028, 1, 1)); day = day.plusDays(1)) {
            YandexPayEngine.Report boundary = YandexPayEngine.calculate(Arrays.asList(
                    op("in", day.minusDays(99).toString(), "Покупка", "Супермаркеты", 100, true),
                    op("out", day.minusDays(100).toString(), "Покупка", "Супермаркеты", 200, true)), Arrays.asList(all), day);
            check(boundary.total == 100 && boundary.count == 1, "Wrong period for " + day);
        }
        System.out.println("PASS: Yandex Pay 100-day period, source cards, bank categories, duplicates, discounts, and partner restrictions.");
    }
    private static YandexPayEngine.Operation op(String id, String date, String merchant, String category, long cents, boolean pay) {
        return new YandexPayEngine.Operation(id, date, merchant, category, cents, pay);
    }
    private static YandexPayEngine.Offer offer(String name, double rate, String kind, boolean pay) {
        return new YandexPayEngine.Offer(name, rate, kind, pay, "", false);
    }
    private static YandexPayEngine.Recommendation recommendation(YandexPayEngine.Report report, String name) {
        for (YandexPayEngine.Recommendation item : report.recommendations) if (item.offer.name.equals(name)) return item;
        throw new AssertionError("Offer missing: " + name);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
