package algo;

import java.util.Arrays;

/**
 * ==========================================================================
 * ALGORITHM 9 : DP ON SUBSETS  (bitmask dynamic programming)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "match fertilizers for rice cotton banana".
 *   Bipartite Matching (algorithm 7) tells us HOW MANY fields can be served.
 *   This DP tells us the BEST WAY to serve them: the assignment whose total
 *   benefit is highest, still giving every bag to at most one field.
 *
 * IDEA
 *   The fields are few (a farmer names 2-8 crops), so a SUBSET of fields fits
 *   in the bits of one int: bit c set  <=>  field c already has a bag.
 *
 *     dp[i][mask] = best benefit after looking at the first i fertilizers,
 *                   with exactly the fields in `mask` served.
 *
 *   For fertilizer i we either leave it on the shelf, or hand it to one field
 *   c that is not yet in the mask and that it suits:
 *
 *     dp[i+1][mask]         <- dp[i][mask]                          (skip)
 *     dp[i+1][mask | 1<<c]  <- dp[i][mask] + benefit[i][c]          (give)
 *
 *   A served field is worth SERVE_BONUS on top of its benefit, so the DP
 *   prefers serving more fields before it prefers richer bags - the answer
 *   therefore is also a maximum matching.
 *
 * TIME  : O(fertilizers * 2^fields * fields)     SPACE : O(fertilizers * 2^fields)
 * ==========================================================================
 */
public class BitmaskAssignment {

    /** More fields than this and the 2^L table stops being small. */
    public static final int MAX_FIELDS = 12;

    /** Serving one more field always beats any benefit gain. */
    private static final int SERVE_BONUS = 1000;

    public static class Result {
        public int[] assignment;      // assignment[field] = fertilizer index, or -1
        public int servedCount;
        public int totalBenefit;      // sum of benefit[f][field] over the assignment
        public int statesVisited;     // size of the DP table, for the trace
    }

    /**
     * @param benefit benefit[fertilizer][field], or a negative number when the
     *                fertilizer does not suit that field
     */
    public static Result solve(int[][] benefit, int fertilizers, int fields) {
        Result r = new Result();
        r.assignment = new int[fields];
        Arrays.fill(r.assignment, -1);
        if (fields == 0 || fertilizers == 0 || fields > MAX_FIELDS) return r;

        int full = 1 << fields;
        int[][] dp = new int[fertilizers + 1][full];
        int[][] gave = new int[fertilizers + 1][full];  // which field got fertilizer i, -1 = skipped
        for (int[] row : dp) Arrays.fill(row, Integer.MIN_VALUE);
        for (int[] row : gave) Arrays.fill(row, -1);
        dp[0][0] = 0;

        for (int i = 0; i < fertilizers; i++) {
            for (int mask = 0; mask < full; mask++) {
                if (dp[i][mask] == Integer.MIN_VALUE) continue;

                if (dp[i][mask] > dp[i + 1][mask]) {                      // skip
                    dp[i + 1][mask] = dp[i][mask];
                    gave[i + 1][mask] = -1;
                }
                for (int c = 0; c < fields; c++) {                        // give to field c
                    if ((mask & (1 << c)) != 0 || benefit[i][c] < 0) continue;
                    int next = mask | (1 << c);
                    int val = dp[i][mask] + SERVE_BONUS + benefit[i][c];
                    if (val > dp[i + 1][next]) {
                        dp[i + 1][next] = val;
                        gave[i + 1][next] = c;
                    }
                }
            }
        }

        // best mask after all fertilizers, then walk back to read the choices
        int bestMask = 0;
        for (int mask = 0; mask < full; mask++) {
            if (dp[fertilizers][mask] > dp[fertilizers][bestMask]) bestMask = mask;
        }
        int mask = bestMask;
        for (int i = fertilizers; i > 0; i--) {
            int c = gave[i][mask];
            if (c >= 0) {
                r.assignment[c] = i - 1;
                r.totalBenefit += benefit[i - 1][c];
                mask &= ~(1 << c);
            }
        }
        r.servedCount = Integer.bitCount(bestMask);
        r.statesVisited = (fertilizers + 1) * full;
        return r;
    }
}
