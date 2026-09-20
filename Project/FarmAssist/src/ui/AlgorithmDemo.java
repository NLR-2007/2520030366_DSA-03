package ui;

import algo.AhoCorasick;
import algo.BipartiteMatching;
import algo.BitmaskAssignment;
import algo.EditDistance;
import algo.KMP;
import algo.Knapsack;
import algo.KnapsackGreedy;
import algo.MaxFlow;
import algo.ParallelPrimitives;
import algo.RabinKarp;
import algo.RandomizedQuickSort;
import algo.SetCoverGreedy;
import algo.SuffixArrayLCP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Type "algo demo" in the chat to run this.
 * Each of the thirteen algorithms is executed on a tiny example so the output can
 * be checked by hand during the viva. Drawn on the same grid as the rest of
 * the app, so it stays readable inside the centred page.
 */
public class AlgorithmDemo {

    private static final String PAD = "  ";
    private static final int LABEL_W = 14;

    public static void runAll() {
        System.out.println();
        System.out.println(Theme.panelTop(Theme.I_SPARK, "algorithm demonstration"));
        System.out.println(Theme.panelRow(Theme.stone("all thirteen algorithms, on inputs small enough to check by hand")));
        System.out.println(Theme.panelBottom());

        demoKmp();
        demoRabinKarp();
        demoAhoCorasick();
        demoEditDistance();
        demoSuffixArray();
        demoKnapsack();
        demoBipartite();
        demoQuickSort();
        demoBitmask();
        demoMaxFlow();
        demoSetCover();
        demoGreedyKnapsack();
        demoParallel();

        System.out.println();
        System.out.println(Theme.rule());
    }

    // 1
    private static void demoKmp() {
        section(1, "KMP", "exact search");
        row("text",     "the blast disease attacks rice and blast spreads fast");
        row("pattern",  "blast");
        String text = "the blast disease attacks rice and blast spreads fast";
        String pat = "blast";
        row("LPS",      Arrays.toString(KMP.buildLPS(pat)));
        row("first at", String.valueOf(KMP.search(text, pat)));
        row("all at",   String.valueOf(KMP.searchAll(text, pat)));
    }

    // 2
    private static void demoRabinKarp() {
        section(2, "Rabin-Karp", "rolling hash search");
        String text = "urea and dap and urea again with urea";
        String pat = "urea";
        row("text",     text);
        row("pattern",  pat);
        row("found at", String.valueOf(RabinKarp.search(text, pat)));
        row("count",    String.valueOf(RabinKarp.count(text, pat)));
    }

    // 3
    private static void demoAhoCorasick() {
        section(3, "Aho-Corasick", "many patterns in one pass");
        AhoCorasick ac = new AhoCorasick();
        ac.addPattern("tomato", "CROP");
        ac.addPattern("rice", "CROP");
        ac.addPattern("yellow", "SYMPTOM");
        ac.addPattern("spot", "SYMPTOM");
        ac.addPattern("urea", "FERTILIZER");
        ac.build();

        String q = "my tomato has yellow leaves and brown spots, should i use urea";
        row("query",    q);
        row("patterns", "tomato, rice, yellow, spot, urea");

        StringBuilder matches = new StringBuilder();
        for (AhoCorasick.Match m : ac.search(q)) matches.append(m).append(' ');
        row("matches",  matches.toString().trim());
        row("note",     "\"rice\" is NOT reported inside \"price\" style words");
    }

    // 4
    private static void demoEditDistance() {
        section(4, "Edit Distance", "spelling correction");
        List<String> dict = Arrays.asList("tomato", "potato", "wheat", "fertilizer", "blight");
        String[] typed = {"tomatoe", "potatoe", "wheet", "fertilzer", "blght"};
        row("dictionary", String.join(", ", dict));
        for (String t : typed) {
            String best = EditDistance.bestMatch(t, dict, 2);
            row(t, (best == null ? Theme.alert("no match") : Theme.leaf(best))
                    + Theme.stone("  " + Theme.I_DOT + "  distance "
                                + (best == null ? "-" : EditDistance.distance(t, best))));
        }
    }

    // 5
    private static void demoSuffixArray() {
        section(5, "Suffix Array + LCP", "related text");
        String s = "banana";
        int[] sa = SuffixArrayLCP.buildSuffixArray(s);
        int[] lcp = SuffixArrayLCP.buildLCP(s, sa);
        StringBuilder suffixes = new StringBuilder();
        for (int i : sa) suffixes.append(s.substring(i)).append(' ');

        row("text",         s);
        row("suffixes",     suffixes.toString().trim());
        row("suffix array", Arrays.toString(sa));
        row("lcp array",    Arrays.toString(lcp));

        String a = "late blight spreads in humid weather on potato";
        String b = "in humid weather late blight destroys the tomato crop";
        row("article A",    a);
        row("article B",    b);
        row("shared",       "\"" + SuffixArrayLCP.longestCommonSubstring(a, b) + "\"");
    }

