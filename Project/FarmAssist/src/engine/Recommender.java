package engine;

import algo.BipartiteMatching;
import algo.Knapsack;
import model.Fertilizer;
import util.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * THE TWO OPTIMISATION FEATURES
 *   ALGORITHM 6 : 0/1 KNAPSACK          -> best fertilizer basket within a budget
 *   ALGORITHM 7 : BIPARTITE MATCHING    -> one fertilizer per crop, no repeats
 */
public class Recommender {

    private final DataLoader data;

    public Recommender(DataLoader data) { this.data = data; }

    // ==================================================================
    // FEATURE A : buy the best fertilizers for a crop inside a budget
    // ==================================================================

    public static class BudgetPlan {
        public List<Fertilizer> chosen = new ArrayList<>();
        public int totalCost;
        public int totalBenefit;
        public int budget;
        public String crop;
        public List<Fertilizer> considered = new ArrayList<>();
    }

    public BudgetPlan planWithinBudget(String crop, int budget) {
        BudgetPlan plan = new BudgetPlan();
        plan.budget = budget;
        plan.crop = crop;

        // build the item list: fertilizers that suit this crop (or all of them)
        for (Fertilizer f : data.fertilizers) {
            if (crop == null || f.suitsCrop(crop)) plan.considered.add(f);
        }
        if (plan.considered.isEmpty()) plan.considered.addAll(data.fertilizers);

        int n = plan.considered.size();
        int[] cost = new int[n];
        int[] value = new int[n];
        for (int i = 0; i < n; i++) {
            Fertilizer f = plan.considered.get(i);
            cost[i] = f.cost;                                 // weight  = price
            value[i] = f.benefit + nutrientBonus(f);          // value   = usefulness
        }

        // ---------- ALGORITHM 6 : 0/1 KNAPSACK ----------
        Knapsack.Result r = Knapsack.solve(cost, value, budget);

        for (int idx : r.chosen) plan.chosen.add(plan.considered.get(idx));
        plan.totalCost = r.totalCost;
        plan.totalBenefit = r.totalValue;

        Trace.log("0/1 Knapsack", "DP table of " + n + " fertilizers x Rs " + budget
                + " budget -> picked " + plan.chosen.size()
                + " bags worth Rs " + plan.totalCost);
        return plan;
    }

    /** A fertilizer that carries more nutrients is slightly more valuable. */
    private int nutrientBonus(Fertilizer f) {
        return (f.n + f.p + f.k) / 10;
    }

    // ==================================================================
    // FEATURE B : give ONE different fertilizer to each of several crops
    // ==================================================================

    public static class MatchPlan {
        public List<String> crops = new ArrayList<>();
        public List<Fertilizer> pool = new ArrayList<>();
        public int[] assignment;         // assignment[i] = index in pool, or -1
        public int matchedCount;
    }

    public MatchPlan matchCropsToFertilizers(List<String> crops) {
        MatchPlan plan = new MatchPlan();
        plan.crops.addAll(crops);

        // right side of the graph = one bag of every fertilizer that suits someone
        for (Fertilizer f : data.fertilizers) {
            for (String c : crops) {
                if (f.suitsCrop(c)) { plan.pool.add(f); break; }
            }
        }

        int L = plan.crops.size(), R = plan.pool.size();
        if (L == 0 || R == 0) { plan.assignment = new int[L]; java.util.Arrays.fill(plan.assignment, -1); return plan; }

        // build the compatibility graph
        boolean[][] adj = new boolean[L][R];
        int edges = 0;
        for (int i = 0; i < L; i++) {
            for (int j = 0; j < R; j++) {
                adj[i][j] = plan.pool.get(j).suitsCrop(plan.crops.get(i));
                if (adj[i][j]) edges++;
            }
        }

        // ---------- ALGORITHM 7 : MAXIMUM BIPARTITE MATCHING ----------
        plan.assignment = BipartiteMatching.maxMatching(adj, L, R);
        plan.matchedCount = BipartiteMatching.countMatched(plan.assignment);

        Trace.log("Bipartite Matching", "graph with " + L + " crops, " + R
                + " fertilizer bags and " + edges + " compatible edges -> matched "
                + plan.matchedCount + " crops");
        return plan;
    }
}
