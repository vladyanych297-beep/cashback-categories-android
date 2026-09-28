package ru.local.lentacashback;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;

public final class PurchaseTotalsTest {
    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 1, 1), end = LocalDate.of(2026, 1, 31);
        PurchaseTotals.Receipt first = receipt("one", "2026-01-01", item("Молоко 3,2%", 100.10),
                item("Сыр твердый", 200.20), item("Неизвестный товар", 50.30));
        PurchaseTotals.Receipt last = receipt("two", "2026-01-31", item("Молоко 2,5%", 0.20));
        PurchaseTotals.Report report = PurchaseTotals.calculate(Arrays.asList(first, first, last,
                receipt("old", "2025-12-31", item("Молоко", 999)),
                receipt("new", "2026-02-01", item("Молоко", 999)),
                receipt("invalid", "2026-02-30", item("Молоко", 999))),
                Arrays.asList("Молоко", "Сыры", "Сыры твердые", "Чай", "Молоко"), start, end);
        check(report.receiptCount == 2, "Inclusive dates or receipt deduplication failed");
        check(report.itemCount == 4, "Wrong item count");
        check(report.totalCents == 35080, "Wrong unique total or decimal arithmetic");
        check(category(report, "Молоко").cents == 10030, "Wrong milk total");
        check(category(report, "Сыры").cents == 20020, "Wrong cheese total");
        check(category(report, "Сыры твердые").cents == 20020, "Overlap lost");
        check(category(report, "Чай").cents == 0, "Zero-spend category missing");
        check(report.categories.size() == 4, "Duplicate category counted");
        check(report.categories.get(0).cents == 20020, "Categories not sorted by spend");
        check(report.unmatchedCents == 5030 && report.unmatchedItems == 1, "Unknown goods lost");
        check(report.overlappingItems == 1, "Overlap warning missing");
        check(category(report, "Молоко").products.size() == 2, "Products are missing inside categories");
        check(PurchaseTotals.sortedProducts(category(report, "Молоко").products).get(0).cents == 10010, "Product amount or sort order failed");
        check(report.unmatchedProducts.size() == 1, "Unknown products missing");
        check(report.undatedReceipts == 1, "Invalid date counted");

        PurchaseTotals.Report subset = PurchaseTotals.calculate(Arrays.asList(first, last),
                Arrays.asList("Молоко"), end, end);
        check(subset.totalCents == 20 && subset.receiptCount == 1, "Custom one-day period failed");
        PurchaseTotals.Report noCategories = PurchaseTotals.calculate(Arrays.asList(first), Collections.emptyList(), start, end);
        check(noCategories.unmatchedCents == noCategories.totalCents, "Purchases disappear without categories");
        PurchaseTotals.Report rounding = PurchaseTotals.calculate(Arrays.asList(receipt("cents", "2026-01-10",
                item("Молоко", 0.1), item("Молоко", 0.2), item("Молоко", 10.005),
                item("Молоко", -1.25), item("Молоко", Double.NaN), item("Молоко", Double.POSITIVE_INFINITY))),
                Arrays.asList("Молоко"), start, end);
        check(rounding.totalCents == 906, "Rounding, returns, or non-finite amounts failed");
        check(category(rounding, "Молоко").products.size() == 1, "Repeated products are not aggregated");
        check(category(rounding, "Молоко").products.get("молоко").cents == 906, "Aggregated product amount failed");
        PurchaseTotals.Report empty = PurchaseTotals.calculate(Collections.emptyList(), Arrays.asList("Молоко"), start, end);
        check(empty.totalCents == 0 && empty.receiptCount == 0, "Empty history failed");
        System.out.println("PASS: category amounts, date boundaries, custom periods, duplicates, overlaps, unknown goods, and kopeck precision.");
    }

    private static PurchaseTotals.Receipt receipt(String id, String date, PurchaseTotals.Item... items) {
        return new PurchaseTotals.Receipt(id, date, Arrays.asList(items));
    }
    private static PurchaseTotals.Item item(String name, double paid) { return new PurchaseTotals.Item(name, paid); }
    private static PurchaseTotals.CategoryTotal category(PurchaseTotals.Report report, String name) {
        for (PurchaseTotals.CategoryTotal category : report.categories) if (category.name.equals(name)) return category;
        throw new AssertionError("Category not found: " + name);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
