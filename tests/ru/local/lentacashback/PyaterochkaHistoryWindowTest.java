package ru.local.lentacashback;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;

public final class PyaterochkaHistoryWindowTest {
    public static void main(String[] args) {
        for (LocalDate today = LocalDate.of(2024, 1, 1);
             today.isBefore(LocalDate.of(2028, 1, 1)); today = today.plusDays(1)) {
            PyaterochkaHistoryWindow window = new PyaterochkaHistoryWindow(today);
            Set<YearMonth> selected = new HashSet<>();
            for (int i = 0; i < window.monthCount; i++) selected.add(YearMonth.from(window.month(i)));
            for (int age = 0; age < 100; age++) {
                LocalDate date = today.minusDays(age);
                check(window.contains(date.toString()), "Missing day " + date + " for " + today);
                check(selected.contains(YearMonth.from(date)), "Missing month " + date + " for " + today);
            }
            check(!window.contains(today.minusDays(100).toString()), "Old receipt included for " + today);
            check(!window.contains(today.plusDays(1).toString()), "Future receipt included for " + today);
            check(!window.contains(""), "Missing date accepted");
            check(!window.contains("2026-02-30"), "Invalid date accepted");
            check(selected.size() == window.monthCount, "Duplicate months for " + today);
            check(YearMonth.from(window.month(window.monthCount - 1)).equals(YearMonth.from(window.start)), "Wrong last month");
        }
        PyaterochkaHistoryWindow september = new PyaterochkaHistoryWindow(LocalDate.of(2026, 9, 28));
        check(september.start.equals(LocalDate.of(2026, 6, 21)), "Wrong September cutoff");
        check(september.monthCount == 4, "June must be loaded");
        PyaterochkaHistoryWindow february = new PyaterochkaHistoryWindow(LocalDate.of(2026, 2, 1));
        check(february.monthCount == 5, "Five months can intersect 100 days");
        check(february.month(4).equals(LocalDate.of(2025, 10, 1)), "Wrong year when crossing January");
        System.out.println("PASS: 100-day boundaries and month coverage for every day in 2024-2027.");
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
