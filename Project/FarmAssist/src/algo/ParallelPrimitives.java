package algo;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;
import java.util.function.ToIntFunction;

/**
 * ==========================================================================
 * ALGORITHM 13 : PARALLEL PRIMITIVES  (parallel map, reduce and prefix sum)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   - The crop planner scores every crop against the farmer's climate. Each
 *     score is independent, so the scoring is a PARALLEL MAP, and the best
 *     score comes out of a PARALLEL REDUCE.
 *   - The greedy knapsack needs the running total of costs down a sorted
 *     list: a PARALLEL PREFIX SUM (scan).
 *
 * IDEA - fork/join on a balanced tree
 *   Split the range in half, solve both halves on different threads, join.
 *
 *   reduce     : each node returns the sum (or max) of its two children.
 *   prefix sum : two sweeps over the same tree.
 *                UP   - every node learns the total of its range.
 *                DOWN - every node is told the total to its LEFT, adds its
 *                       left child's total to pass on to the right child.
 *                Leaves then know the sum of everything before them.
 *
 * WORK AND SPAN
 *   work  = total operations if run on one core           = O(n)
 *   span  = the longest chain of dependent operations     = O(log n)
 *   With p cores Brent's bound gives time <= work/p + span, so the speed-up
 *   keeps growing until p is about n / log n. For a table of 60 crops the
 *   tree is 6 levels deep: the numbers in the trace are those two values.
 *
 * The leaves stop at LEAF_SIZE elements and go sequential, because forking a
 * task costs more than adding a handful of numbers.
 * ==========================================================================
 */
public class ParallelPrimitives {

    /** Below this many elements a task works sequentially. */
    private static final int LEAF_SIZE = 8;

    private static final ForkJoinPool POOL = ForkJoinPool.commonPool();

    /** Numbers from the last call, for the trace. */
    public static int lastWork, lastSpan, lastTasks;

    public static int threads() { return POOL.getParallelism(); }

    // ------------------------------------------------------------ map

    /** out[i] = f(items[i]), every leaf block scored on its own thread. */
    public static <T> int[] map(List<T> items, ToIntFunction<T> f) {
        int[] out = new int[items.size()];
        if (items.isEmpty()) { note(0); return out; }
        POOL.invoke(new MapTask<>(items, f, out, 0, items.size()));
        note(items.size());
        return out;
    }

    private static class MapTask<T> extends RecursiveAction {
        final List<T> items; final ToIntFunction<T> f; final int[] out; final int lo, hi;
        MapTask(List<T> items, ToIntFunction<T> f, int[] out, int lo, int hi) {
            this.items = items; this.f = f; this.out = out; this.lo = lo; this.hi = hi;
        }
        @Override protected void compute() {
            if (hi - lo <= LEAF_SIZE) {
                for (int i = lo; i < hi; i++) out[i] = f.applyAsInt(items.get(i));
                return;
            }
            int mid = (lo + hi) >>> 1;
            invokeAll(new MapTask<>(items, f, out, lo, mid),
                      new MapTask<>(items, f, out, mid, hi));
        }
    }

    // ---------------------------------------------------------- reduce

    /** Largest value in the array, folded up a fork/join tree. */
    public static int max(int[] a) {
        if (a.length == 0) { note(0); return Integer.MIN_VALUE; }
        int m = POOL.invoke(new ReduceTask(a, 0, a.length, true));
        note(a.length);
        return m;
    }

    /** Total of the array, folded up a fork/join tree. */
    public static long sum(long[] a) {
        if (a.length == 0) { note(0); return 0; }
        long s = POOL.invoke(new SumTask(a, 0, a.length));
        note(a.length);
        return s;
    }

