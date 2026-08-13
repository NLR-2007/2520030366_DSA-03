package algo;

import java.util.ArrayList;
import java.util.List;

/**
 * ==========================================================================
 * ALGORITHM 1 : KNUTH - MORRIS - PRATT  (exact string matching)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   - confirming that an exact crop / disease name really occurs in a document
 *   - finding the EXACT POSITION of that word so we can cut a snippet to show
 *   - checking a symptom keyword against a disease symptom list
 *
 * IDEA
 *   A naive search restarts from scratch after a mismatch. KMP first builds an
 *   LPS array (Longest Proper Prefix which is also a Suffix). On a mismatch the
 *   pattern slides forward using LPS, so the text pointer NEVER goes backwards.
 *
 * TIME  : O(n + m)      SPACE : O(m)
 * ==========================================================================
 */
public class KMP {

    /** Step 1 : build the LPS (failure) table of the pattern. */
    public static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;                       // length of the current matching prefix
        int i = 1;
        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else if (len > 0) {
                len = lps[len - 1];        // fall back, do NOT move i
            } else {
                lps[i] = 0;
                i++;
            }
        }
        return lps;
    }

    /** Step 2 : return the index of the FIRST occurrence, or -1 if absent. */
    public static int search(String text, String pattern) {
        if (pattern.isEmpty() || text.length() < pattern.length()) return -1;
        int[] lps = buildLPS(pattern);
        int i = 0, j = 0;                  // i -> text, j -> pattern
        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++; j++;
                if (j == pattern.length()) return i - j;   // full match found
            } else if (j > 0) {
                j = lps[j - 1];            // slide the pattern, keep i
            } else {
                i++;
            }
        }
        return -1;
    }

    /** Return every starting index where the pattern occurs. */
    public static List<Integer> searchAll(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        if (pattern.isEmpty()) return hits;
        int[] lps = buildLPS(pattern);
        int i = 0, j = 0;
        while (i < text.length()) {
            if (text.charAt(i) == pattern.charAt(j)) {
                i++; j++;
                if (j == pattern.length()) {
                    hits.add(i - j);
                    j = lps[j - 1];        // continue searching for overlaps
                }
            } else if (j > 0) {
                j = lps[j - 1];
            } else {
                i++;
            }
        }
        return hits;
    }

    public static boolean contains(String text, String pattern) {
        return search(text, pattern) >= 0;
    }
}
