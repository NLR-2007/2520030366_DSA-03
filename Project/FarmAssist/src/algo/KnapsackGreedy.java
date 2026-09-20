package algo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ==========================================================================
 * ALGORITHM 12 : GREEDY KNAPSACK  (a 1/2-approximation, with parallel prefix)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   Next to the exact 0/1 Knapsack (algorithm 6) on every budget question,
 *   so the two answers can be compared on screen.
 *
 * WHY IT EXISTS
 *   0/1 KNAPSACK is NP-hard: SUBSET SUM (from 3-SAT via PARTITION) reduces to
 *   it by setting value = weight for every item. The DP in algorithm 6 is
 *   only PSEUDO-polynomial - O(n * budget) grows with the NUMBER in the
 *   budget, not the length of its digits - so a budget of Rs 10 crore would
 *   need a table of a billion cells. This greedy always runs in O(n log n).
 *
 * THE APPROXIMATION
 *   1. Sort items by value per rupee (best bang for the buck first).
 *   2. Walk down the list, taking every item that still fits.
 *   3. Compare with the single most valuable item that fits on its own.
 *   4. Return whichever of the two is better.
 *   Step 3 is what turns "no guarantee" into a PROVEN guarantee: the greedy
 *   prefix plus the first item that did not fit is at least the fractional
 *   optimum, which is at least OPT; so the better of the two halves is
 *   at least OPT / 2.  (The full FPTAS scales values down before the DP; the
 *   1/2 bound is enough to show the idea.)
 *
 * PARALLEL PRIMITIVE
 *   Step 2 needs, for each position, the running total of costs before it -
 *   a PREFIX SUM. It is computed with the parallel prefix from
 *   ParallelPrimitives, so this is also where the work-span numbers shown in
 *   the trace come from.
 *
 * TIME  : O(n log n) sort + O(n) work, O(log n) span for the prefix
 * ==========================================================================
 */
public class KnapsackGreedy {

    public static class Result {
        public List<Integer> chosen = new ArrayList<>();
        public int totalValue;
        public int totalCost;
        public boolean usedSingleItem;     // true when step 3 won
    }

    public static Result solve(int[] cost, int[] value, int budget) {
        Result r = new Result();
        int n = cost.length;
        if (n == 0 || budget <= 0) return r;

        // 1. order by value per rupee
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < n; i++) order.add(i);
        RandomizedQuickSort.sort(order, Comparator.comparingDouble(
                (Integer i) -> -(double) value[i] / cost[i]));

        // 2. running cost down the sorted list, computed as a parallel prefix sum
        long[] sortedCost = new long[n];
        for (int k = 0; k < n; k++) sortedCost[k] = cost[order.get(k)];
        long[] prefix = ParallelPrimitives.prefixSum(sortedCost);

        // take the longest prefix that fits (prefix[k] is the cost BEFORE item k),
        // then keep filling gaps with whatever still fits
        long spent = 0;
        for (int k = 0; k < n; k++) {
            int i = order.get(k);
            boolean inPrefix = prefix[k] + sortedCost[k] <= budget;
            if (inPrefix || spent + cost[i] <= budget) {
                r.chosen.add(i);
                spent += cost[i];
                r.totalValue += value[i];
            }
        }
        r.totalCost = (int) spent;

        // 3. the single best item that fits on its own
        int bestSingle = -1;
        for (int i = 0; i < n; i++) {
            if (cost[i] <= budget && (bestSingle == -1 || value[i] > value[bestSingle])) bestSingle = i;
        }

        // 4. better of the two
        if (bestSingle != -1 && value[bestSingle] > r.totalValue) {
            r.chosen.clear();
            r.chosen.add(bestSingle);
            r.totalValue = value[bestSingle];
            r.totalCost = cost[bestSingle];
            r.usedSingleItem = true;
        }
        return r;
    }
}
