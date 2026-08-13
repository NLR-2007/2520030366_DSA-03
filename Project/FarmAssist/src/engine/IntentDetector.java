package engine;

import util.Trace;

/**
 * STAGE 3 OF THE PIPELINE  ->  keyword rules (no algorithm, just routing)
 *
 * Decides WHICH feature should answer the question, which in turn decides
 * which algorithm runs next. This is what makes FarmAssist feel like a chat
 * bot instead of a plain search box.
 */
public class IntentDetector {

    public enum Intent {
        FERTILIZER_BUDGET,      // -> Knapsack
        CROP_FERTILIZER_MATCH,  // -> Bipartite Matching
        CROP_COMPARE,           // -> side by side crop table
        CROP_PLANNER,           // -> rainfall / temperature / water filter
        DISEASE_DIAGNOSIS,      // -> KMP over symptom and damage lists
        CROP_INFO,              // -> direct record lookup
        FERTILIZER_INFO,        // -> direct record lookup
        PEST_INFO,              // -> direct pest record lookup
        GENERAL_SEARCH          // -> Rabin-Karp + KMP + QuickSort + Suffix Array
    }

    public Intent detect(String query, EntityExtractor.Entities e) {
        String q = query.toLowerCase();
        Intent intent = decide(q, e);
        Trace.log("Intent routing", "question classified as " + intent);
        return intent;
    }

    private Intent decide(String q, EntityExtractor.Entities e) {

        boolean hasMoney   = hasAny(q, "budget", "cost", "rupees", " rs", "under", "within",
                                       "price", "afford", "money", "cheap");
        boolean hasNumber  = q.matches(".*\\d{2,}.*");
        boolean saysFert   = hasAny(q, "fertilizer", "fertiliser", "manure", "nutrient")
                             || !e.fertilizers.isEmpty();

        // 1. budget question  ->  Knapsack
        if (hasMoney && hasNumber && (saysFert || !e.crops.isEmpty())) {
            return Intent.FERTILIZER_BUDGET;
        }

        // 2. assign fertilizers to several crops  ->  Bipartite Matching
        if (hasAny(q, "match", "assign", "allocate", "distribute", "each crop", "one each")
                && e.crops.size() >= 2) {
            return Intent.CROP_FERTILIZER_MATCH;
        }
        if (e.crops.size() >= 3 && saysFert && !hasMoney) {
            return Intent.CROP_FERTILIZER_MATCH;
        }

        // 2b. compare two crops side by side
        if (e.crops.size() >= 2 && hasAny(q, "compare", "comparison", "difference",
                                              "differ", "vs", "versus", "better", "between")) {
            return Intent.CROP_COMPARE;
        }

        // 2c. a pest named by its own name with no symptoms  ->  pest profile
        if (!e.pests.isEmpty() && e.symptoms.isEmpty() && e.diseases.isEmpty()) {
            return Intent.PEST_INFO;
        }

        // 3. the farmer describes a problem  ->  disease / pest diagnosis
        if (!e.symptoms.isEmpty() || !e.diseases.isEmpty()
                || hasAny(q, "disease", "infected", "attack", "dying", "problem", "pest",
                          "insect", "bug", "worm", "borer", "moth", "hopper", "fly",
                          "mite", "aphid", "weevil", "caterpillar", "maggot", "grub")) {
            return Intent.DISEASE_DIAGNOSIS;
        }

        // 3b. plan crops for the local rainfall and climate  ->  crop planner
        boolean wantsWaterPlan = hasAny(q, "low water", "less water", "little water",
                "high water", "lots of water", "much water", "plenty of water",
                "water need", "water requirement", "water supply");
        if (e.crops.isEmpty() && e.fertilizers.isEmpty()
                && (hasAny(q, "rainfall", " mm", "rain", "climate", "temperature", "hot",
                           "cold", "cool", "warm", "dry", "arid", "humid", "drought")
                    || wantsWaterPlan)
                && hasAny(q, "crop", "crops", "grow", "growing", "suit", "suitable",
                          "best", "which", "recommend", "plant", "cultivat")) {
            return Intent.CROP_PLANNER;
        }

        // 4. plain record lookups
        if (!e.fertilizers.isEmpty() && hasAny(q, "what is", "about", "npk", "detail", "info")) {
            return Intent.FERTILIZER_INFO;
        }
        if (!e.crops.isEmpty() && hasAny(q, "grow", "season", "soil", "water", "care",
                                            "cultivat", "about", "info", "detail", "sow",
                                            "type", "variet", "tell", "what is", "npk",
                                            "duration", "mature", "harvest", "temperature",
                                            "rainfall", "rain", "climate", "spacing",
                                            "distance", "yield", "production", "output")) {
            return Intent.CROP_INFO;
        }

        // 5. anything else  ->  full document search
        return Intent.GENERAL_SEARCH;
    }

    private static boolean hasAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    /** Pull the budget number out of a sentence like "under 3000 rupees". */
    public static int extractBudget(String query) {
        int best = 0;
        int i = 0;
        while (i < query.length()) {
            if (Character.isDigit(query.charAt(i))) {
                int j = i;
                while (j < query.length() && Character.isDigit(query.charAt(j))) j++;
                try { best = Math.max(best, Integer.parseInt(query.substring(i, j))); }
                catch (Exception ignored) { }
                i = j;
            } else i++;
        }
        return best;
    }
}
