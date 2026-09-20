package engine;

import algo.BipartiteMatching;
import algo.BitmaskAssignment;
import algo.Knapsack;
import algo.KnapsackGreedy;
import algo.MaxFlow;
import algo.ParallelPrimitives;
import algo.SetCoverGreedy;
import model.Fertilizer;
import util.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * THE OPTIMISATION FEATURES
 *   ALGORITHM 6  : 0/1 KNAPSACK          -> best fertilizer basket within a budget
 *   ALGORITHM 12 : GREEDY KNAPSACK       -> the 1/2-approximation, shown beside it
 *   ALGORITHM 7  : BIPARTITE MATCHING    -> how many crops can get their own bag
 *   ALGORITHM 9  : BITMASK DP            -> the best-benefit way to hand those bags out
 *   ALGORITHM 10 : MAX FLOW / MIN CUT    -> many fields, limited stock
 *   ALGORITHM 11 : GREEDY SET COVER      -> fewest products that suit every crop
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
        // the same question answered by the greedy 1/2-approximation
        public List<Fertilizer> greedyChosen = new ArrayList<>();
        public int greedyBenefit;
        public int greedyCost;
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

        // ---------- ALGORITHM 12 : GREEDY KNAPSACK (1/2-approximation) ----------
        KnapsackGreedy.Result g = KnapsackGreedy.solve(cost, value, budget);
        for (int idx : g.chosen) plan.greedyChosen.add(plan.considered.get(idx));
        plan.greedyBenefit = g.totalValue;
        plan.greedyCost = g.totalCost;

        Trace.log("Greedy Knapsack", "value-per-rupee order -> benefit " + g.totalValue
                + " vs exact " + r.totalValue + " (guaranteed >= half of exact"
                + (g.usedSingleItem ? ", single best item won)" : ")"));
        Trace.log("Parallel prefix", "running cost over " + ParallelPrimitives.lastWork
                + " bags: work " + ParallelPrimitives.lastWork + ", span "
                + ParallelPrimitives.lastSpan + ", " + ParallelPrimitives.lastTasks
                + " leaf tasks on " + ParallelPrimitives.threads() + " cores");
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
        public int matchedCount;         // from Kuhn's matching
        public int totalBenefit;         // from the bitmask DP, when it ran
        public boolean optimised;        // false when there were too many crops for 2^n
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

        // ---------- ALGORITHM 9 : BITMASK DP over subsets of crops ----------
        // Kuhn's answer is A maximum matching; the DP picks the BEST one.
        if (L <= BitmaskAssignment.MAX_FIELDS) {
            int[][] benefit = new int[R][L];
            for (int j = 0; j < R; j++) {
                for (int i = 0; i < L; i++) benefit[j][i] = adj[i][j] ? plan.pool.get(j).benefit : -1;
            }
            BitmaskAssignment.Result b = BitmaskAssignment.solve(benefit, R, L);
            plan.assignment = b.assignment;
            plan.totalBenefit = b.totalBenefit;
            plan.optimised = true;

            Trace.log("Bitmask DP", "dp[" + (R + 1) + "][2^" + L + "] = " + b.statesVisited
                    + " states -> serves " + b.servedCount + " crops with total benefit "
                    + b.totalBenefit + (b.servedCount == plan.matchedCount
                    ? " (agrees with Kuhn on the count)" : ""));
        }
        return plan;
    }

    // ==================================================================
    // FEATURE C : many fields of each crop, limited bags on the shelf
    // ==================================================================

    public static class SupplyPlan {
        public List<String> crops = new ArrayList<>();
        public List<Integer> fields = new ArrayList<>();      // fields of each crop
        public List<Fertilizer> pool = new ArrayList<>();
        public int[][] bags;                                   // bags[crop][fertilizer]
        public int[] supplied;                                 // fields served per crop
        public int totalFields, totalSupplied;
        public List<String> shortCrops = new ArrayList<>();    // on the source side of the cut
        public List<String> soldOut = new ArrayList<>();       // shelves the cut goes through
    }

    /**
     * @param crops  the crops named
     * @param fields how many fields of each, same order
     */
    public SupplyPlan supplyFields(List<String> crops, List<Integer> fields) {
        SupplyPlan plan = new SupplyPlan();
        plan.crops.addAll(crops);
        plan.fields.addAll(fields);
        for (int f : fields) plan.totalFields += f;

        for (Fertilizer f : data.fertilizers) {
            for (String c : crops) if (f.suitsCrop(c)) { plan.pool.add(f); break; }
        }
        int L = crops.size(), R = plan.pool.size();
        plan.bags = new int[L][R];
        plan.supplied = new int[L];
        if (L == 0 || R == 0) return plan;

        // vertices: 0 = source, 1..L crops, L+1..L+R fertilizers, L+R+1 = sink
        int source = 0, sink = L + R + 1;
        MaxFlow net = new MaxFlow(sink + 1);
        int[] cropEdge = new int[L];
        int[][] pairEdge = new int[L][R];
        int edges = 0;
        for (int i = 0; i < L; i++) {
            cropEdge[i] = net.addEdge(source, 1 + i, fields.get(i));
            edges++;
        }
        for (int j = 0; j < R; j++) {
            for (int i = 0; i < L; i++) {
                pairEdge[i][j] = plan.pool.get(j).suitsCrop(crops.get(i))
                        ? net.addEdge(1 + i, 1 + L + j, Integer.MAX_VALUE) : -1;
                if (pairEdge[i][j] >= 0) edges++;
            }
            net.addEdge(1 + L + j, sink, plan.pool.get(j).stock);
            edges++;
        }

        // ---------- ALGORITHM 10 : MAX FLOW (Edmonds-Karp) ----------
        plan.totalSupplied = net.maxFlow(source, sink);

        for (int i = 0; i < L; i++) {
            plan.supplied[i] = net.flowOn(cropEdge[i]);
            for (int j = 0; j < R; j++) {
                if (pairEdge[i][j] >= 0) plan.bags[i][j] = net.flowOn(pairEdge[i][j]);
            }
        }
        // min cut: crops still reachable from the source are the ones left short,
        // and every shelf reachable from them is a shelf the cut passes through
        for (int i = 0; i < L; i++) {
            if (net.onSourceSide(1 + i) && plan.supplied[i] < fields.get(i)) plan.shortCrops.add(crops.get(i));
        }
        for (int j = 0; j < R; j++) {
            if (net.onSourceSide(1 + L + j)) plan.soldOut.add(plan.pool.get(j).name);
        }

        Trace.log("Max Flow", "network of " + (sink + 1) + " vertices and " + edges
                + " edges, " + net.augmentingPaths + " augmenting paths -> flow "
                + plan.totalSupplied + " of " + plan.totalFields + " fields");
        if (plan.totalSupplied < plan.totalFields) {
            Trace.log("Min Cut", "cut of capacity " + plan.totalSupplied + " separates short crops "
                    + plan.shortCrops + " from the sink through " + plan.soldOut.size()
                    + " sold-out shelves");
        }
        return plan;
    }

    // ==================================================================
    // FEATURE D : the fewest products that suit every crop on the farm
    // ==================================================================

    public static class CoverPlan {
        public List<String> crops = new ArrayList<>();
        public List<Fertilizer> chosen = new ArrayList<>();
        public List<List<String>> coversCrops = new ArrayList<>();  // per chosen product
        public List<String> uncovered = new ArrayList<>();
        public double bound;                                        // H(n)
        public int candidates;
    }

    public CoverPlan coverCrops(List<String> crops) {
        CoverPlan plan = new CoverPlan();
        plan.crops.addAll(crops);

        // candidate sets: every fertilizer that suits at least one of the crops,
        // ignoring "all" products, which would make the puzzle trivial
        List<Fertilizer> sets = new ArrayList<>();
        for (Fertilizer f : data.fertilizers) {
            if (f.suited.contains("all")) continue;
            for (String c : crops) if (f.suitsCrop(c)) { sets.add(f); break; }
        }
        plan.candidates = sets.size();
        int S = sets.size(), U = crops.size();
        boolean[][] covers = new boolean[S][U];
        for (int s = 0; s < S; s++) {
            for (int u = 0; u < U; u++) covers[s][u] = sets.get(s).suitsCrop(crops.get(u));
        }

        // ---------- ALGORITHM 11 : GREEDY SET COVER ----------
        SetCoverGreedy.Result r = SetCoverGreedy.solve(covers, S, U);
        boolean[] done = new boolean[U];
        for (int idx : r.chosen) {
            Fertilizer f = sets.get(idx);
            plan.chosen.add(f);
            List<String> newly = new ArrayList<>();
            for (int u = 0; u < U; u++) {
                if (!done[u] && covers[idx][u]) { done[u] = true; newly.add(crops.get(u)); }
            }
            plan.coversCrops.add(newly);
        }
        for (int u = 0; u < U; u++) if (!done[u]) plan.uncovered.add(crops.get(u));
        plan.bound = r.bound;

        Trace.log("Greedy Set Cover", S + " candidate sets over " + U + " crops -> "
                + r.chosen.size() + " products" + (r.complete ? "" : ", " + plan.uncovered.size()
                + " crop(s) uncovered") + "; NP-hard, greedy is within H(" + U + ") = "
                + String.format("%.2f", r.bound) + " of optimal");
        return plan;
    }
}
