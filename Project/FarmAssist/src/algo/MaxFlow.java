package algo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

/**
 * ==========================================================================
 * ALGORITHM 10 : MAXIMUM FLOW  (Ford-Fulkerson with Edmonds-Karp) + MIN CUT
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   "supply 3 fields of rice and 2 fields of cotton".
 *   Bipartite matching gives every crop ONE bag. Real farms have several
 *   fields of a crop and the shop has several bags of each fertilizer, so the
 *   question becomes a flow network:
 *
 *        source --(fields)--> crop --(inf)--> fertilizer --(stock)--> sink
 *
 *   The maximum flow is the largest number of fields that can be supplied.
 *   The MINIMUM CUT that goes with it names the bottleneck: which fields are
 *   short of supply and which shelves are sold out.
 *
 * IDEA
 *   Repeatedly find a path from source to sink that still has spare capacity
 *   (an AUGMENTING PATH) and push as much flow as its narrowest edge allows.
 *   Edmonds-Karp finds each path by BFS, so it is always a SHORTEST path -
 *   that is what bounds the number of rounds to O(V*E) and makes the running
 *   time polynomial instead of depending on the capacities.
 *   Every edge carries a paired REVERSE edge so flow can be undone later.
 *
 *   Max-flow / min-cut duality: after the last round, the vertices the BFS
 *   can still reach from the source form one side of a cut whose capacity
 *   equals the flow. Dinic's algorithm does the same with a level graph and
 *   blocking flows; Edmonds-Karp is enough for a shop-sized network.
 *
 * TIME  : O(V * E^2)
 * ==========================================================================
 */
public class MaxFlow {

    // edges are stored flat; edge e and its reverse twin e^1 sit side by side
    private final List<Integer> to = new ArrayList<>();
    private final List<Integer> capacity = new ArrayList<>();
    private final List<Integer> flow = new ArrayList<>();
    private final List<List<Integer>> adjacency = new ArrayList<>();

    private boolean[] sourceSide;          // filled after maxFlow()
    public int augmentingPaths;

    public MaxFlow(int vertices) {
        for (int i = 0; i < vertices; i++) adjacency.add(new ArrayList<>());
    }

    /** Add a directed edge and its reverse twin; returns an id for flowOn(). */
    public int addEdge(int from, int target, int cap) {
        int id = to.size();
        to.add(target);   capacity.add(cap); flow.add(0); adjacency.get(from).add(id);
        to.add(from);     capacity.add(0);   flow.add(0); adjacency.get(target).add(id + 1);
        return id;
    }

    private int residual(int e) { return capacity.get(e) - flow.get(e); }

    public int maxFlow(int source, int sink) {
        int total = 0;
        augmentingPaths = 0;
        while (true) {
            // BFS for the shortest path that still has room
            int[] viaEdge = new int[adjacency.size()];
            Arrays.fill(viaEdge, -1);
            boolean[] seen = new boolean[adjacency.size()];
            seen[source] = true;

            Queue<Integer> queue = new ArrayDeque<>();
            queue.add(source);
            while (!queue.isEmpty() && !seen[sink]) {
                int u = queue.poll();
                for (int e : adjacency.get(u)) {
                    int v = to.get(e);
                    if (!seen[v] && residual(e) > 0) {
                        seen[v] = true;
                        viaEdge[v] = e;
                        queue.add(v);
                    }
                }
            }
            if (!seen[sink]) {                                    // no path left: done
                sourceSide = seen;                                // reachable set = min cut
                return total;
            }

            // bottleneck of the path, then push it and open the reverse edges
            int push = Integer.MAX_VALUE;
            for (int v = sink; v != source; v = to.get(viaEdge[v] ^ 1)) {
                push = Math.min(push, residual(viaEdge[v]));
            }
            for (int v = sink; v != source; v = to.get(viaEdge[v] ^ 1)) {
                int e = viaEdge[v];
                flow.set(e, flow.get(e) + push);
                flow.set(e ^ 1, flow.get(e ^ 1) - push);
            }
            total += push;
            augmentingPaths++;
        }
    }

    /** Flow currently on the edge returned by addEdge(). */
    public int flowOn(int edgeId) {
        return Math.max(0, flow.get(edgeId));
    }

    /** True when the vertex sits on the source side of the minimum cut. */
    public boolean onSourceSide(int vertex) {
        return sourceSide != null && sourceSide[vertex];
    }
}