    // 6
    private static void demoKnapsack() {
        section(6, "0/1 Knapsack", "fertilizer under a budget");
        String[] names = {"urea", "dap", "potash", "vermicompost"};
        int[] cost     = {300, 1350, 850, 400};
        int[] value    = {70, 85, 75, 65};
        int budget = 1600;

        row("budget", "Rs " + budget);
        for (int i = 0; i < names.length; i++) {
            row(names[i], "Rs " + cost[i] + Theme.stone("  " + Theme.I_DOT + "  benefit ") + value[i]);
        }
        Knapsack.Result r = Knapsack.solve(cost, value, budget);
        StringBuilder chosen = new StringBuilder();
        for (int i : r.chosen) chosen.append(names[i]).append(' ');
        row("chosen", Theme.leaf(chosen.toString().trim()));
        row("result", "spent Rs " + r.totalCost
                + Theme.stone("  " + Theme.I_DOT + "  total benefit ") + r.totalValue);
    }

    // 7
    private static void demoBipartite() {
        section(7, "Bipartite Matching", "one fertilizer per crop");
        String[] crops = {"rice", "banana", "cotton"};
        String[] ferts = {"urea", "potash", "dap"};
        boolean[][] adj = {
            {true,  false, true },   // rice   likes urea, dap
            {true,  true,  false},   // banana likes urea, potash
            {false, false, true }    // cotton likes dap
        };
        row("rice likes",   "urea, dap");
        row("banana likes", "urea, potash");
        row("cotton likes", "dap");

        int[] match = BipartiteMatching.maxMatching(adj, 3, 3);
        for (int i = 0; i < crops.length; i++) {
            row(crops[i], Theme.I_ARROW + " " + (match[i] >= 0 ? Theme.leaf(ferts[match[i]])
                                                               : Theme.alert("nothing")));
        }
        row("matched", BipartiteMatching.countMatched(match) + " of 3 crops");
    }

    // 8
    private static void demoQuickSort() {
        section(8, "Randomized QuickSort", "ranking");
        List<Integer> scores = new ArrayList<>(Arrays.asList(12, 47, 3, 88, 25, 61, 7, 99, 34));
        row("before", scores.toString());
        RandomizedQuickSort.sort(scores, Comparator.comparingInt(x -> -x));
        row("after",  scores + Theme.stone("  " + Theme.I_DOT + "  highest relevance first"));
        row("note",   "Las Vegas: always right, running time is what is random");
        row("",       "(Rabin-Karp without its final check would be Monte Carlo)");
    }

    // 9
    private static void demoBitmask() {
        section(9, "Bitmask DP", "best assignment over subsets");
        String[] crops = {"rice", "banana", "cotton"};
        String[] ferts = {"urea", "potash", "dap"};
        // benefit[fertilizer][crop], -1 = does not suit
        int[][] benefit = {
            {70, 60, -1},   // urea   : rice 70, banana 60
            {-1, 75, 50},   // potash : banana 75, cotton 50
            {85, -1, 80}    // dap    : rice 85, cotton 80
        };
        row("rice",    "urea 70, dap 85");
        row("banana",  "urea 60, potash 75");
        row("cotton",  "potash 50, dap 80");
        row("states",  "dp[4][2^3] = 32");
        row("options", "two ways serve all three: urea/potash/dap = 225, dap/urea/potash = 195");

        BitmaskAssignment.Result r = BitmaskAssignment.solve(benefit, 3, 3);
        for (int i = 0; i < crops.length; i++) {
            row(crops[i], Theme.I_ARROW + " " + (r.assignment[i] >= 0
                    ? Theme.leaf(ferts[r.assignment[i]]) : Theme.alert("nothing")));
        }
        row("benefit", Theme.leaf(String.valueOf(r.totalBenefit)) + Theme.stone("  " + Theme.I_DOT
                + "  Kuhn stops at any maximum matching; the DP picks the richest"));
    }

    // 10
    private static void demoMaxFlow() {
        section(10, "Max Flow + Min Cut", "Edmonds-Karp");
        // source 0, rice 1, cotton 2, urea 3, dap 4, sink 5
        MaxFlow net = new MaxFlow(6);
        int eRice = net.addEdge(0, 1, 3);      // 3 fields of rice
        int eCotton = net.addEdge(0, 2, 2);    // 2 fields of cotton
        net.addEdge(1, 3, 99);                 // rice takes urea
        int eRiceDap = net.addEdge(1, 4, 99);  // rice takes dap
        net.addEdge(2, 4, 99);                 // cotton takes dap only
        net.addEdge(3, 5, 2);                  // 2 bags urea in stock
        net.addEdge(4, 5, 2);                  // 2 bags dap in stock
        row("fields",  "rice 3, cotton 2");
        row("stock",   "urea 2, dap 2");
        row("suits",   "rice: urea, dap   cotton: dap");

        int flow = net.maxFlow(0, 5);
        row("max flow", Theme.leaf(flow + " fields supplied") + Theme.stone("  " + Theme.I_DOT
                + "  " + net.augmentingPaths + " augmenting paths"));
        row("rice",    net.flowOn(eRice) + " of 3" + Theme.stone("  " + Theme.I_DOT
                + "  " + net.flowOn(eRiceDap) + " bag(s) of dap"));
        row("cotton",  net.flowOn(eCotton) + " of 2");
        StringBuilder side = new StringBuilder();
        String[] names = {"source", "rice", "cotton", "urea", "dap", "sink"};
        for (int v = 0; v < 6; v++) if (net.onSourceSide(v)) side.append(names[v]).append(' ');
        row("min cut", "{ " + side.toString().trim() + " }" + Theme.stone("  " + Theme.I_DOT
                + "  capacity 4 = flow: only 4 bags exist, so 4 is the ceiling"));
    }

