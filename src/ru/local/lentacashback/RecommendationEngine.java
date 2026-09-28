package ru.local.lentacashback;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.time.LocalDate;

public final class RecommendationEngine {
    private RecommendationEngine() {}

    public static final class Recommendation {
        public final String name;
        public final double rate;
        public final double spend;
        public final int items;
        public final double score;

        Recommendation(String name, double rate, double spend, int items) {
            this.name = name;
            this.rate = rate;
            this.spend = spend;
            this.items = items;
            this.score = spend * rate / 100.0;
        }
    }

    public static boolean matches(String category, String product) {
        return ProductMatcher.matches(category, product);
    }

    public static List<Recommendation> rank(JSONArray receipts, JSONArray categories) {
        return rankSince(receipts, categories, LocalDate.now().minusDays(90));
    }

    public static List<Recommendation> rankMonths(JSONArray receipts, JSONArray categories, int months) {
        return rankSince(receipts, categories, LocalDate.now().minusMonths(months));
    }

    private static List<Recommendation> rankSince(JSONArray receipts, JSONArray categories, LocalDate cutoff) {
        return rankBetween(receipts, categories, cutoff, LocalDate.now());
    }

    public static List<Recommendation> rankBetween(JSONArray receipts, JSONArray categories, LocalDate cutoff, LocalDate end) {
        List<Recommendation> result = new ArrayList<>();
        for (int c = 0; c < categories.length(); c++) {
            JSONObject category = categories.optJSONObject(c);
            if (category == null) continue;
            String name = category.optString("name");
            double rate = category.optDouble("rate", 0);
            if (name.isEmpty() || rate <= 0) continue;
            double spend = 0;
            int count = 0;
            for (int r = 0; r < receipts.length(); r++) {
                JSONObject receipt = receipts.optJSONObject(r);
                if (receipt == null) continue;
                try {
                    LocalDate date = LocalDate.parse(receipt.optString("date"));
                    if (date.isBefore(cutoff) || date.isAfter(end)) continue;
                } catch (Exception invalidDate) { continue; }
                JSONArray items = receipt.optJSONArray("items");
                if (items == null) continue;
                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.optJSONObject(i);
                    if (item != null && matches(name, item.optString("name"))) {
                        spend += item.optDouble("paid", 0);
                        count++;
                    }
                }
            }
            result.add(new Recommendation(name, rate, spend, count));
        }
        Collections.sort(result, new Comparator<Recommendation>() {
            @Override public int compare(Recommendation a, Recommendation b) {
                return Double.compare(b.score, a.score);
            }
        });
        return result;
    }
}
