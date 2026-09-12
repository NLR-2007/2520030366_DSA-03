package algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Aho-Corasick multi pattern search.
 *
 * KMP finds one pattern at a time. Here we push all the patterns into a trie
 * and add a "failure link" to every node, which points to the longest proper
 * suffix that is also a node in the trie. Then one walk over the text finds
 * every pattern at once, no matter how many patterns there are.
 *
 * Build : O(total length of all patterns)
 * Search: O(length of text + number of matches)
 */
public class AhoCorasick {

    private static class Node {
        Map<Character, Node> next = new HashMap<>();
        Node fail;                                  // where to go on a mismatch
        List<String> words = new ArrayList<>();     // patterns ending here
        List<String> types = new ArrayList<>();     // what each one is
    }

    /** One reported hit. */
    public static class Match {
        public final String word;
        public final String type;
        public final int position;

        Match(String word, String type, int position) {
            this.word = word;
            this.type = type;
            this.position = position;
        }

        @Override
        public String toString() {
            return type + ":" + word + "@" + position;
        }
    }

    private final Node root = new Node();
    private int patternCount = 0;
    private boolean built = false;

    /** Add one pattern before calling build(). Type is just a label, e.g. CROP. */
    public void addPattern(String word, String type) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase();

        Node node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            node.next.putIfAbsent(c, new Node());
            node = node.next.get(c);
        }
        node.words.add(word);
        node.types.add(type);
        patternCount++;
        built = false;
    }

    /**
     * Work out the failure links, breadth first. A node's failure link is
     * found by following its parent's failure link and looking for the same
     * character there.
     */
    public void build() {
        Queue<Node> queue = new LinkedList<>();

        // depth 1 always falls back to the root
        for (Node child : root.next.values()) {
            child.fail = root;
            queue.add(child);
        }

        while (!queue.isEmpty()) {
            Node current = queue.poll();

            for (Map.Entry<Character, Node> entry : current.next.entrySet()) {
                char c = entry.getKey();
                Node child = entry.getValue();

                Node f = current.fail;
                while (f != null && !f.next.containsKey(c)) {
                    f = f.fail;
                }
                child.fail = (f == null) ? root : f.next.get(c);

                // a suffix match is also a match here, so copy it up
                child.words.addAll(child.fail.words);
                child.types.addAll(child.fail.types);

                queue.add(child);
            }
        }
        built = true;
    }

    /**
     * One pass over the text. Only whole words are reported, so "rice" is not
     * found inside "price" and "spot" is not found inside "spotless".
     */
    public List<Match> search(String text) {
        List<Match> found = new ArrayList<>();
        if (text == null || text.isEmpty()) return found;
        if (!built) build();

        String lower = text.toLowerCase();
        Node node = root;

        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);

            while (node != root && !node.next.containsKey(c)) {
                node = node.fail;
            }
            if (node.next.containsKey(c)) {
                node = node.next.get(c);
            }

            for (int k = 0; k < node.words.size(); k++) {
                String word = node.words.get(k);
                int start = i - word.length() + 1;
                if (isWholeWord(lower, start, i)) {
                    found.add(new Match(word, node.types.get(k), start));
                }
            }
        }
        return found;
    }

    /** True when the match is not glued to a letter on either side. */
    private boolean isWholeWord(String text, int start, int end) {
        if (start > 0 && Character.isLetterOrDigit(text.charAt(start - 1))) return false;
        if (end < text.length() - 1 && Character.isLetterOrDigit(text.charAt(end + 1))) return false;
        return true;
    }

    public int patternCount() {
        return patternCount;
    }
}
