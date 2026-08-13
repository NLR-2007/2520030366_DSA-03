package engine;

import algo.AhoCorasick;
import model.Crop;
import model.Disease;
import model.Fertilizer;
import model.Pest;
import util.Trace;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * STAGE 2 OF THE PIPELINE  ->  uses ALGORITHM 3 : AHO-CORASICK
 *
 * One single pass over the question detects every crop, disease, fertilizer
 * and symptom keyword that our database knows about.
 */
public class EntityExtractor {

    public static final String CROP = "CROP";
    public static final String DISEASE = "DISEASE";
    public static final String FERTILIZER = "FERTILIZER";
    public static final String SYMPTOM = "SYMPTOM";
    public static final String PEST = "PEST";

    private final AhoCorasick automaton = new AhoCorasick();
    private int patternCount = 0;

    public EntityExtractor(DataLoader data) {
        for (Crop c : data.crops)             { automaton.addPattern(c.name, CROP);       patternCount++; }
        for (Disease d : data.diseases)       { automaton.addPattern(d.name, DISEASE);    patternCount++; }
        for (Fertilizer f : data.fertilizers) { automaton.addPattern(f.name, FERTILIZER); patternCount++; }
        for (Pest p : data.pests)             { automaton.addPattern(p.name, PEST);       patternCount++; }
        for (String s : data.symptomKeywords) { automaton.addPattern(s, SYMPTOM);         patternCount++; }
        automaton.build();
    }

    /** What the query talks about. */
    public static class Entities {
        public Set<String> crops = new LinkedHashSet<>();
        public Set<String> diseases = new LinkedHashSet<>();
        public Set<String> fertilizers = new LinkedHashSet<>();
        public Set<String> symptoms = new LinkedHashSet<>();
        public Set<String> pests = new LinkedHashSet<>();

        public boolean isEmpty() {
            return crops.isEmpty() && diseases.isEmpty()
                && fertilizers.isEmpty() && symptoms.isEmpty() && pests.isEmpty();
        }

        /** Everything we detected, used later as search terms. */
        public List<String> allTerms() {
            List<String> t = new ArrayList<>();
            t.addAll(diseases); t.addAll(crops); t.addAll(fertilizers); t.addAll(symptoms);
            t.addAll(pests);
            return t;
        }
    }

    public Entities extract(String query) {
        Entities e = new Entities();

        List<AhoCorasick.Match> matches = automaton.search(query);   // ONE pass
        for (AhoCorasick.Match m : matches) {
            switch (m.category) {
                case CROP:       e.crops.add(m.pattern); break;
                case DISEASE:    e.diseases.add(m.pattern); break;
                case FERTILIZER: e.fertilizers.add(m.pattern); break;
                case SYMPTOM:    e.symptoms.add(m.pattern); break;
                case PEST:       e.pests.add(m.pattern); break;
            }
        }

        Trace.log("Aho-Corasick", "one pass over " + patternCount + " patterns, "
                + matches.size() + " hits");
        if (!e.isEmpty()) {
            Trace.log("Aho-Corasick", "crops=" + e.crops + " diseases=" + e.diseases
                    + " fertilizers=" + e.fertilizers + " symptoms=" + e.symptoms
                    + " pests=" + e.pests);
        }
        return e;
    }

    public int patternCount() { return patternCount; }
}
