package model;

import java.util.List;

/** One row of data/fertilizers.csv */
public class Fertilizer {
    public String name;
    public int n, p, k;             // percentage of each nutrient
    public int cost;                // rupees per bag  -> the WEIGHT in the knapsack
    public List<String> suited;     // crops it suits, or the single word "all"
    public int benefit;             // 0-100 usefulness score -> the VALUE in the knapsack

    public boolean suitsCrop(String crop) {
        if (suited.contains("all")) return true;
        for (String c : suited) if (c.equalsIgnoreCase(crop)) return true;
        return false;
    }

    public String pretty() {
        return "Fertilizer: " + name
             + "\nNPK       : " + n + "-" + p + "-" + k
             + "\nCost      : Rs " + cost + " per bag"
             + "\nSuited for: " + String.join(", ", suited)
             + "\nBenefit   : " + benefit + "/100";
    }
}
