package ru.local.lentacashback;

import java.util.Locale;
import java.util.regex.Pattern;

final class ProductMatcher {
    private ProductMatcher() {}

    private static boolean starts(String text, String expression) {
        String unicodeBoundary = "(?=$|[^\\p{L}\\p{N}_])";
        return Pattern.compile(expression.replace("\\b", unicodeBoundary), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(text).find();
    }

    // Deliberately conservative: ambiguous products stay unclassified rather than
    // increasing a cashback estimate for a category that may not include them.
    public static boolean matches(String category, String product) {
        String c = category.toLowerCase(Locale.ROOT);
        String p = product.trim();
        if (c.contains("violette")) return starts(p, "^Сыр творожный.*Violette");
        if (c.contains("mymuse")) return starts(p, "^(Шампунь|Бальзам|Маска|Кондиционер|Средство).*MYMUSE");
        if (c.contains("marco panatti")) return starts(p, "^Кетчуп.*MARCO PANATTI");
        if (c.contains("монбри") || c.contains("белебеевск")) return starts(p, "^Сыр .*?(Монбри|Белебеевск)");
        if (c.contains("belster") || c.contains("santa rosa")) return starts(p, "^Сыр .*?(BELSTER|SANTA ROSA)");
        if (c.contains("supreme") || c.contains("grand plaisir")) return starts(p, "^Сыр .*?(SUPREME|GRAND PLAISIR)");
        if (c.contains("творожные массы")) return starts(p, "^(Масса творожная|Творожная масса|Сырок творожный)");
        if (c.equals("молоко")) return starts(p, "^Молоко");
        if (c.contains("кисломолочные напитки")) return starts(p, "^(Кефир|Йогурт питьевой|Айран|Ряженка|Снежок|Тан )");
        if (c.contains("сыры твердые") || c.contains("сыры твёрдые")) return starts(p, "^Сыр ");
        if (c.contains("зефир") || c.contains("пастила")) return starts(p, "^(Зефир|Пастила)");
        if (c.contains("соки") || c.contains("нектары") || c.contains("морсы")) return starts(p, "^(Сок |Нектар|Морс)");
        if (c.equals("лимонады")) return starts(p, "^(Лимонад|Напиток газированный)");
        if (c.contains("крупы") || c.contains("бобовые")) return starts(p, "^(Крупа|Гречка|Рис |Горох|Фасоль|Чечевица)");
        if (c.contains("свежие яблоки")) return starts(p, "^Яблок");
        if (c.contains("влажный корм")) return starts(p, "^(Корм .*влажный|Влажный корм)");
        if (c.equals("хлебцы")) return starts(p, "^Хлебцы");
        if (c.contains("глазированные сырки")) return starts(p, "^(Сырок глазированный|Сырок творожный глазированный)");
        if (c.contains("чёрный чай") || c.contains("черный чай")) return starts(p, "^Чай .*черн");
        if (c.contains("цитрусовые")) return starts(p, "^(Апельсин|Мандарин|Лимон|Грейпфрут|Помело)");
        if (c.contains("свежие огурцы")) return starts(p, "^Огурц");
        if (c.contains("lenor")) return starts(p, "^(Кондиционер|Ополаскиватель).*LENOR");
        if (c.contains("персил")) return starts(p, "^(Гель|Порошок|Капсулы).*ПЕРСИЛ");
        if (c.contains("ariel")) return starts(p, "^(Гель|Порошок|Капсулы).*ARIEL");
        if (c.contains("сметана") && c.contains("творог")) return starts(p, "^(Сметана|Творог|Продукт творожный|Десерт творожный)\\b");
        if (c.contains("красота") || c.contains("гигиена")) return starts(p, "^(Шампунь|Гель-шампунь|Бальзам для губ|Крем-уход|Дезодорант|Зубная|Диски ватные|Палочки ватные|Прокладки|Жидкое .*мыло|Спрей солнцезащитный)\\b");
        if (c.equals("сыры")) return starts(p, "^Сыр ");
        if (c.contains("кефир") && c.contains("йогурт")) return starts(p, "^(Кефир|Йогурт|Айран|Ряженка)\\b");
        if (c.equals("сладости")) return starts(p, "^(Печенье|Карамель|Конфеты|Шоколад|Пастила|Зефир|Драже|Пирожное|Изделие мучное кондитерское|Мармелад|Жевательная резинка)\\b");
        if (c.equals("колбасы")) return starts(p, "^(Колбаса|Сосиски|Сардельки|Ветчина)\\b");
        if (c.equals("яйцо")) return starts(p, "^Яйцо\\b");
        if (c.contains("молоко") && c.contains("сливки")) return starts(p, "^(Молоко пастеризованное|Молоко ультрапастеризованное|Сливки)\\b");
        if (c.contains("масло сливочное")) return starts(p, "^Масло сливочное\\b");
        if (c.contains("бытовая химия")) return starts(p, "^(Стиральный порошок|Средство для стирки|Гель для стирки|Средство чистящее|Пена для очищения обуви)\\b");
        if (c.contains("снэки")) return starts(p, "^(Батончик|Чипсы|Сухарики|Семечки|Орехи|Сырные шарики|Семена чиа|Тараллини|Ядра подсолнечника|Хлебцы)\\b");
        if (c.contains("хлеб") && c.contains("выпечка")) return starts(p, "^(Хлеб |Булочка|Булка|Круассан|Слойка|Лаваш|Батон |Лепешка|Гата)\\b");
        if (c.contains("вода") && c.contains("напитки")) return starts(p, "^(Вода питьевая|Сок |Напиток [^к]|Лимонад)\\b");
        if (c.equals("крупы")) return starts(p, "^(Каша |Хлопья|Крупа|Гречка|Рис )\\b");
        if (c.equals("макароны")) return starts(p, "^(Макароны|Лапша)\\b");
        if (c.equals("рыба и морепродукты")) return starts(p, "^(Креветки|Сельдь|Рыба|Форель|Лосось)\\b");
        if (c.equals("птица")) return starts(p, "^(Курица|Индейка|Филе куриное)\\b");
        if (c.equals("мясо")) return starts(p, "^(Говядина|Свинина|Мясо|Фарш)\\b");
        if (c.equals("соусы")) return starts(p, "^(Майонез|Кетчуп|Соус)\\b");
        if (c.equals("консервы")) return starts(p, "^(Тунец .*в собственном соку|Кукуруза .*консервированная|Горошек .*консервированный)\\b");
        if (c.equals("вино")) return starts(p, "^Вино ");
        if (c.equals("кофе, какао")) return starts(p, "^(Кофе|Какао|Напиток кофейный)\\b");
        if (c.equals("чай")) return starts(p, "^Чай ");
        return false;
    }

}
