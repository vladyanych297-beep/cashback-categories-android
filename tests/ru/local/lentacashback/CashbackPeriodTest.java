package ru.local.lentacashback;
import java.time.LocalDate;

public final class CashbackPeriodTest {
    public static void main(String[] args) {
        LocalDate september = LocalDate.of(2026, 9, 29);
        check(CashbackPeriod.key("Выберите категории на октябрь", september).equals("2026-10"));
        check(CashbackPeriod.fresh("2026-09-29", "Категории в сентябре", september));
        check(CashbackPeriod.fresh("2026-09-29", "Категории на октябрь", september));
        check(!CashbackPeriod.fresh("2026-09-29", "Категории на октябрь", LocalDate.of(2026, 10, 1)));
        check(!CashbackPeriod.fresh("2026-09-29", "Категории на август", september));
        check(!CashbackPeriod.fresh("", "Категории на октябрь", september));
        check(!CashbackPeriod.fresh("2026-09-30", "Категории на октябрь", september));
        check(!CashbackPeriod.fresh("2026-09-29", "Категории на октябрь 2025", september));
        check(!CashbackPeriod.fresh("2026-09-29", "Категории магазина", september));
        LocalDate december = LocalDate.of(2026, 12, 31);
        check(CashbackPeriod.key("на январь", december).equals("2027-01"));
        check(CashbackPeriod.fresh("2026-12-31", "на январь", december));
        check(!CashbackPeriod.fresh("2026-12-31", "на январь", LocalDate.of(2027, 1, 1)));
        check(CashbackPeriod.key("на май", september).equals("2026-05"));
        check(CashbackPeriod.key("в мае", september).equals("2026-05"));
        check(CashbackPeriod.key("магазины", september).isEmpty());
        System.out.println("PASS: monthly expiry, upcoming periods, year rollover, unknown periods and date validation.");
    }
    private static void check(boolean value) { if (!value) throw new AssertionError("Period check failed"); }
}
