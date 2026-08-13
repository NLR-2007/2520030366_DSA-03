package engine;

import model.Crop;
import util.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CROP PLANNER  ->  no new algorithm, it reuses the crop facts
 *
 * Filters the whole crop table by the rainfall, temperature and water need that
 * the farmer mentions, so questions like
 *
 *     "which crops suit low rainfall"
 *     "best crops for a hot climate with 800 mm rain"
 *
 * get a real list of crops instead of a generic search. Each crop is scored by
 * how many of the stated conditions it satisfies, and the best matches win.
 */
public class Planner {

    private final DataLoader data;

    public Planner(DataLoader data) { this.data = data; }

    public static class Plan {
        public List<Crop> crops = new ArrayList<>();   // best matches first
        public List<String> notes = new ArrayList<>(); // what we matched on
    }

    public Plan planFor(String query) {
        Plan plan = new Plan();
        String q = query.toLowerCase();

        Integer rain = extractRainfall(q);       // explicit "800 mm"
        Integer temp = extractTemperature(q);    // explicit "25 c" / "25 degree"
        String water = extractWater(q);          // "low water" -> "Low"

        boolean wantLowRain  = rain == null && hasAny(q, "low rainfall", "less rain",
                "low rain", "dry", "arid", "drought", "little water", "rainfed");
        boolean wantHighRain = rain == null && hasAny(q, "high rainfall", "heavy rain",
                "lots of rain", "humid", "wet");
        boolean wantCool     = temp == null && hasAny(q, "cool", "cold", "temperate", "hill");
        boolean wantHot      = temp == null && hasAny(q, "hot", "warm", "heat");

        if (rain != null)   plan.notes.add("rainfall " + rain + " mm");
        if (temp != null)   plan.notes.add("temperature " + temp + " C");
        if (water != null)  plan.notes.add(water.toLowerCase() + " water need");
        if (wantLowRain)    plan.notes.add("low rainfall");
        if (wantHighRain)   plan.notes.add("high rainfall");
        if (wantCool)       plan.notes.add("cool climate");
        if (wantHot)        plan.notes.add("hot climate");

        List<Scored> scored = new ArrayList<>();
        for (Crop c : data.crops) {
            int score = 0;

            if (rain != null && rainFits(c.rainfall, rain))                        score += 2;
            if (temp != null && tempFits(c.temperature, temp))                     score += 2;
            if (water != null && c.waterNeed.equalsIgnoreCase(water))              score += 2;
            if (wantLowRain  && rainMax(c.rainfall) <= 700)                        score++;
            if (wantHighRain && rainMin(c.rainfall) >= 1000)                       score++;
            if (wantCool     && tempMax(c.temperature) <= 22)                      score++;
            if (wantHot      && tempMin(c.temperature) >= 24)                      score++;
            if (wantLowRain  && c.waterNeed.equalsIgnoreCase("Low"))               score++;
            if (wantHighRain && c.waterNeed.equalsIgnoreCase("High"))              score++;

            if (score > 0) scored.add(new Scored(c, score));
        }

        // best matches first, then alphabetical
        scored.sort(Comparator.comparingInt((Scored s) -> -s.score)
                              .thenComparing(s -> s.crop.name));
        for (Scored s : scored) plan.crops.add(s.crop);

        Trace.log("Crop planner", "matched " + scored.size() + " of "
                + data.crops.size() + " crops against " + plan.notes);
        return plan;
    }

    private static class Scored {
        Crop crop; int score;
        Scored(Crop c, int s) { crop = c; score = s; }
    }

    // ---------------------------------------------------------- parsing

    static Integer extractRainfall(String q) {
        Matcher m = Pattern.compile("(\\d{3,4})\\s*(?:mm|millimetre|millimeter)").matcher(q);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    static Integer extractTemperature(String q) {
        Matcher m = Pattern.compile("(\\d{1,2})\\s*(?:degrees?|deg|\\bc\\b)").matcher(q);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    static String extractWater(String q) {
        if (hasAny(q, "low water", "less water", "little water", "low irrigation")) return "Low";
        if (hasAny(q, "high water", "heavy irrigation", "lots of water", "flood"))  return "High";
        if (hasAny(q, "medium water", "moderate water"))                            return "Medium";
        return null;
    }

    // ------------------------------------------------ range checks

    /** "1000-1500 mm" -> true when rain sits inside, with a little slack. */
    static boolean rainFits(String range, int rain) {
        int[] lo = range(range);
        return lo == null || (rain >= lo[0] - 200 && rain <= lo[1] + 200);
    }

    /** "20-35 C" -> true when temp sits inside, with a little slack. */
    static boolean tempFits(String range, int temp) {
        int[] lo = range(range);
        return lo == null || (temp >= lo[0] - 2 && temp <= lo[1] + 2);
    }

    static int rainMin(String range) { int[] r = range(range); return r == null ? Integer.MAX_VALUE : r[0]; }
    static int rainMax(String range) { int[] r = range(range); return r == null ? Integer.MIN_VALUE : r[1]; }
    static int tempMin(String range) { int[] r = range(range); return r == null ? Integer.MAX_VALUE : r[0]; }
    static int tempMax(String range) { int[] r = range(range); return r == null ? Integer.MIN_VALUE : r[1]; }

    /** The first "a-b" pair inside a field like "1000-1500 mm" or "20-35 C". */
    private static int[] range(String field) {
        Matcher m = Pattern.compile("(\\d+)\\s*-\\s*(\\d+)").matcher(field);
        return m.find()
             ? new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))}
             : null;
    }

    private static boolean hasAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }
}
