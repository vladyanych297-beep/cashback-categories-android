package ru.local.lentacashback;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class YandexPayEngine {
    static final class Operation {
        final String id, date, merchant, category;
        final long cents;
        final boolean cardPay;
        Operation(String id, String date, String merchant, String category, long cents, boolean cardPay) {
            this.id = id; this.date = date; this.merchant = merchant; this.category = category;
            this.cents = cents; this.cardPay = cardPay;
        }
    }
    static final class Offer {
        final String name, description, kind;
        final double rate;
        final boolean cardPay, selected;
        Offer(String name, double rate, String kind, boolean cardPay, String description, boolean selected) {
            this.name = name; this.rate = rate; this.kind = kind; this.cardPay = cardPay;
            this.description = description; this.selected = selected;
        }
    }
    static final class Recommendation {
        final Offer offer;
        long spend;
        int count;
        Recommendation(Offer offer) { this.offer = offer; }
        long estimate() { return "discount".equals(offer.kind) ? 0 : Math.round(spend * offer.rate / 100.0); }
    }
    static final class Group {
        final String name;
        long cents;
        int count;
        final Map<String, Group> merchants = new LinkedHashMap<>();
        Group(String name) { this.name = name; }
    }
    static final class Report {
        long total;
        int count;
        final List<Group> groups = new ArrayList<>();
        final List<Recommendation> recommendations = new ArrayList<>();
    }

    static String norm(String text) { return text.toLowerCase(Locale.ROOT).replace('ё', 'е').replace('\u00a0', ' ').replaceAll("\\s+", " ").trim(); }
    static boolean purchase(Operation operation) {
        return operation.cents > 0 && !norm(operation.category).matches(".*(перевод|пополн|снятие|погашение|комисси|проценты|начисление|возврат|списание баллов).*" );
    }
    static boolean matches(Offer offer, Operation operation) {
        if (offer.cardPay && !operation.cardPay) return false;
        String name = norm(offer.name), category = norm(operation.category), merchant = norm(operation.merchant);
        if (name.equals("все покупки")) return true;
        if (name.equals("яндекс такси")) return merchant.contains("яндекс такси") || merchant.contains("yandex taxi") || merchant.contains("yandex go");
        if (name.equals("яндекс лавка")) return merchant.contains("яндекс лавка") || merchant.contains("yandex lavka");
        if (name.equals("яндекс заправки")) return merchant.contains("яндекс заправки") || merchant.contains("yandex fuel");
        if (name.equals("еда и деливери")) return merchant.contains("яндекс еда") || merchant.contains("деливери") || merchant.contains("yandex eats");
        if (name.equals("кинопоиск")) return merchant.contains("кинопоиск") && (category.contains("кинотеатр") || merchant.contains("билет"));
        if (name.equals("осаго") || name.equals("каско")) return merchant.contains(name) && merchant.contains("яндекс забот");
        if (name.equals(category)) return true;
        if (name.equals("электроника")) return category.equals("электроника и бытовая техника");
        if (name.equals("книги")) return category.equals("книжные магазины");
        if (name.equals("красота")) return category.equals("парикмахерские и салоны красоты") || category.equals("косметика и парфюмерия");
        return false;
    }
    static Report calculate(List<Operation> operations, List<Offer> offers, LocalDate today) {
        Report report = new Report();
        LocalDate start = today.minusDays(99);
        Map<String, Group> groups = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        Set<String> offerNames = new HashSet<>();
        for (Offer offer : offers) {
            if (!offer.name.trim().isEmpty() && offerNames.add(norm(offer.name))) report.recommendations.add(new Recommendation(offer));
        }
        for (Operation operation : operations) {
            if (!purchase(operation)) continue;
            try {
                LocalDate date = LocalDate.parse(operation.date);
                if (date.isBefore(start) || date.isAfter(today)) continue;
            } catch (Exception invalidDate) { continue; }
            if (operation.id.isEmpty() || !seen.add(operation.id)) continue;
            report.total += operation.cents;
            report.count++;
            String category = operation.category.trim().isEmpty() ? "Без категории" : operation.category;
            String key = norm(category);
            Group group = groups.get(key);
            if (group == null) { group = new Group(category); groups.put(key, group); }
            group.cents += operation.cents; group.count++;
            String merchantKey = norm(operation.merchant);
            Group merchant = group.merchants.get(merchantKey);
            if (merchant == null) { merchant = new Group(operation.merchant); group.merchants.put(merchantKey, merchant); }
            merchant.cents += operation.cents; merchant.count++;
            for (Recommendation recommendation : report.recommendations) {
                if (matches(recommendation.offer, operation)) { recommendation.spend += operation.cents; recommendation.count++; }
            }
        }
        report.groups.addAll(groups.values());
        Collections.sort(report.groups, (a, b) -> Long.compare(b.cents, a.cents));
        Collections.sort(report.recommendations, (a, b) -> {
            int byEstimate = Long.compare(b.estimate(), a.estimate());
            return byEstimate != 0 ? byEstimate : Long.compare(b.spend, a.spend);
        });
        return report;
    }
}
