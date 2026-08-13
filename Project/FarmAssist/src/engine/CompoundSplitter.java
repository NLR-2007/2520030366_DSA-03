package engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * JOINED WORD SPLITTER  ->  dynamic programming (the same DP idea as Edit Distance)
 *
 * Farmers type "lateblight" or "leafcurl" without the space. Edit Distance
 * cannot repair that, because "lateblight" is four edits away from "blight"
 * and the lengths are too far apart. The right tool is word segmentation.
 *
 * IDEA
 *   best[i] = the smallest number of dictionary words that exactly cover the
 *             first i characters, or INFINITE when that is impossible.
 *   best[0] = 0. For each end i we try every start j and ask whether
 *   word[j..i] is a known word and whether best[j] was reachable.
 *   Then we walk the cut[] array backwards to rebuild the split.
 *
 * Every piece must be a REAL word from our dictionary, so a genuinely unknown
 * word like "pomegranate" is left untouched instead of being cut into rubbish.
 *
 * TIME : O(n^2) per word, and n is one word - so it is effectively free.
 */
public class CompoundSplitter {

    private static final int INFINITE = Integer.MAX_VALUE / 2;
    private static final int MIN_PIECE = 3;    // no 1 or 2 letter fragments
    private static final int MAX_PIECE = 20;
    private static final int MIN_INPUT = 7;    // shorter words are not worth splitting

    private final Set<String> dictionary;

    public CompoundSplitter(Set<String> dictionary) {
        this.dictionary = dictionary;
    }

    /** @return "late blight" for "lateblight", or null when it cannot be split. */
    public String split(String word) {
        int n = word.length();
        if (n < MIN_INPUT) return null;

        int[] best = new int[n + 1];
        int[] cut = new int[n + 1];
        Arrays.fill(best, INFINITE);
        best[0] = 0;

        for (int i = MIN_PIECE; i <= n; i++) {
            int from = Math.max(0, i - MAX_PIECE);
            for (int j = from; j <= i - MIN_PIECE; j++) {
                if (best[j] == INFINITE) continue;
                if (!dictionary.contains(word.substring(j, i))) continue;
                if (best[j] + 1 < best[i]) {
                    best[i] = best[j] + 1;
                    cut[i] = j;
                }
            }
        }

        // it must split into at least TWO real words, otherwise there is no point
        if (best[n] >= INFINITE || best[n] < 2) return null;

        List<String> pieces = new ArrayList<>();
        for (int i = n; i > 0; i = cut[i]) pieces.add(0, word.substring(cut[i], i));
        return String.join(" ", pieces);
    }
}
