package ru.local.lentacashback;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Locale;

final class PurchaseTotals {
    static final class Item {
        final String name;
        final long cents;

        Item(String name, double paid) {
            this.name = name == null ? "" : name;
            cents = Double.isNaN(paid) || Double.isInfinite(paid) ? 0
                    : BigDecimal.valueOf(paid).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        }
    }

    static final class Receipt {
        final String id;
        final String date;
        final List<Item> items;

        Receipt(String id, String date, List<Item> items) {
            this.id = id == null ? "" : id;
            this.date = date;
            this.items = items;
        }
    }

    static final class CategoryTotal {
        final String name;
        long cents;
        int items;
        final Map<String, ProductTotal> products = new LinkedHashMap<>();

        CategoryTotal(String name) { this.name = name; }
    }

    static final class ProductTotal {
        final String name;
        long cents;
        int items;

        ProductTotal(String name) { this.name = name; }
    }

    static final class Report {
        final List<CategoryTotal> categories = new ArrayList<>();
        long totalCents;
        long unmatchedCents;
        int receiptCount;
        int itemCount;
        int unmatchedItems;
        int overlappingItems;
        final Map<String, ProductTotal> unmatchedProducts = new LinkedHashMap<>();
        int undatedReceipts;
        LocalDate earliest;
        LocalDate latest;
    }

    static Report calculate(List<Receipt> receipts, List<String> names, LocalDate start, LocalDate end) {
        if (start.isAfter(end)) throw new IllegalArgumentException("Start must precede end");
        Report report = new Report();
        Map<String, CategoryTotal> categories = new LinkedHashMap<>();
        for (String name : names) {
            if (name != null && !name.trim().isEmpty() && !categories.containsKey(name.trim())) {
                categories.put(name.trim(), new CategoryTotal(name.trim()));
            }
        }
        Set<String> seen = new HashSet<>();
        for (Receipt receipt : receipts) {
            if (!receipt.id.isEmpty() && !seen.add(receipt.id)) continue;
            LocalDate date;
            try { date = LocalDate.parse(receipt.date); }
            catch (Exception invalidDate) { report.undatedReceipts++; continue; }
            if (date.isAfter(LocalDate.now())) continue;
            if (report.earliest == null || date.isBefore(report.earliest)) report.earliest = date;
            if (report.latest == null || date.isAfter(report.latest)) report.latest = date;
            if (date.isBefore(start) || date.isAfter(end)) continue;
            report.receiptCount++;
            for (Item item : receipt.items) {
                report.totalCents += item.cents;
                report.itemCount++;
                int matches = 0;
                for (CategoryTotal category : categories.values()) {
                    if (ProductMatcher.matches(category.name, item.name)) {
                        category.cents += item.cents;
                        category.items++;
                        addProduct(category.products, item);
                        matches++;
                    }
                }
                if (matches == 0) {
                    report.unmatchedCents += item.cents;
                    report.unmatchedItems++;
                    addProduct(report.unmatchedProducts, item);
                } else if (matches > 1) report.overlappingItems++;
            }
        }
        report.categories.addAll(categories.values());
        Collections.sort(report.categories, (a, b) -> {
            int byAmount = Long.compare(b.cents, a.cents);
            return byAmount != 0 ? byAmount : a.name.compareTo(b.name);
        });
        return report;
    }

    private static void addProduct(Map<String, ProductTotal> products, Item item) {
        String name = item.name.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) name = "Товар без названия";
        String key = name.toLowerCase(Locale.ROOT);
        ProductTotal product = products.get(key);
        if (product == null) { product = new ProductTotal(name); products.put(key, product); }
        product.cents += item.cents;
        product.items++;
    }

    static List<ProductTotal> sortedProducts(Map<String, ProductTotal> products) {
        List<ProductTotal> result = new ArrayList<>(products.values());
        Collections.sort(result, (a, b) -> {
            int byAmount = Long.compare(b.cents, a.cents);
            return byAmount != 0 ? byAmount : a.name.compareTo(b.name);
        });
        return result;
    }
}
