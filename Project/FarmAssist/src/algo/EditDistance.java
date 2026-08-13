package algo;

import java.util.Collection;

/**
 * ==========================================================================
 * ALGORITHM 4 : EDIT DISTANCE  (Levenshtein - dynamic programming)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   Spelling correction. A farmer types "tomatoe leef curl" and we silently
 *   fix it to "tomato leaf curl" before any searching happens. Without this,
 *   one typo would return zero results.
 *
 * IDEA
 *   dp[i][j] = minimum number of insert / delete / replace operations needed
 *              to convert the first i characters of a into the first j of b.
 *   If the characters match, nothing is spent: dp[i][j] = dp[i-1][j-1].
 *   Otherwise pay 1 and take the cheapest of the three neighbours.
 *
 * TIME  : O(n * m)     SPACE : O(n * m)
 * ==========================================================================
 */
public class EditDistance {

    /** Classic Levenshtein distance between two words. */
    public static int distance(String a, String b) {
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][m + 1];

        for (int i = 0; i <= n; i++) dp[i][0] = i;   // delete every char of a
        for (int j = 0; j <= m; j++) dp[0][j] = j;   // insert every char of b

        for (int i = 1; i <= n; i++) {
            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];                 // free match
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1],    // replace
                                   Math.min(dp[i - 1][j],        // delete
                                            dp[i][j - 1]));      // insert
                }
            }
        }
        return dp[n][m];
    }

    /**
     * Closest dictionary word to the given word, or null when nothing is
     * close enough. Used by the spell corrector.
     */
    public static String bestMatch(String word, Collection<String> dictionary, int maxDistance) {
        return bestMatch(word, dictionary, maxDistance, false);
    }

    /**
     * @param sameFirstLetter when true, only words starting with the same letter
     *        are considered. People rarely mistype the FIRST letter of a word, so
     *        this one rule stops silly corrections like "strange" -> "orange"
     *        while still fixing "tomatoe" -> "tomato".
     */
    public static String bestMatch(String word, Collection<String> dictionary,
                                   int maxDistance, boolean sameFirstLetter) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : dictionary) {
            // cheap length filter - saves a lot of DP work
            if (Math.abs(candidate.length() - word.length()) > maxDistance) continue;
            if (sameFirstLetter && !word.isEmpty() && !candidate.isEmpty()
                    && word.charAt(0) != candidate.charAt(0)) continue;

            int d = distance(word, candidate);
            if (d < bestDist) { bestDist = d; best = candidate; }
        }
        return bestDist <= maxDistance ? best : null;
    }

    /** Distance of the best match (used only for printing the trace). */
    public static int bestDistance(String word, Collection<String> dictionary) {
        int bestDist = Integer.MAX_VALUE;
        for (String candidate : dictionary) bestDist = Math.min(bestDist, distance(word, candidate));
        return bestDist;
    }
}
