package algo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TRIE DATA STRUCTURE FOR PREFIX MATCHING & NEXT-WORD PREDICTION
 *
 * Stores phrases in a tree where each node represents a character.
 * Allows O(L) prefix lookup to predict next words and complete queries.
 */
public class Trie {

    public static class Node {
        public final Map<Character, Node> children = new HashMap<>();
        public boolean isEnd = false;
        public final List<String> phrases = new ArrayList<>();
    }

    private final Node root = new Node();

    public void insert(String phrase) {
        if (phrase == null || phrase.trim().isEmpty()) return;
        String clean = phrase.trim().toLowerCase();
        Node curr = root;
        for (char c : clean.toCharArray()) {
            curr = curr.children.computeIfAbsent(c, k -> new Node());
        }
        curr.isEnd = true;
        if (!curr.phrases.contains(phrase)) {
            curr.phrases.add(phrase);
        }
    }

    public List<String> predictNextWords(String prefix, int maxResults) {
        List<String> results = new ArrayList<>();
        if (prefix == null || prefix.trim().isEmpty()) return results;

        String clean = prefix.trim().toLowerCase();
        Node curr = root;
        for (char c : clean.toCharArray()) {
            curr = curr.children.get(c);
            if (curr == null) return results; // No prefix match
        }

        collect(curr, results, maxResults);
        return results;
    }

    private void collect(Node node, List<String> results, int maxResults) {
        if (results.size() >= maxResults) return;
        if (node.isEnd) {
            for (String p : node.phrases) {
                if (!results.contains(p)) {
                    results.add(p);
                    if (results.size() >= maxResults) return;
                }
            }
        }
        for (Node child : node.children.values()) {
            collect(child, results, maxResults);
            if (results.size() >= maxResults) return;
        }
    }
}
