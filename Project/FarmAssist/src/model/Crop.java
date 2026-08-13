package model;

import java.util.List;

/** One row of data/crops.csv */
public class Crop {
    public String name;
    public String season;
    public String soil;
    public String waterNeed;
    public int n, p, k;                 // nutrient requirement in kg per hectare
    public List<String> diseases;       // diseases that commonly attack this crop
    public String description;
    // richer agronomic facts (columns 10-15 of data/crops.csv)
    public String duration;             // sowing to harvest, e.g. "110-145 days"
    public String temperature;          // optimum range in Celsius, e.g. "20-35 C"
    public String rainfall;             // suitable annual range, e.g. "1000-1500 mm"
    public String spacing;              // planting geometry, e.g. "20x15 cm"
    public String varieties;            // recommended cultivars, comma separated
    public String yield;                // typical range, e.g. "45-60 q/ha"

    public String searchText() {
        return (name + " " + season + " " + soil + " " + description
                + " " + duration + " " + temperature + " " + rainfall
                + " " + spacing + " " + varieties + " " + yield).toLowerCase();
    }

    public String pretty() {
        return "Crop       : " + name
             + "\nSeason     : " + season
             + "\nSoil       : " + soil
             + "\nWater need : " + waterNeed
             + "\nDuration   : " + duration
             + "\nTemperature: " + temperature
             + "\nRainfall   : " + rainfall
             + "\nSpacing    : " + spacing
             + "\nVarieties  : " + varieties
             + "\nYield      : " + yield
             + "\nNutrients  : N=" + n + "  P=" + p + "  K=" + k + " kg/hectare"
             + "\nDiseases   : " + String.join(", ", diseases)
             + "\nAbout      : " + description;
    }
}
