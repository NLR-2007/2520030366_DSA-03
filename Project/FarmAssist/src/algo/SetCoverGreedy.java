package algo;

import java.util.ArrayList;
import java.util.List;

/**
 * ==========================================================================
 * ALGORITHM 11 : GREEDY SET COVER  (an approximation for an NP-hard problem)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "fewest fertilizers that cover rice wheat cotton banana".
 *   The farmer wants ONE shopping list that suits every crop on the farm,
 *   with as few different products as possible.
 *        universe = the farmer's crops
 *        set      = one fertilizer, i.e. the crops it suits
 *        goal     = pick the fewest sets whose union is the whole universe
 *
 * WHY WE DO NOT SOLVE IT EXACTLY
 *   SET COVER is NP-hard. The decision version ("is there a cover of size k?")
 *   is NP-complete: a certificate (the k sets) is checked in polynomial time,
 *   and VERTEX COVER reduces to it - take every edge as an element and every
 *   vertex as the set of edges touching it; a vertex cover of size k is
 *   exactly a set cover of size k. VERTEX COVER itself comes from 3-SAT, so
 *   unless P = NP there is no polynomial exact algorithm for large inputs.
 *
 * THE APPROXIMATION
 *   Repeatedly take the set that covers the most STILL-UNCOVERED elements.
 *   Chvatal (1979) proved this uses at most  H(n) = 1 + 1/2 + ... + 1/n
 *   times the optimum number of sets, about ln(n) + 1. For a farm with 8
 *   crops that is a guaranteed factor of at most 2.72, and in practice the
 *   greedy answer is usually optimal or one set over.
 *
 * TIME  : O(sets * universe * answer)
 * ==========================================================================
 */
public class SetCoverGreedy {

    public static class Result {
        public List<Integer> chosen = new ArrayList<>();   // indices of picked sets, in pick order
        public List<Integer> gained = new ArrayList<>();   // how many new elements each pick covered
        public boolean complete;                           // every element covered?
        public double bound;                               // H(n) : the proven approximation ratio
    }

    /**
     * @param covers covers[set][element] = true when that set contains the element
     */
    public static Result solve(boolean[][] covers, int sets, int universe) {
        Result r = new Result();
        boolean[] covered = new boolean[universe];
        int left = universe;

        while (left > 0) {
            int best = -1, bestGain = 0;
            for (int s = 0; s < sets; s++) {
                int gain = 0;
                for (int e = 0; e < universe; e++) if (!covered[e] && covers[s][e]) gain++;
                if (gain > bestGain) { bestGain = gain; best = s; }
            }
            if (best == -1) break;                          // nothing covers what is left

            r.chosen.add(best);
            r.gained.add(bestGain);
            for (int e = 0; e < universe; e++) {
                if (!covered[e] && covers[best][e]) { covered[e] = true; left--; }
            }
        }
        r.complete = left == 0;
        r.bound = harmonic(universe);
        return r;
    }

    /** H(n) = 1 + 1/2 + ... + 1/n, the greedy approximation ratio. */
    public static double harmonic(int n) {
        double h = 0;
        for (int i = 1; i <= n; i++) h += 1.0 / i;
        return h;
    }
}
