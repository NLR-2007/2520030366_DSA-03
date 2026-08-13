package algo;

import java.util.Arrays;
import java.util.Comparator;

/**
 * ==========================================================================
 * ALGORITHM 5 : SUFFIX ARRAY + LCP  (Kasai)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "Related articles". After we show the best article we must suggest other
 *   articles that talk about the same thing. We measure similarity as the
 *   LONGEST COMMON SUBSTRING between the two article bodies, which a suffix
 *   array plus its LCP array gives us directly.
 *
 * IDEA
 *   Suffix array = all suffixes of the text sorted alphabetically, stored as
 *   starting indices. Built here by rank doubling: sort by the first character,
 *   then by the first 2, then 4, 8 ... comparing two already computed ranks.
 *   LCP array (Kasai) = length of the common prefix of two neighbouring
 *   suffixes in that sorted order.
 *
 *   Longest common substring of A and B: build the suffix array of
 *   A + '#' + B, then look for the largest LCP value between two neighbouring
 *   suffixes that come from DIFFERENT sides of the separator.
 *
 * TIME  : O(n log^2 n) to build, O(n) for Kasai
 * ==========================================================================
 */
public class SuffixArrayLCP {

    /** Separator placed between the two texts. It never occurs in real data. */
    public static final char SEPARATOR = (char) 1;

    private final String text;
    private final int[] suffixArray;
    private final int[] lcp;

    public SuffixArrayLCP(String text) {
        this.text = text;
        this.suffixArray = buildSuffixArray(text);
        this.lcp = buildLCP(text, suffixArray);
    }

    public int[] getSuffixArray() { return suffixArray; }
    public int[] getLcp()         { return lcp; }
    public String getText()       { return text; }

    /** Sorted suffix start indices, built by rank doubling. */
    public static int[] buildSuffixArray(String s) {
        int n = s.length();
        if (n == 0) return new int[0];

        Integer[] order = new Integer[n];
        final int[] rank = new int[n];
        int[] tmp = new int[n];

        for (int i = 0; i < n; i++) { order[i] = i; rank[i] = s.charAt(i); }

        for (int k = 1; ; k <<= 1) {
            final int step = k;
            Comparator<Integer> cmp = (a, b) -> {
                if (rank[a] != rank[b]) return Integer.compare(rank[a], rank[b]);
                int ra = (a + step < n) ? rank[a + step] : -1;
                int rb = (b + step < n) ? rank[b + step] : -1;
                return Integer.compare(ra, rb);
            };
            Arrays.sort(order, cmp);

            tmp[order[0]] = 0;
            for (int i = 1; i < n; i++) {
                tmp[order[i]] = tmp[order[i - 1]] + (cmp.compare(order[i - 1], order[i]) < 0 ? 1 : 0);
            }
            System.arraycopy(tmp, 0, rank, 0, n);
            if (rank[order[n - 1]] == n - 1) break;   // all ranks distinct - done
        }

        int[] sa = new int[n];
        for (int i = 0; i < n; i++) sa[i] = order[i];
        return sa;
    }

    /** Kasai's algorithm - LCP of every neighbouring pair in the suffix array. */
    public static int[] buildLCP(String s, int[] sa) {
        int n = s.length();
        int[] lcp = new int[n];
        if (n == 0) return lcp;

        int[] pos = new int[n];                       // pos[i] = place of suffix i in sa
        for (int i = 0; i < n; i++) pos[sa[i]] = i;

        int h = 0;
        for (int i = 0; i < n; i++) {
            if (pos[i] > 0) {
                int j = sa[pos[i] - 1];
                while (i + h < n && j + h < n && s.charAt(i + h) == s.charAt(j + h)) h++;
                lcp[pos[i]] = h;
                if (h > 0) h--;                       // key trick: h shrinks by at most 1
            } else {
                h = 0;
            }
        }
        return lcp;
    }

    /** Longest common substring of two texts - our "how related are they" score. */
    public static String longestCommonSubstring(String a, String b) {
        if (a.isEmpty() || b.isEmpty()) return "";
        String combined = a + SEPARATOR + b;    // separator never occurs in real text
        int split = a.length();

        int[] sa = buildSuffixArray(combined);
        int[] lcp = buildLCP(combined, sa);

        int bestLen = 0, bestStart = 0;
        for (int i = 1; i < sa.length; i++) {
            boolean firstFromA  = sa[i - 1] < split;
            boolean secondFromA = sa[i]     < split;
            if (firstFromA != secondFromA && lcp[i] > bestLen) {   // different sides
                bestLen = lcp[i];
                bestStart = sa[i];
            }
        }
        return combined.substring(bestStart, bestStart + bestLen);
    }
}
