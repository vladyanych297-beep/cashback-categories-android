package ru.local.lentacashback;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

final class PyaterochkaHistoryWindow {
    final LocalDate end;
    final LocalDate start;
    final int monthCount;

    PyaterochkaHistoryWindow(LocalDate today) {
        end = today;
        start = today.minusDays(99);
        monthCount = (int) ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1;
    }

    LocalDate month(int index) {
        return end.withDayOfMonth(1).minusMonths(index);
    }

    boolean contains(String date) {
        try {
            LocalDate value = LocalDate.parse(date);
            return !value.isBefore(start) && !value.isAfter(end);
        } catch (Exception invalidDate) {
            return false;
        }
    }
}
