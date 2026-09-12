package algo;

/**
 * Levenshtein edit distance, written from scratch.
 *
 * The distance between two words is the smallest number of single character
 * edits - insert, delete, replace - that turns one into the other. In
 * FarmAssist it is the spell checker: the farmer types "tomatoe" or "yelow",
 * Aho-Corasick finds nothing because it only matches exactly, and this class
 * works out which known keyword the typo was probably meant to be.
 *
 * The table is the usual dynamic programming one:
 *
 *     dp[i][j] = distance between the first i characters of a
 *                and the first j characters of b
 *
 *     dp[i][j] = dp[i-1][j-1]                     when the characters agree
 *              = 1 + min( dp[i-1][j]     delete
 *                       , dp[i][j-1]     insert
 *                       , dp[i-1][j-1] ) replace
 *
 * Only the previous row is ever read, so two rows are kept instead of the
 * whole table.
 *
 * Time  : O(n * m)
 * Space : O(min(n, m))
 */
public class EditDistance {

    /** Longest word we are willing to run the table over. */
    private static final int MAX_LENGTH = 256;

    private EditDistance() { }

    /**
     * Distance between two words, comparing in lower case.
     *
     * @throws IllegalArgumentException if either word is null or absurdly long
     */
    public static int distance(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("edit distance needs two words, got a null");
        }
        if (a.length() > MAX_LENGTH || b.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("word too long for the table (limit "
                    + MAX_LENGTH + " characters)");
        }

        a = a.toLowerCase();
        b = b.toLowerCase();

        // the empty word costs one edit per character of the other one
        if (a.isEmpty()) return b.length();
        if (b.isEmpty()) return a.length();

        int n = a.length();
        int m = b.length();

        int[] previous = new int[m + 1];
        int[] current = new int[m + 1];

        // row 0: turning "" into the first j characters of b costs j inserts
        for (int j = 0; j <= m; j++) {
            previous[j] = j;
        }

        for (int i = 1; i <= n; i++) {
            current[0] = i;                       // j = 0: i deletes

            for (int j = 1; j <= m; j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    current[j] = previous[j - 1];              // free, no edit
                } else {
                    int delete  = previous[j];
                    int insert  = current[j - 1];
                    int replace = previous[j - 1];
                    current[j] = 1 + min3(delete, insert, replace);
                }
            }

            // this row becomes the previous one, reuse the old array
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[m];
    }

    /**
     * The distance as a 0..1 similarity, so words of different lengths can be
     * compared fairly. 1.0 is identical, 0.0 is nothing in common.
     */
    public static double similarity(String a, String b) {
        if (a == null || b == null) return 0.0;
        int longest = Math.max(a.length(), b.length());
        if (longest == 0) return 1.0;
        return 1.0 - (double) distance(a, b) / longest;
    }

    /** True when the two words are within the given number of edits. */
    public static boolean isClose(String a, String b, int maxEdits) {
        return distance(a, b) <= maxEdits;
    }

    /** A suggestion and how far away it was. */
    public static class Suggestion {
        public final String word;
        public final int distance;

        Suggestion(String word, int distance) {
            this.word = word;
            this.distance = distance;
        }

        @Override
        public String toString() {
            return word + " (" + distance + (distance == 1 ? " edit)" : " edits)");
        }
    }

    /**
     * The closest word in the vocabulary, or null when nothing is close enough.
     *
     * The budget grows with the length of the word, because one typo in a
     * three letter word is a much bigger change than one in a ten letter word:
     *
     *     up to 4 letters   1 edit
     *     up to 7 letters   2 edits
     *     longer            3 edits
     *
     * Ties are broken by the shorter vocabulary word, which keeps the answer
     * stable however the file happens to be ordered.
     */
    public static Suggestion bestMatch(String word, Iterable<String> vocabulary) {
        if (word == null || word.isEmpty() || vocabulary == null) return null;

        int budget = budgetFor(word);
        String best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String candidate : vocabulary) {
            if (candidate == null || candidate.isEmpty()) continue;

            // a length gap bigger than the budget can never be closed, and
            // skipping it here saves building the table at all
            if (Math.abs(candidate.length() - word.length()) > budget) continue;

            // a typo rarely lands on the first letter, so a candidate that
            // starts differently has to be a single edit away to be believed.
            // Without this, "blite" is corrected to "late" rather than left
            // alone, because "late" happens to be two edits away.
            boolean sameStart = candidate.charAt(0) == word.charAt(0);

            int d;
            try {
                d = distance(word, candidate);
            } catch (IllegalArgumentException e) {
                continue;                          // an unusable entry, ignore it
            }

            if (!sameStart && d > 1) continue;

            if (d < bestDistance || (d == bestDistance && best != null
                                     && candidate.length() < best.length())) {
                bestDistance = d;
                best = candidate;
            }
        }

        if (best == null || bestDistance > budget || bestDistance == 0) return null;
        return new Suggestion(best, bestDistance);
    }

    /** How many edits we forgive in a word of this length. */
    public static int budgetFor(String word) {
        if (word == null) return 0;
        if (word.length() <= 4) return 1;
        if (word.length() <= 7) return 2;
        return 3;
    }

    private static int min3(int a, int b, int c) {
        int min = a;
        if (b < min) min = b;
        if (c < min) min = c;
        return min;
    }
}
