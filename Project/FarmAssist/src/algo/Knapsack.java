package algo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ==========================================================================
 * ALGORITHM 6 : 0/1 KNAPSACK  (dynamic programming)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "Suggest fertilizers for tomato under 3000 rupees".
 *        item   = one fertilizer
 *        weight = its price per bag        (rupees)
 *        value  = its benefit score for that crop (0-100)
 *        capacity = the farmer's budget
 *   The answer is the BEST COMBINATION of fertilizers the farmer can afford.
 *
 * IDEA
 *   dp[i][w] = best total value using only the first i items with budget w.
 *   For each item we either skip it, or buy it and spend its cost.
 *   After filling the table we walk backwards to find WHICH items were chosen.
 *
 * TIME  : O(items * budget)     SPACE : O(items * budget)
 * ==========================================================================
 */
public class Knapsack {

    /** Budget is scaled down internally so the DP table never explodes. */
    private static final int SCALE_LIMIT = 200000;

    public static class Result {
        public List<Integer> chosen = new ArrayList<>();  // indices of picked items
        public int totalValue;
        public int totalCost;
    }

    public static Result solve(int[] cost, int[] value, int budget) {
        Result result = new Result();
        int n = cost.length;
        if (n == 0 || budget <= 0) return result;
        if (budget > SCALE_LIMIT) budget = SCALE_LIMIT;

        int[][] dp = new int[n + 1][budget + 1];

        for (int i = 1; i <= n; i++) {
            for (int w = 0; w <= budget; w++) {
                dp[i][w] = dp[i - 1][w];                       // option 1 : skip item
                if (cost[i - 1] <= w) {                        // option 2 : buy item
                    int take = dp[i - 1][w - cost[i - 1]] + value[i - 1];
                    if (take > dp[i][w]) dp[i][w] = take;
                }
            }
        }

        // backtrack through the table to recover the chosen items
        int w = budget;
        for (int i = n; i > 0; i--) {
            if (dp[i][w] != dp[i - 1][w]) {                    // item i was taken
                result.chosen.add(i - 1);
                result.totalCost += cost[i - 1];
                w -= cost[i - 1];
            }
        }
        Collections.reverse(result.chosen);
        result.totalValue = dp[n][budget];
        return result;
    }
}
