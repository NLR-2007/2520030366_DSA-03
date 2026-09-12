package algo;

import java.util.ArrayList;
import java.util.List;

/**
 * Knuth-Morris-Pratt exact string search.
 *
 * The idea: when a mismatch happens we already know the part of the pattern
 * that matched, so instead of sliding the pattern one step and starting over,
 * we jump to the longest prefix of the pattern that is also a suffix of what
 * we matched. That table of jumps is the LPS array.
 *
 * Time  : O(n + m)
 * Space : O(m)
 */
public class KMP {

    /**
     * lps[i] = length of the longest proper prefix of pattern[0..i] that is
     * also a suffix of it. This is the whole trick of KMP.
     */
    public static int[] buildLPS(String pattern) {
        if (pattern == null) throw new IllegalArgumentException("KMP: the pattern is null");
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;                 // length of the previous longest prefix-suffix
        int i = 1;                   // lps[0] is always 0

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                // don't move i, fall back and try a shorter prefix
                len = lps[len - 1];
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    /** Index of the first occurrence, or -1 if the pattern is not there. */
    public static int search(String text, String pattern) {
        if (text == null || pattern == null) {
            throw new IllegalArgumentException("KMP: both a text and a pattern are needed");
        }
        if (pattern.isEmpty() || pattern.length() > text.length()) return -1;

        int[] lps = buildLPS(pattern);
        int i = 0;                   // index in text
        int j = 0;                   // index in pattern

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) return i - j;
            } else if (j > 0) {
                j = lps[j - 1];      // reuse what we already matched
            } else {
                i++;
            }
        }
        return -1;
    }

    /** Every position where the pattern occurs. Overlaps are included. */
    public static List<Integer> searchAll(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        if (text == null || pattern == null || pattern.isEmpty()) return hits;

        int[] lps = buildLPS(pattern);
        int i = 0, j = 0;

        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++;
                j++;
                if (j == pattern.length()) {
                    hits.add(i - j);
                    j = lps[j - 1];  // keep going, don't restart from scratch
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return hits;
    }

    /** Convenience wrapper used by the search code. */
    public static boolean contains(String text, String pattern) {
        return search(text, pattern) >= 0;
    }
}
