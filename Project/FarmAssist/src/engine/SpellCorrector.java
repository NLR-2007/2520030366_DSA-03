package engine;

import algo.EditDistance;
import model.Crop;
import model.Disease;
import model.Fertilizer;
import util.Trace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * STAGE 1 OF THE PIPELINE  ->  uses ALGORITHM 4 : EDIT DISTANCE
 *
 * Builds a dictionary from every word that exists in our database and then
 * repairs each mis-spelled word of the farmer's question before searching.
 */
public class SpellCorrector {

    private final Set<String> dictionary = new LinkedHashSet<>();
    private final CompoundSplitter splitter = new CompoundSplitter(dictionary);

    public SpellCorrector(DataLoader data, List<String> extraWords) {
        // Order matters. When two dictionary words are equally close to the typo,
        // bestMatch keeps the one it saw first, so everyday words are added FIRST.
        // Otherwise "whaat" becomes the crop "wheat" instead of the word "what".
        for (String w : DataLoader.commonWords()) addWords(w);
        for (String w : extraWords) addWords(w);        // greeting words, so "hi" survives
        for (String[] pair : data.synonyms) addWords(pair[0]);   // "paddy" is a real word

        for (Crop c : data.crops)              addWords(c.name);
        for (Disease d : data.diseases)      { addWords(d.name); for (String s : d.symptoms) addWords(s); }
        for (Fertilizer f : data.fertilizers)  addWords(f.name);
        for (String s : data.symptomKeywords)  addWords(s);
    }

    private void addWords(String phrase) {
        for (String w : phrase.toLowerCase().split("[^a-z]+")) {
            if (w.length() >= 3) dictionary.add(w);
        }
    }

    public static class Correction {
        public final String wrong, right;
        public final int distance;
        Correction(String wrong, String right, int distance) {
            this.wrong = wrong; this.right = right; this.distance = distance;
        }
    }

    public static class Result {
        public String correctedQuery;
        public List<Correction> corrections = new ArrayList<>();
    }

    /** Fix every unknown word of the query using the closest dictionary word. */
    public Result correct(String query) {
        Result result = new Result();
        StringBuilder rebuilt = new StringBuilder();

        // walk the query keeping punctuation and numbers exactly as they are
        int i = 0;
        while (i < query.length()) {
            char c = query.charAt(i);
            if (Character.isLetter(c)) {
                int j = i;
                while (j < query.length() && Character.isLetter(query.charAt(j))) j++;
                String word = query.substring(i, j);
                rebuilt.append(fixWord(word, result));
                i = j;
            } else {
                rebuilt.append(c);
                i++;
            }
        }

        result.correctedQuery = rebuilt.toString();
        if (result.corrections.isEmpty()) {
            Trace.log("Edit Distance", "no spelling mistakes found in the question");
        }
        return result;
    }

    private String fixWord(String word, Result result) {
        String lower = word.toLowerCase();

        if (lower.length() < 4)             return word;   // too short to correct safely
        if (dictionary.contains(lower))     return word;   // already a known word

        // "lateblight" is not a typo, it is two words stuck together.
        // Edit Distance cannot repair that, so try splitting first.
        String pieces = splitter.split(lower);
        if (pieces != null) {
            result.corrections.add(new Correction(lower, pieces, 0));
            Trace.log("Word splitter", "\"" + lower + "\" is two joined words -> \"" + pieces + "\"");
            return pieces;
        }

        int maxDistance = (lower.length() <= 6) ? 1 : 2;   // longer word, more tolerance
        String best = EditDistance.bestMatch(lower, dictionary, maxDistance, true);

        if (best == null) return word;                     // nothing close, leave it alone

        int d = EditDistance.distance(lower, best);
        result.corrections.add(new Correction(lower, best, d));
        Trace.log("Edit Distance", "\"" + lower + "\" -> \"" + best + "\"  (distance " + d + ")");
        return best;
    }

    public int dictionarySize() { return dictionary.size(); }
}
