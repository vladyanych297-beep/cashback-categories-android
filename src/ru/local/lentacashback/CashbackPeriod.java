package ru.local.lentacashback;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CashbackPeriod {
    private static final String[] MONTHS = {"январ", "феврал", "март", "апрел", "ма", "июн", "июл", "август", "сентябр", "октябр", "ноябр", "декабр"};
    static String key(String heading, LocalDate checked) {
        String text = heading.toLowerCase(Locale.ROOT).replace('ё', 'е');
        int month = 0;
        for (int i = 0; i < MONTHS.length; i++) {
            String stem = MONTHS[i];
            if (Pattern.compile("(?<![\\p{L}])" + stem + (i == 4 ? "[йяе]" : "[\\p{L}]*") + "(?![\\p{L}])").matcher(text).find()) { month = i + 1; break; }
        }
        if (month == 0) return "";
        Matcher year = Pattern.compile("\\b(20[0-9]{2})\\b").matcher(text);
        int value = year.find() ? Integer.parseInt(year.group(1)) : checked.getYear();
        if (!text.matches(".*20[0-9]{2}.*") && checked.getMonthValue() == 12 && month == 1) value++;
        return YearMonth.of(value, month).toString();
    }
    static boolean fresh(String checkedDate, String period, LocalDate today) {
        try {
            LocalDate checked = LocalDate.parse(checkedDate);
            if (checked.isAfter(today) || !YearMonth.from(checked).equals(YearMonth.from(today))) return false;
            String key = key(period, checked);
            return key.equals(YearMonth.from(today).toString()) || key.equals(YearMonth.from(today.plusMonths(1)).toString());
        } catch (Exception invalid) { return false; }
    }
}
