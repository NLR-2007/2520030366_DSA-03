package engine;

import algo.KMP;
import algo.RandomizedQuickSort;
import model.Disease;
import model.Pest;
import util.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * DISEASE DIAGNOSIS FEATURE
 *   ALGORITHM 1 : KMP              -> match every detected symptom keyword
 *                                     against every disease symptom list
 *   ALGORITHM 8 : RANDOMIZED QSORT -> rank the possible diseases
 *
 * The symptoms themselves were detected earlier by Aho-Corasick.
 */
public class Diagnoser {

    private final DataLoader data;

    public Diagnoser(DataLoader data) { this.data = data; }

    public static class Suspect {
        public Disease disease;
        public int score;
        public List<String> matchedSymptoms = new ArrayList<>();
        public boolean cropMatches;
    }

    /** One candidate pest, scored exactly like a disease suspect. */
    public static class PestSuspect {
        public Pest pest;
        public int score;
        public List<String> matchedDamage = new ArrayList<>();
        public boolean cropMatches;
    }

    /**
     * @param symptoms symptom keywords found in the question
     * @param crops    crops found in the question
     */
    public List<Suspect> diagnose(Set<String> symptoms, Set<String> crops, Set<String> namedDiseases) {
        List<Suspect> suspects = new ArrayList<>();
        int kmpCalls = 0;

        // If the farmer named a crop, only diseases that actually attack that crop
        // can be the answer. Without this filter a rice disease can win a tomato
        // question just because both show "brown spots".
        List<Disease> candidates = filterByCrop(crops, namedDiseases);

        for (Disease d : candidates) {
            Suspect s = new Suspect();
            s.disease = d;
            String symptomText = d.symptomText();

            // ---------- ALGORITHM 1 : KMP ----------
            for (String keyword : symptoms) {
                kmpCalls++;
                if (KMP.contains(symptomText, keyword)) {     // exact substring search
                    s.matchedSymptoms.add(keyword);
                    s.score += 4;
                }
            }

            // the farmer named the crop and this disease attacks it
            for (String crop : crops) {
                if (d.crops.contains(crop)) { s.score += 6; s.cropMatches = true; }
            }

            // the farmer named the disease directly
            if (namedDiseases.contains(d.name)) s.score += 10;

            if (s.score > 0) suspects.add(s);
        }

        Trace.log("KMP", kmpCalls + " exact symptom searches over "
                + candidates.size() + " disease records");

        // ---------- ALGORITHM 8 : RANDOMIZED QUICKSORT ----------
        RandomizedQuickSort.sort(suspects, Comparator.comparingInt((Suspect s) -> -s.score));
        Trace.log("Random QuickSort", "ranked " + suspects.size() + " possible diseases");

        return suspects;
    }

    /**
     * The same diagnosis over the pest table instead of the disease table.
     * Damage phrases are searched with KMP exactly like symptom lists are.
     */
    public List<PestSuspect> diagnosePests(Set<String> symptoms, Set<String> crops,
                                           Set<String> namedPests) {
        List<PestSuspect> suspects = new ArrayList<>();
        int kmpCalls = 0;

        // if the farmer named a crop, only pests that attack it can be the answer
        List<Pest> candidates = filterPestsByCrop(crops, namedPests);

        for (Pest p : candidates) {
            PestSuspect s = new PestSuspect();
            s.pest = p;
            String damageText = p.damageText();

            // ---------- ALGORITHM 1 : KMP ----------
            for (String keyword : symptoms) {
                kmpCalls++;
                if (KMP.contains(damageText, keyword)) {     // exact substring search
                    s.matchedDamage.add(keyword);
                    s.score += 4;
                }
            }

            // the farmer named the crop and this pest attacks it
            for (String crop : crops) {
                if (p.crops.contains(crop)) { s.score += 6; s.cropMatches = true; }
            }

            // the farmer named the pest directly
            if (namedPests.contains(p.name)) s.score += 10;

            if (s.score > 0) suspects.add(s);
        }

        Trace.log("KMP", kmpCalls + " exact damage searches over "
                + candidates.size() + " pest records");

        // ---------- ALGORITHM 8 : RANDOMIZED QUICKSORT ----------
        RandomizedQuickSort.sort(suspects, Comparator.comparingInt((PestSuspect s) -> -s.score));
        Trace.log("Random QuickSort", "ranked " + suspects.size() + " possible pests");

        return suspects;
    }

    /** Only the pests that attack one of the crops the farmer mentioned. */
    private List<Pest> filterPestsByCrop(Set<String> crops, Set<String> namedPests) {
        if (crops.isEmpty()) return data.pests;

        List<Pest> filtered = new ArrayList<>();
        for (Pest p : data.pests) {
            boolean attacksOurCrop = false;
            for (String crop : crops) if (p.crops.contains(crop)) attacksOurCrop = true;

            // a pest the farmer named by its own name is always kept
            if (attacksOurCrop || namedPests.contains(p.name)) filtered.add(p);
        }

        if (filtered.isEmpty()) return data.pests;

        Trace.log("Crop filter", "kept " + filtered.size() + " of " + data.pests.size()
                + " pests that actually attack " + crops);
        return filtered;
    }

    /**
     * Keep only the diseases that attack one of the crops the farmer mentioned.
     * If the farmer named no crop, or none of our diseases attack it, we search
     * the whole disease list instead.
     */
    private List<Disease> filterByCrop(Set<String> crops, Set<String> namedDiseases) {
        if (crops.isEmpty()) return data.diseases;

        List<Disease> filtered = new ArrayList<>();
        for (Disease d : data.diseases) {
            boolean attacksOurCrop = false;
            for (String crop : crops) if (d.crops.contains(crop)) attacksOurCrop = true;

            // a disease the farmer named by its own name is always kept
            if (attacksOurCrop || namedDiseases.contains(d.name)) filtered.add(d);
        }

        if (filtered.isEmpty()) return data.diseases;

        Trace.log("Crop filter", "kept " + filtered.size() + " of " + data.diseases.size()
                + " diseases that actually attack " + crops);
        return filtered;
    }
}
