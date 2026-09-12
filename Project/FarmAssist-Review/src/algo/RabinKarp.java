package algo;

import java.util.ArrayList;
import java.util.List;

/**
 * Rabin-Karp search using a rolling hash.
 *
 * Instead of comparing the pattern against every window character by
 * character, we compare a number. The hash of the next window is worked out
 * from the previous one in O(1) - drop the leading character, shift, add the
 * new trailing character.
 *
 * Two hashes being equal does not prove the strings are equal, so every hash
 * hit is confirmed with a real comparison. That is why the worst case is
 * O(n*m), even though in practice it behaves like O(n + m).
 */
public class RabinKarp {

    private static final int BASE = 256;          // alphabet size we treat text as
    private static final int MOD = 1_000_000_007; // prime, keeps the hash in range

    /** All positions where the pattern occurs. */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> hits = new ArrayList<>();
        if (text == null || pattern == null) return hits;

        int n = text.length();
        int m = pattern.length();
        if (m == 0 || m > n) return hits;

        // BASE^(m-1) % MOD - needed to remove the leading character later
        long high = 1;
        for (int i = 0; i < m - 1; i++) {
            high = (high * BASE) % MOD;
        }

        long patternHash = 0;
        long windowHash = 0;
        for (int i = 0; i < m; i++) {
            patternHash = (patternHash * BASE + pattern.charAt(i)) % MOD;
            windowHash = (windowHash * BASE + text.charAt(i)) % MOD;
        }

        for (int i = 0; i + m <= n; i++) {
            // hashes match, but they could still be different strings
            if (windowHash == patternHash && sameAt(text, pattern, i)) {
                hits.add(i);
            }

            // roll the window one step to the right
            if (i + m < n) {
                windowHash = (windowHash - text.charAt(i) * high % MOD + MOD) % MOD;
                windowHash = (windowHash * BASE + text.charAt(i + m)) % MOD;
            }
        }
        return hits;
    }

    /** How many times the pattern appears. */
    public static int count(String text, String pattern) {
        return search(text, pattern).size();
    }

    /** The confirmation step after a hash collision. */
    private static boolean sameAt(String text, String pattern, int start) {
        for (int i = 0; i < pattern.length(); i++) {
            if (text.charAt(start + i) != pattern.charAt(i)) return false;
        }
        return true;
    }
}
