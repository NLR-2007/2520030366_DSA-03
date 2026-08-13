package engine;

import algo.AhoCorasick;
import util.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VOCABULARY LAYER  ->  reuses ALGORITHM 3 : AHO-CORASICK
 *
 * A farmer may say "paddy", "dhan" or "chawal" but our database only stores
 * "rice". One Aho-Corasick pass finds every alias in the question and rewrites
 * it to the word we actually store, so the search can succeed.
 */
public class SynonymMapper {

    private final AhoCorasick automaton = new AhoCorasick();
    private final Map<String, String> canonical = new HashMap<>();

    public SynonymMapper(DataLoader data) {
        for (String[] pair : data.synonyms) {
            String alias = pair[0].trim().toLowerCase();
            canonical.put(alias, pair[1].trim().toLowerCase());
            automaton.addPattern(alias, "SYNONYM");
        }
        automaton.build();
    }

    /** Replace every alias in the question with the word our database uses. */
    public String rewrite(String query) {
        List<AhoCorasick.Match> raw = automaton.search(query);
        if (raw.isEmpty()) return query;

        String lower = query.toLowerCase();

        // an alias must end at a word boundary too, so "corn" is not replaced
        // inside "corner". A trailing plural "s" is still allowed.
        List<AhoCorasick.Match> valid = new ArrayList<>();
        for (AhoCorasick.Match m : raw) {
            int end = m.start + m.pattern.length();
            if (!endsAtWordBoundary(lower, end)) continue;
            if (alreadyCanonical(lower, m)) continue;   // do not rewrite a word into itself
            valid.add(m);
        }
        if (valid.isEmpty()) return query;

        // longest alias first, then keep only non overlapping replacements
        valid.sort(Comparator.comparingInt((AhoCorasick.Match m) -> m.start)
                             .thenComparing(m -> -m.pattern.length()));

        List<AhoCorasick.Match> chosen = new ArrayList<>();
        int usedUpto = -1;
        for (AhoCorasick.Match m : valid) {
            if (m.start > usedUpto) {
                chosen.add(m);
                usedUpto = m.start + m.pattern.length() - 1;
            }
        }

        // apply right to left so the earlier indexes stay valid
        StringBuilder sb = new StringBuilder(query);
        List<String> changes = new ArrayList<>();
        for (int i = chosen.size() - 1; i >= 0; i--) {
            AhoCorasick.Match m = chosen.get(i);
            String replacement = canonical.get(m.pattern);
            if (replacement == null || replacement.equals(m.pattern)) continue;

            int end = m.start + m.pattern.length();
            if (end < sb.length() && sb.charAt(end) == 's') end++;      // eat the plural
            sb.replace(m.start, end, replacement);
            changes.add(m.pattern + " -> " + replacement);
        }

        if (changes.isEmpty()) return query;
        Trace.log("Aho-Corasick", "synonym rewrite " + changes);
        return sb.toString();
    }

    /**
     * "potash" maps to "muriate of potash", but the alias also sits INSIDE its own
     * replacement. Without this guard, typing "muriate of potash" would expand to
     * "muriate of muriate of potash". If the text already spells out the canonical
     * form around this match, we leave it alone.
     */
    private boolean alreadyCanonical(String lower, AhoCorasick.Match m) {
        String full = canonical.get(m.pattern);
        if (full == null || !full.contains(m.pattern)) return false;

        int offset = full.indexOf(m.pattern);
        int from = m.start - offset;
        int to = from + full.length();
        return from >= 0 && to <= lower.length() && lower.startsWith(full, from);
    }

    private static boolean endsAtWordBoundary(String s, int end) {
        if (end >= s.length()) return true;
        char c = s.charAt(end);
        if (!Character.isLetter(c)) return true;
        return c == 's' && (end + 1 >= s.length() || !Character.isLetter(s.charAt(end + 1)));
    }

    public int size() { return canonical.size(); }
}
