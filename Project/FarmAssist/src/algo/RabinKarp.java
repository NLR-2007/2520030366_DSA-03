package algo;

import java.util.ArrayList;
import java.util.List;

/**
 * ==========================================================================
 * ALGORITHM 2 : RABIN - KARP  (rolling hash search)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   Scanning the WHOLE article corpus. For every search term we count how many
 *   times it appears in every document. That count becomes the relevance score.
 *   Rabin-Karp is ideal here because we repeat the same scan over many
 *   documents and only need cheap hash comparisons most of the time.
 *
 * IDEA
 *   Hash the pattern once. Then roll a window of the same length over the text:
 *   removing the first character and adding the next one is O(1) arithmetic.
 *   Only when the two hashes are equal do we verify character by character
 *   (this protects us from hash collisions - a "spurious hit").
 *
 * TIME  : O(n + m) on average        SPACE : O(1)
 * ==========================================================================
 */
public class RabinKarp {

    private static final long BASE = 256;          // alphabet size
    private static final long MOD  = 1000000007L;  // large prime to limit collisions

    /** Every starting index where pattern occurs in text. */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        int n = text.length(), m = pattern.length();
        if (m == 0 || n < m) return hits;

        long patternHash = 0, windowHash = 0, highPower = 1;

        // highPower = BASE^(m-1) % MOD  -> used when removing the leading char
        for (int i = 0; i < m - 1; i++) highPower = (highPower * BASE) % MOD;

        // hash of the pattern and of the first window
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            windowHash  = (windowHash  * BASE + text.charAt(i))    % MOD;
        }

        for (int i = 0; i <= n - m; i++) {
            if (windowHash == patternHash && equalsAt(text, pattern, i)) {
                hits.add(i);                        // verified real match
            }
            if (i < n - m) {                        // roll the window forward
                windowHash = (windowHash - text.charAt(i) * highPower % MOD + MOD) % MOD;
                windowHash = (windowHash * BASE + text.charAt(i + m)) % MOD;
            }
        }
        return hits;
    }

    /** How many times the pattern occurs - this is the raw relevance count. */
    public static int count(String text, String pattern) {
        return search(text, pattern).size();
    }

    private static boolean equalsAt(String text, String pattern, int start) {
        for (int j = 0; j < pattern.length(); j++) {
            if (text.charAt(start + j) != pattern.charAt(j)) return false;
        }
        return true;
    }
}
