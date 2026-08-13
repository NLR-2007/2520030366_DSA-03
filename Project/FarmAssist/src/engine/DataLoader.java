package engine;

import model.Article;
import model.Crop;
import model.Disease;
import model.Fertilizer;
import model.Pest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads the pipe separated files in the data folder into memory at startup. */
public class DataLoader {

    public List<Crop> crops = new ArrayList<>();
    public List<Disease> diseases = new ArrayList<>();
    public List<Fertilizer> fertilizers = new ArrayList<>();
    public List<Article> articles = new ArrayList<>();
    public List<Pest> pests = new ArrayList<>();
    public List<String> symptomKeywords = new ArrayList<>();
    public List<String[]> synonyms = new ArrayList<>();    // {alias, canonical}
    public List<String[]> smallTalk = new ArrayList<>();   // {triggers, response}

    public void loadAll(String dataFolder) {
        loadCrops(dataFolder + "/crops.csv");
        loadDiseases(dataFolder + "/diseases.csv");
        loadFertilizers(dataFolder + "/fertilizers.csv");
        loadArticles(dataFolder + "/articles.csv");
        loadPests(dataFolder + "/pests.csv");
        loadSymptoms(dataFolder + "/symptoms.csv");
        synonyms = readRows(dataFolder + "/synonyms.csv", 2);
        smallTalk = readRows(dataFolder + "/smalltalk.csv", 2);
    }

    // ------------------------------------------------------------------ files

    private void loadCrops(String path) {
        for (String[] f : readRows(path, 15)) {
            Crop c = new Crop();
            c.name = f[0].trim().toLowerCase();
            c.season = f[1].trim();
            c.soil = f[2].trim();
            c.waterNeed = f[3].trim();
            c.n = toInt(f[4]); c.p = toInt(f[5]); c.k = toInt(f[6]);
            c.diseases = splitList(f[7]);
            c.description = f[8].trim();
            c.duration = f[9].trim();
            c.temperature = f[10].trim();
            c.rainfall = f[11].trim();
            c.spacing = f[12].trim();
            c.varieties = f[13].trim();
            c.yield = f[14].trim();
            crops.add(c);
        }
    }

    private void loadDiseases(String path) {
        for (String[] f : readRows(path, 4)) {
            Disease d = new Disease();
            d.name = f[0].trim().toLowerCase();
            d.crops = splitList(f[1]);
            d.symptoms = splitList(f[2]);
            d.treatment = f[3].trim();
            diseases.add(d);
        }
    }

    private void loadFertilizers(String path) {
        for (String[] f : readRows(path, 7)) {
            Fertilizer x = new Fertilizer();
            x.name = f[0].trim().toLowerCase();
            x.n = toInt(f[1]); x.p = toInt(f[2]); x.k = toInt(f[3]);
            x.cost = toInt(f[4]);
            x.suited = splitList(f[5]);
            x.benefit = toInt(f[6]);
            fertilizers.add(x);
        }
    }

    private void loadArticles(String path) {
        for (String[] f : readRows(path, 3)) {
            Article a = new Article();
            a.id = f[0].trim();
            a.title = f[1].trim();
            a.body = f[2].trim();
            articles.add(a);
        }
    }

    private void loadPests(String path) {
        for (String[] f : readRows(path, 4)) {
            Pest p = new Pest();
            p.name = f[0].trim().toLowerCase();
            p.crops = splitList(f[1]);
            p.damage = splitList(f[2]);
            p.control = f[3].trim();
            pests.add(p);
        }
    }

    private void loadSymptoms(String path) {
        for (String line : readLines(path)) symptomKeywords.add(line.trim().toLowerCase());
    }

    // ----------------------------------------------------------------- helpers

    private List<String[]> readRows(String path, int expectedFields) {
        List<String[]> rows = new ArrayList<>();
        for (String line : readLines(path)) {
            String[] parts = line.split("\\|", -1);
            if (parts.length >= expectedFields) rows.add(parts);
            else System.out.println("   [warn] skipping bad row in " + path + " -> " + line);
        }
        return rows;
    }

    private List<String> readLines(String path) {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;   // comments
                out.add(line);
            }
        } catch (Exception e) {
            System.out.println("   [error] cannot read " + path + " : " + e.getMessage());
        }
        return out;
    }

    private static List<String> splitList(String field) {
        List<String> out = new ArrayList<>();
        for (String s : field.split(",")) {
            String t = s.trim().toLowerCase();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static int toInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    public String summary() {
        return crops.size() + " crops, " + diseases.size() + " diseases, "
             + pests.size() + " pests, " + fertilizers.size() + " fertilizers, "
             + articles.size() + " articles";
    }

    public Crop findCrop(String name) {
        for (Crop c : crops) if (c.name.equalsIgnoreCase(name)) return c;
        return null;
    }

    public Pest findPest(String name) {
        for (Pest p : pests) if (p.name.equalsIgnoreCase(name)) return p;
        return null;
    }

    public Fertilizer findFertilizer(String name) {
        for (Fertilizer f : fertilizers) if (f.name.equalsIgnoreCase(name)) return f;
        return null;
    }

    public Disease findDisease(String name) {
        for (Disease d : diseases) if (d.name.equalsIgnoreCase(name)) return d;
        return null;
    }

    public List<String> allCropNames() {
        List<String> names = new ArrayList<>();
        for (Crop c : crops) names.add(c.name);
        return names;
    }

    /** Extra everyday words the spell checker should treat as correct. */
    public static List<String> commonWords() {
        return Arrays.asList(
            "fertilizer", "fertiliser", "disease", "symptom", "symptoms", "treatment",
            "budget", "cost", "price", "rupees", "under", "within", "best", "suggest",
            "recommend", "which", "what", "when", "where", "how", "grow", "growing",
            "cultivation", "season", "soil", "water", "irrigation", "spray", "control",
            "crop", "crops", "leaf", "leaves", "plant", "plants", "field", "yield",
            "manure", "compost", "organic", "nitrogen", "phosphorus", "potassium",
            "about", "info", "information", "care", "give", "need", "help", "problem",
            "attack", "infected", "damage", "related", "article", "articles", "match",
            "assign", "allocate", "distribute", "have", "with", "that", "this", "please",
            // ordinary English, so normal words are never "corrected" into crop names
            "strange", "marks", "mark", "small", "large", "little", "many", "much",
            "some", "other", "another", "good", "better", "best", "bad", "worse",
            "there", "their", "they", "them", "then", "than", "from", "into", "over",
            "under", "after", "before", "should", "would", "could", "will", "want",
            "tell", "show", "find", "know", "look", "looks", "looking", "seeing",
            "getting", "become", "becoming", "turning", "turned", "start", "started",
            "stop", "keep", "make", "made", "used", "using", "very", "more", "most",
            "less", "least", "time", "times", "days", "week", "month", "year", "years",
            "morning", "evening", "night", "today", "tomorrow", "yesterday",
            "money", "rupees", "cheap", "costly", "market", "price", "sell", "buy",
            "farm", "farmer", "farming", "garden", "acre", "hectare", "bigha",
            "rain", "rainfall", "weather", "summer", "winter", "monsoon", "drought",
            "flood", "wind", "heat", "cold", "temperature", "humidity"
        );
    }
}
