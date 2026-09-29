package ru.local.lentacashback;

public final class ProductMatcherTest {
    public static void main(String[] args) {
        yes("Уход за волосами", "Шампунь для волос Pantene Pro-V Aqua Light 400мл");
        yes("Уход за волосами", "Маска для волос Garnier Fructis 3в1 Superfood Папайя 390мл");
        yes("Лоск для\u00a0стирки", "Гель для стирки Лоск Color");
        yes("Ariel для стирки", "Капсулы ARIEL для стирки");
        yes("Персил для стирки", "Средство для стирки Persil Color");
        yes("Сыры Belster и\u00a0Santa Rosa", "Сыр BELSTER сливочный");
        yes("Сгущенное молоко и сливки", "Молоко сгущённое с сахаром");
        yes("Йогурты", "Йогурт питьевой клубничный");
        yes("Ряженка", "Ряженка 4%");
        yes("Сладкая газированная вода", "Напиток газированный Добрый Кола");
        yes("Соки, морсы, холодный чай", "Чай холодный персик");
        yes("Тихие красные вина", "Вино Авторское Каберне-Саперави красное сухое 13%");
        yes("Мюсли", "Мюсли с фруктами");
        yes("Мороженое и замороженные десерты", "Пломбир ванильный");
        yes("Almette - натуральный творожный сыр", "Сыр творожный Almette");
        yes("Корм для кошек WHISKAS®", "Корм Whiskas для кошек");
        no("Корм для кошек WHISKAS®", "Корм Felix для кошек");
        no("Тихие красные вина", "Вино красное игристое");
        no("Сгущенное молоко и сливки", "Молоко пастеризованное");
        no("Сладкая газированная вода", "Вода минеральная газированная");
        no("Уход за волосами", "Кондиционер для белья Lenor");
        no("Кешбэк с подпиской", "Молоко");
        no("Сухой корм Felix", "Корм влажный Felix");
        no("Неизвестное предложение", "Шампунь");
        System.out.println("PASS: store category names, Unicode spaces, brands, product types and exclusions.");
    }
    private static void yes(String c, String p) { if (!ProductMatcher.matches(c, p)) throw new AssertionError(c + ": " + p); }
    private static void no(String c, String p) { if (ProductMatcher.matches(c, p)) throw new AssertionError("False match " + c + ": " + p); }
}
