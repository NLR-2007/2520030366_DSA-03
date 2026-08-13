package model;

import java.util.List;

/** One row of data/diseases.csv */
public class Disease {
    public String name;
    public List<String> crops;      // crops attacked by this disease
    public List<String> symptoms;   // symptom phrases
    public String treatment;

    /** All symptoms joined into a single string - KMP searches inside this. */
    public String symptomText() {
        return String.join(" , ", symptoms).toLowerCase();
    }

    public String pretty() {
        return "Disease  : " + name
             + "\nAttacks  : " + String.join(", ", crops)
             + "\nSymptoms : " + String.join(", ", symptoms)
             + "\nTreatment: " + treatment;
    }
}
