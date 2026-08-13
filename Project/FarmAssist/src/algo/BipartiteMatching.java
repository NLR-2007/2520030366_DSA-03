package algo;

import java.util.Arrays;

/**
 * ==========================================================================
 * ALGORITHM 7 : MAXIMUM BIPARTITE MATCHING  (Kuhn's augmenting path method)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "I grow rice, cotton and banana - assign one fertilizer to each crop."
 *   Left side  = the farmer's crops.
 *   Right side = available fertilizers (one bag each, so a fertilizer cannot
 *                be given to two crops).
 *   An edge exists when the fertilizer suits that crop.
 *   Maximum matching = the largest number of crops that can be served.
 *
 * IDEA
 *   For every crop try to find an AUGMENTING PATH. If the fertilizer we want is
 *   free, take it. If it is already taken, ask its current owner to move to
 *   some other fertilizer (recursion). If the owner can move, we steal it.
 *
 * TIME  : O(V * E)
 * ==========================================================================
 */
public class BipartiteMatching {

    /**
     * @param adj adj[crop][fertilizer] = true when that pair is compatible
     * @return matchLeft[crop] = fertilizer index assigned to it, or -1
     */
    public static int[] maxMatching(boolean[][] adj, int leftCount, int rightCount) {
        int[] matchRight = new int[rightCount];   // fertilizer -> crop
        Arrays.fill(matchRight, -1);

        for (int crop = 0; crop < leftCount; crop++) {
            boolean[] visited = new boolean[rightCount];
            tryAssign(crop, adj, visited, matchRight);
        }

        int[] matchLeft = new int[leftCount];     // flip it back: crop -> fertilizer
        Arrays.fill(matchLeft, -1);
        for (int f = 0; f < rightCount; f++) {
            if (matchRight[f] != -1) matchLeft[matchRight[f]] = f;
        }
        return matchLeft;
    }

    /** Try to give some fertilizer to this crop, moving others if needed. */
    private static boolean tryAssign(int crop, boolean[][] adj, boolean[] visited, int[] matchRight) {
        for (int f = 0; f < matchRight.length; f++) {
            if (!adj[crop][f] || visited[f]) continue;
            visited[f] = true;

            // free fertilizer, or its present owner can shift somewhere else
            if (matchRight[f] == -1 || tryAssign(matchRight[f], adj, visited, matchRight)) {
                matchRight[f] = crop;
                return true;
            }
        }
        return false;
    }

    public static int countMatched(int[] matchLeft) {
        int c = 0;
        for (int v : matchLeft) if (v != -1) c++;
        return c;
    }
}
