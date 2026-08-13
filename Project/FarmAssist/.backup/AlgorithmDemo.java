package ui;

import algo.AhoCorasick;
import algo.BipartiteMatching;
import algo.EditDistance;
import algo.KMP;
import algo.Knapsack;
import algo.RabinKarp;
import algo.RandomizedQuickSort;
import algo.SuffixArrayLCP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Type "algo demo" in the chat to run this.
 * Each of the eight algorithms is executed on a tiny example so the output can
 * be checked by hand during the viva.
 */
public class AlgorithmDemo {

    public static void runAll() {
        head("ALGORITHM DEMONSTRATION - all 8 on small inputs");
        demoKmp();
        demoRabinKarp();
        demoAhoCorasick();
        demoEditDistance();
        demoSuffixArray();
        demoKnapsack();
        demoBipartite();
        demoQuickSort();
        System.out.println("\n" + bar());
    }

    // 1
    private static void demoKmp() {
        section("1. KMP - exact search");
        String text = "the blast disease attacks rice and blast spreads fast";
        String pat = "blast";
        System.out.println("   text    : " + text);
        System.out.println("   pattern : " + pat);
        System.out.println("   LPS     : " + Arrays.toString(KMP.buildLPS(pat)));
        System.out.println("   first at: " + KMP.search(text, pat));
        System.out.println("   all at  : " + KMP.searchAll(text, pat));
    }

    // 2
    private static void demoRabinKarp() {
        section("2. Rabin-Karp - rolling hash search");
        String text = "urea and dap and urea again with urea";
        String pat = "urea";
        System.out.println("   text    : " + text);
        System.out.println("   pattern : " + pat);
        System.out.println("   found at: " + RabinKarp.search(text, pat));
        System.out.println("   count   : " + RabinKarp.count(text, pat));
    }

    // 3
    private static void demoAhoCorasick() {
        section("3. Aho-Corasick - many patterns in one pass");
        AhoCorasick ac = new AhoCorasick();
        ac.addPattern("tomato", "CROP");
        ac.addPattern("rice", "CROP");
        ac.addPattern("yellow", "SYMPTOM");
        ac.addPattern("spot", "SYMPTOM");
        ac.addPattern("urea", "FERTILIZER");
        ac.build();

        String q = "my tomato has yellow leaves and brown spots, should i use urea";
        System.out.println("   query   : " + q);
        System.out.println("   patterns: tomato, rice, yellow, spot, urea");
        System.out.print("   matches : ");
        for (AhoCorasick.Match m : ac.search(q)) System.out.print(m + " ");
        System.out.println("\n   note    : \"rice\" is NOT reported inside \"price\" style words");
    }

    // 4
    private static void demoEditDistance() {
        section("4. Edit Distance - spelling correction");
        List<String> dict = Arrays.asList("tomato", "potato", "wheat", "fertilizer", "blight");
        String[] typed = {"tomatoe", "potatoe", "wheet", "fertilzer", "blght"};
        System.out.println("   dictionary: " + dict);
        for (String t : typed) {
            String best = EditDistance.bestMatch(t, dict, 2);
            System.out.println("   " + pad(t, 12) + " -> " + pad(String.valueOf(best), 12)
                    + " (distance " + (best == null ? "-" : EditDistance.distance(t, best)) + ")");
        }
    }

    // 5
    private static void demoSuffixArray() {
        section("5. Suffix Array + LCP - related text");
        String s = "banana";
        int[] sa = SuffixArrayLCP.buildSuffixArray(s);
        int[] lcp = SuffixArrayLCP.buildLCP(s, sa);
        System.out.println("   text        : " + s);
        System.out.print("   suffixes    : ");
        for (int i : sa) System.out.print(s.substring(i) + " ");
        System.out.println("\n   suffix array: " + Arrays.toString(sa));
        System.out.println("   lcp array   : " + Arrays.toString(lcp));

        String a = "late blight spreads in humid weather on potato";
        String b = "in humid weather late blight destroys the tomato crop";
        System.out.println("   article A   : " + a);
        System.out.println("   article B   : " + b);
        System.out.println("   longest shared phrase : \""
                + SuffixArrayLCP.longestCommonSubstring(a, b) + "\"");
    }

    // 6
    private static void demoKnapsack() {
        section("6. 0/1 Knapsack - fertilizer under a budget");
        String[] names = {"urea", "dap", "potash", "vermicompost"};
        int[] cost     = {300, 1350, 850, 400};
        int[] value    = {70, 85, 75, 65};
        int budget = 1600;

        System.out.println("   budget : Rs " + budget);
        for (int i = 0; i < names.length; i++) {
            System.out.println("   item   : " + pad(names[i], 14) + " cost Rs " + pad("" + cost[i], 6)
                    + " benefit " + value[i]);
        }
        Knapsack.Result r = Knapsack.solve(cost, value, budget);
        System.out.print("   chosen : ");
        for (int i : r.chosen) System.out.print(names[i] + " ");
        System.out.println("\n   spent  : Rs " + r.totalCost + "   total benefit " + r.totalValue);
    }

    // 7
    private static void demoBipartite() {
        section("7. Bipartite Matching - one fertilizer per crop");
        String[] crops = {"rice", "banana", "cotton"};
        String[] ferts = {"urea", "potash", "dap"};
        boolean[][] adj = {
            {true,  false, true },   // rice   likes urea, dap
            {true,  true,  false},   // banana likes urea, potash
            {false, false, true }    // cotton likes dap
        };
        System.out.println("   rice   -> urea, dap");
        System.out.println("   banana -> urea, potash");
        System.out.println("   cotton -> dap");

        int[] match = BipartiteMatching.maxMatching(adj, 3, 3);
        for (int i = 0; i < crops.length; i++) {
            System.out.println("   " + pad(crops[i], 8) + " gets "
                    + (match[i] >= 0 ? ferts[match[i]] : "nothing"));
        }
        System.out.println("   matched " + BipartiteMatching.countMatched(match) + " of 3 crops");
    }

    // 8
    private static void demoQuickSort() {
        section("8. Randomized QuickSort - ranking");
        List<Integer> scores = new ArrayList<>(Arrays.asList(12, 47, 3, 88, 25, 61, 7, 99, 34));
        System.out.println("   before : " + scores);
        RandomizedQuickSort.sort(scores, Comparator.comparingInt(x -> -x));
        System.out.println("   after  : " + scores + "   (highest relevance first)");
    }

    // helpers
    private static void head(String t) { System.out.println("\n" + bar() + "\n  " + t + "\n" + bar()); }
    private static void section(String t) { System.out.println("\n--- " + t + " " + dashes(50 - t.length())); }
    private static String bar() { return dashes(68); }
    private static String dashes(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.max(0, n); i++) sb.append('-');
        return sb.toString();
    }
    private static String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }
}