    private static class ReduceTask extends RecursiveTask<Integer> {
        final int[] a; final int lo, hi; final boolean wantMax;
        ReduceTask(int[] a, int lo, int hi, boolean wantMax) { this.a = a; this.lo = lo; this.hi = hi; this.wantMax = wantMax; }
        @Override protected Integer compute() {
            if (hi - lo <= LEAF_SIZE) {
                int r = wantMax ? Integer.MIN_VALUE : 0;
                for (int i = lo; i < hi; i++) r = wantMax ? Math.max(r, a[i]) : r + a[i];
                return r;
            }
            int mid = (lo + hi) >>> 1;
            ReduceTask left = new ReduceTask(a, lo, mid, wantMax);
            ReduceTask right = new ReduceTask(a, mid, hi, wantMax);
            left.fork();
            int r = right.compute(), l = left.join();
            return wantMax ? Math.max(l, r) : l + r;
        }
    }

    private static class SumTask extends RecursiveTask<Long> {
        final long[] a; final int lo, hi;
        SumTask(long[] a, int lo, int hi) { this.a = a; this.lo = lo; this.hi = hi; }
        @Override protected Long compute() {
            if (hi - lo <= LEAF_SIZE) {
                long s = 0;
                for (int i = lo; i < hi; i++) s += a[i];
                return s;
            }
            int mid = (lo + hi) >>> 1;
            SumTask left = new SumTask(a, lo, mid), right = new SumTask(a, mid, hi);
            left.fork();
            return right.compute() + left.join();
        }
    }

    // ------------------------------------------------------ prefix sum

    /**
     * Exclusive prefix sum: out[i] = a[0] + ... + a[i-1], so out[0] = 0.
     * Two sweeps over one fork/join tree, O(n) work and O(log n) span.
     */
    public static long[] prefixSum(long[] a) {
        long[] out = new long[a.length];
        if (a.length == 0) { note(0); return out; }
        Node root = POOL.invoke(new UpSweep(a, 0, a.length));         // build totals
        POOL.invoke(new DownSweep(a, out, root, 0));                  // hand down left sums
        note(a.length);
        return out;
    }

    /** One node of the scan tree: the range [lo, hi) and its total. */
    private static class Node {
        final int lo, hi; long sum; Node left, right;
        Node(int lo, int hi) { this.lo = lo; this.hi = hi; }
    }

    private static class UpSweep extends RecursiveTask<Node> {
        final long[] a; final int lo, hi;
        UpSweep(long[] a, int lo, int hi) { this.a = a; this.lo = lo; this.hi = hi; }
        @Override protected Node compute() {
            Node n = new Node(lo, hi);
            if (hi - lo <= LEAF_SIZE) {
                for (int i = lo; i < hi; i++) n.sum += a[i];
                return n;
            }
            int mid = (lo + hi) >>> 1;
            UpSweep left = new UpSweep(a, lo, mid), right = new UpSweep(a, mid, hi);
            left.fork();
            n.right = right.compute();
            n.left = left.join();
            n.sum = n.left.sum + n.right.sum;
            return n;
        }
    }

    private static class DownSweep extends RecursiveAction {
        final long[] a, out; final Node node; final long leftSum;   // total before node.lo
        DownSweep(long[] a, long[] out, Node node, long leftSum) {
            this.a = a; this.out = out; this.node = node; this.leftSum = leftSum;
        }
        @Override protected void compute() {
            if (node.left == null) {                                  // leaf: sequential scan
                long run = leftSum;
                for (int i = node.lo; i < node.hi; i++) { out[i] = run; run += a[i]; }
                return;
            }
            invokeAll(new DownSweep(a, out, node.left, leftSum),
                      new DownSweep(a, out, node.right, leftSum + node.left.sum));
        }
    }

    // ------------------------------------------------------------ stats

    /** Record work = n, span = depth of the tree, tasks = leaves forked. */
    private static void note(int n) {
        lastWork = n;
        int leaves = 1, depth = 0;
        for (int size = n; size > LEAF_SIZE; size = (size + 1) / 2) { leaves *= 2; depth++; }
        lastSpan = depth + Math.min(n, LEAF_SIZE);
        lastTasks = n == 0 ? 0 : leaves;
    }
}