    // 11
    private static void demoSetCover() {
        section(11, "Greedy Set Cover", "NP-hard, ln(n) approximation");
        String[] crops = {"rice", "wheat", "cotton", "banana", "maize"};
        String[] ferts = {"urea", "dap", "potash", "npk"};
        boolean[][] covers = {
            {true,  true,  false, true,  true },   // urea   : rice wheat banana maize
            {true,  true,  true,  false, false},   // dap    : rice wheat cotton
            {false, false, false, true,  false},   // potash : banana
            {false, false, true,  false, true }    // npk    : cotton maize
        };
        row("crops",   String.join(", ", crops));
        row("urea",    "rice, wheat, banana, maize");
        row("dap",     "rice, wheat, cotton");
        row("potash",  "banana");
        row("npk",     "cotton, maize");

        SetCoverGreedy.Result r = SetCoverGreedy.solve(covers, 4, 5);
        for (int k = 0; k < r.chosen.size(); k++) {
            row("pick " + (k + 1), Theme.leaf(ferts[r.chosen.get(k)]) + Theme.stone("  "
                    + Theme.I_DOT + "  covers " + r.gained.get(k) + " new crop(s)"));
        }
        row("bound",   String.format("H(5) = %.2f", r.bound) + Theme.stone("  " + Theme.I_DOT
                + "  greedy uses at most that many times the optimum"));
    }

    // 12
    private static void demoGreedyKnapsack() {
        section(12, "Greedy Knapsack", "1/2-approximation vs exact DP");
        String[] names = {"urea", "dap", "potash", "vermicompost"};
        int[] cost     = {300, 1350, 850, 400};
        int[] value    = {70, 85, 75, 65};
        int budget = 1600;
        row("budget",  "Rs " + budget + Theme.stone("  " + Theme.I_DOT + "  same items as section 6"));

        Knapsack.Result exact = Knapsack.solve(cost, value, budget);
        KnapsackGreedy.Result greedy = KnapsackGreedy.solve(cost, value, budget);
        StringBuilder g = new StringBuilder();
        for (int i : greedy.chosen) g.append(names[i]).append(' ');
        row("greedy",  Theme.leaf(g.toString().trim()) + Theme.stone("  " + Theme.I_DOT
                + "  benefit " + greedy.totalValue + ", Rs " + greedy.totalCost));
        row("exact DP", "benefit " + exact.totalValue + ", Rs " + exact.totalCost);
        row("ratio",   String.format("%.2f", greedy.totalValue / (double) exact.totalValue)
                + Theme.stone("  " + Theme.I_DOT + "  proven >= 0.50"));
        row("why",     "knapsack is NP-hard (subset sum reduces to it); the DP is");
        row("",        "pseudo-polynomial in the budget, greedy is O(n log n)");
    }

    // 13
    private static void demoParallel() {
        section(13, "Parallel Primitives", "map, reduce, prefix sum");
        long[] costs = {300, 1350, 850, 400, 250, 600, 900, 150, 500, 700, 200, 1100};
        row("costs",   Arrays.toString(costs));
        long[] prefix = ParallelPrimitives.prefixSum(costs);
        row("prefix",  Arrays.toString(prefix) + Theme.stone("  " + Theme.I_DOT + "  exclusive scan"));
        row("tree",    "work " + ParallelPrimitives.lastWork + ", span " + ParallelPrimitives.lastSpan
                + ", " + ParallelPrimitives.lastTasks + " leaf tasks");
        row("sum",     String.valueOf(ParallelPrimitives.sum(costs)) + Theme.stone("  "
                + Theme.I_DOT + "  parallel reduce"));
        int[] scores = {3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8};
        row("max",     ParallelPrimitives.max(scores) + " of " + Arrays.toString(scores));
        row("cores",   ParallelPrimitives.threads() + " in the fork/join pool" + Theme.stone("  "
                + Theme.I_DOT + "  Brent: time <= work/p + span"));
    }

    // ------------------------------------------------------------- drawing

    private static void section(int number, String name, String what) {
        System.out.println();
        System.out.println(Theme.section(number + ". " + name + "  " + Theme.I_DOT + "  " + what));
        System.out.println();
    }

    /** One "label   value" line, wrapped inside the page. */
    private static void row(String label, String value) {
        int indentWidth = PAD.length() + LABEL_W;
        String indent = " ".repeat(indentWidth);
        System.out.println(PAD + Theme.stone(Theme.padRight(label, LABEL_W))
                + Theme.chalk(Theme.wrap(value, Theme.WIDTH - indentWidth - 1, indent)));
    }
}
