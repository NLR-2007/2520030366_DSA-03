package engine;

import algo.Trie;
import model.Crop;
import model.Disease;
import model.Fertilizer;
import model.Pest;

import java.util.ArrayList;
import java.util.List;

/**
 * AUTOCOMPLETE & NEXT-WORD PREDICTION ENGINE
 *
 * Uses Trie data structure to index agricultural entities, common templates,
 * and data records to predict next words and complete partial user queries.
 */
public class AutocompleteEngine {

    private final Trie trie = new Trie();

    /** Every phrase in the trie, kept in insertion order for the word-match fallback. */
    private final List<String> phrases = new ArrayList<>();

    public AutocompleteEngine(DataLoader data) {
        buildIndex(data);
    }

    private void add(String phrase) {
        String clean = phrase.trim().toLowerCase();
        if (clean.isEmpty() || phrases.contains(clean)) return;
        trie.insert(clean);
        phrases.add(clean);
    }

    private void buildIndex(DataLoader data) {
        // Index crop names & question templates
        for (Crop c : data.crops) {
            String name = c.name.toLowerCase();
            add(name);
            add("how to grow " + name);
            add("growing " + name);
            add("varieties of " + name);
            add("yield of " + name);
            add("fertilizer for " + name);
            add("suggest fertilizer for " + name + " under 3000");
            add("compare " + name + " and wheat");
            add("compare " + name + " and rice");
        }

        // Index disease names & symptoms
        for (Disease d : data.diseases) {
            String name = d.name.toLowerCase();
            add(name);
            add(name + " treatment");
            add("how to treat " + name);
            for (String s : d.symptoms) {
                add(s);
                add("yellow leaves");
                add("brown spots");
                add("my crop has " + s);
            }
        }

        // Index pests
        for (Pest p : data.pests) {
            String name = p.name.toLowerCase();
            add(name);
            add("how to control " + name);
            add(name + " control");
        }

        // Index fertilizers
        for (Fertilizer f : data.fertilizers) {
            String name = f.name.toLowerCase();
            add(name);
            add("what is " + name);
            add("cost of " + name);
        }

        // Common starter phrases
        add("how to grow");
        add("how to control");
        add("how to treat");
        add("suggest fertilizer for");
        add("match fertilizers for");
        add("my tomato has yellow leaves");
        add("which crops suit low rainfall");
        add("crops for a hot climate");
        add("weather in");
        add("weather today");
        add("weather in hyderabad");
        add("will it rain tomorrow");
        add("forecast for this week");
    }

    /**
     * Given a word or partial prefix (e.g. "how", "tom", "fert"),
     * returns predicted next-word completions.
     */
    public List<String> predict(String input, int maxResults) {
        if (input == null || input.trim().isEmpty()) return new ArrayList<>();
        return trie.predictNextWords(input.trim().toLowerCase(), maxResults);
    }

    /**
     * Live search-box completions, the way a video site fills the drop down
     * under the search bar while you type.
     *
     * Phrases that start with what was typed come first (an O(L) trie walk),
     * then phrases where every typed word starts some word of the phrase, so
     * "yellow tom" still finds "my tomato has yellow leaves". Shorter phrases
     * are listed before longer ones so the most direct completion sits on top.
     */
    public List<String> complete(String typed, int maxResults) {
        List<String> out = new ArrayList<>();
        if (typed == null) return out;
        String clean = typed.trim().toLowerCase().replaceAll("\\s+", " ");
        if (clean.isEmpty()) return out;

        List<String> prefix = trie.predictNextWords(clean, Integer.MAX_VALUE);
        prefix.sort((a, b) -> a.length() != b.length() ? a.length() - b.length() : a.compareTo(b));
        for (String p : prefix) {
            if (out.size() >= maxResults) return out;
            if (!p.equals(clean) && !out.contains(p)) out.add(p);
        }

        String[] words = clean.split(" ");
        List<String> loose = new ArrayList<>();
        for (String p : phrases) {
            if (p.equals(clean) || out.contains(p)) continue;
            if (everyWordStartsAWord(words, p.split(" "))) loose.add(p);
        }
        loose.sort((a, b) -> a.length() != b.length() ? a.length() - b.length() : a.compareTo(b));
        for (String p : loose) {
            if (out.size() >= maxResults) break;
            out.add(p);
        }
        return out;
    }

    private static boolean everyWordStartsAWord(String[] typed, String[] phrase) {
        for (String t : typed) {
            boolean hit = false;
            for (String w : phrase) if (w.startsWith(t)) { hit = true; break; }
            if (!hit) return false;
        }
        return true;
    }
}
