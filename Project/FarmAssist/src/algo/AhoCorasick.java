package algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * ==========================================================================
 * ALGORITHM 3 : AHO - CORASICK  (multi pattern search in ONE pass)
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   The farmer types one sentence such as
 *        "my tomato has yellow leaves and brown spots, which fertilizer"
 *   We must detect ALL known entities at once - crop names, disease names,
 *   fertilizer names and symptom keywords. Running KMP once per pattern would
 *   need hundreds of passes. Aho-Corasick finds every pattern in a single pass.
 *
 * IDEA
 *   1. Put all patterns in a TRIE.
 *   2. Build FAILURE LINKS with a BFS: node.fail points to the longest proper
 *      suffix of the current prefix that is also a prefix of some pattern.
 *      This is exactly the KMP idea generalised to many patterns.
 *   3. Walk the text once. On a mismatch jump along fail links instead of
 *      restarting. Collect the output of every node we land on.
 *
 * TIME  : O(textLength + totalPatternLength + numberOfMatches)
 * ==========================================================================
 */
public class AhoCorasick {

    /** One detected entity inside the query. */
    public static class Match {
        public final String pattern;    // e.g. "tomato"
        public final String category;   // CROP / DISEASE / FERTILIZER / SYMPTOM
        public final int start;         // index in the query
        Match(String pattern, String category, int start) {
            this.pattern = pattern; this.category = category; this.start = start;
        }
        @Override public String toString() { return category + ":" + pattern; }
    }

    private static class Node {
        Map<Character, Node> next = new HashMap<>();
        Node fail;
        List<String[]> output = new ArrayList<>();   // {pattern, category}
    }

    private final Node root = new Node();
    private boolean built = false;

    /** Insert one pattern into the trie. Call before build(). */
    public void addPattern(String pattern, String category) {
        if (pattern == null || pattern.isEmpty()) return;
        Node cur = root;
        for (char c : pattern.toLowerCase().toCharArray()) {
            cur = cur.next.computeIfAbsent(c, x -> new Node());
        }
        cur.output.add(new String[]{pattern.toLowerCase(), category});
        built = false;
    }

    /** Build the failure links using a breadth first traversal of the trie. */
    public void build() {
        Queue<Node> q = new LinkedList<>();
        root.fail = root;
        for (Node child : root.next.values()) {     // depth 1 always fails to root
            child.fail = root;
            q.add(child);
        }
        while (!q.isEmpty()) {
            Node cur = q.poll();
            for (Map.Entry<Character, Node> e : cur.next.entrySet()) {
                char c = e.getKey();
                Node child = e.getValue();

                Node f = cur.fail;
                while (f != root && !f.next.containsKey(c)) f = f.fail;
                child.fail = (f.next.containsKey(c) && f.next.get(c) != child)
                           ? f.next.get(c) : root;

                child.output.addAll(child.fail.output);   // inherit suffix outputs
                q.add(child);
            }
        }
        built = true;
    }

    /**
     * Scan the text ONCE and return every pattern found.
     * We only accept a match that begins at a word boundary, so "rice" is not
     * reported inside "price". The end is left free on purpose so a short
     * keyword like "curl" still detects "curling".
     */
    public List<Match> search(String text) {
        if (!built) build();
        List<Match> found = new ArrayList<>();
        String s = text.toLowerCase();
        Node cur = root;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            while (cur != root && !cur.next.containsKey(c)) cur = cur.fail;
            if (cur.next.containsKey(c)) cur = cur.next.get(c);

            for (String[] out : cur.output) {
                int start = i - out[0].length() + 1;
                if (startsAtWordBoundary(s, start)) {
                    found.add(new Match(out[0], out[1], start));
                }
            }
        }
        return found;
    }

    private static boolean startsAtWordBoundary(String s, int start) {
        return start == 0 || !Character.isLetter(s.charAt(start - 1));
    }
}
