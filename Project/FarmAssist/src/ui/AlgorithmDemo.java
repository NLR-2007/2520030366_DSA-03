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
 * be checked by hand during the viva. Drawn on the same grid as the rest of
 * the app, so it stays readable inside the centred page.
 */
public class AlgorithmDemo {

    private static final String PAD = "  ";
    private static final int LABEL_W = 14;

    public static void runAll() {
        System.out.println();
        System.out.println(Theme.panelTop(Theme.I_SPARK, "algorithm demonstration"));
        System.out.println(Theme.panelRow(Theme.stone("all eight algorithms, on inputs small enough to check by hand")));
        System.out.println(Theme.panelBottom());

        demoKmp();
        demoRabinKarp();
        demoAhoCorasick();
        demoEditDistance();
        demoSuffixArray();
        demoKnapsack();
        demoBipartite();
        demoQuickSort();

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
