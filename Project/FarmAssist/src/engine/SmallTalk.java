package engine;

import algo.EditDistance;
import util.Trace;

import java.util.ArrayList;
import java.util.List;

/**
 * EVERYDAY CONVERSATION LAYER  ->  uses ALGORITHM 4 : EDIT DISTANCE
 *
 * Runs before the farming pipeline. If the farmer just says "hi" or asks
 * "what can you do", we answer like a chat bot instead of searching documents.
 * Edit Distance lets "helo" and "thnks" still work.
 */
public class SmallTalk {

    private final List<String[]> entries = new ArrayList<>();   // {triggers, response}

    public SmallTalk(DataLoader data) {
        entries.addAll(data.smallTalk);
    }

    /** @return the reply, or null when this is a real farming question. */
    public String reply(String rawQuery) {
        String q = normalise(rawQuery);
        if (q.isEmpty()) return null;
        int words = q.split(" ").length;

        // 1. the whole question IS the greeting  ->  "hi"
        for (String[] e : entries) {
            for (String trigger : e[0].split(",")) {
                if (q.equals(trigger.trim())) return hit(e[1], trigger.trim(), "exact");
            }
        }

        // 2. a short question CONTAINS the greeting  ->  "ok thank you"
        if (words <= 5) {
            for (String[] e : entries) {
                for (String trigger : e[0].split(",")) {
                    String t = trigger.trim();
                    if (containsWord(q, t)) return hit(e[1], t, "contains");
                }
            }
        }

        // 3. a short question is a TYPO of a greeting  ->  "helo", "thnks"
        if (words <= 3) {
            for (String[] e : entries) {
                for (String trigger : e[0].split(",")) {
                    String t = trigger.trim();
                    if (Math.abs(t.length() - q.length()) > 2) continue;
                    int d = EditDistance.distance(q, t);
                    if (d <= (t.length() <= 4 ? 1 : 2)) return hit(e[1], t, "edit distance " + d);
                }
            }
        }
        return null;
    }

    private String hit(String response, String trigger, String how) {
        Trace.log("Small talk", "matched the everyday phrase \"" + trigger + "\" (" + how + ")");
        return response;
    }

    private static boolean containsWord(String haystack, String needle) {
        return (" " + haystack + " ").contains(" " + needle + " ");
    }

    /** letters and spaces only, single spaced, lower case. */
    private static String normalise(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toLowerCase().toCharArray()) {
            sb.append(Character.isLetter(c) ? c : ' ');
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    /** Trigger words are added to the spell dictionary so they are never "corrected". */
    public List<String> triggerWords() {
        List<String> out = new ArrayList<>();
        for (String[] e : entries) {
            for (String trigger : e[0].split(",")) out.add(trigger.trim());
        }
        return out;
    }
}
