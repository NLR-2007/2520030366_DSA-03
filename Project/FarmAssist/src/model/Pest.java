package model;

import java.util.List;

/** One row of data/pests.csv - an insect or other pest that damages crops. */
public class Pest {
    public String name;
    public List<String> crops;      // crops attacked by this pest
    public List<String> damage;     // the damage symptoms it causes
    public String control;          // how to control it

    /** All damage phrases joined into one string - KMP searches inside this. */
    public String damageText() {
        return String.join(" , ", damage).toLowerCase();
    }

    public String pretty() {
        return "Pest      : " + name
             + "\nAttacks   : " + String.join(", ", crops)
             + "\nDamage    : " + String.join(", ", damage)
             + "\nControl   : " + control;
    }
}
