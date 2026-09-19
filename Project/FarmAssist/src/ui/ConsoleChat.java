package ui;

import engine.AutocompleteEngine;
import engine.DataLoader;
import engine.Diagnoser;
import engine.EntityExtractor;
import engine.IntentDetector;
import engine.Planner;
import engine.Recommender;
import engine.SearchEngine;
import engine.SearchResult;
import engine.SmallTalk;
import engine.SpellCorrector;
import engine.SynonymMapper;
import engine.WeatherService;
import model.Crop;
import model.Fertilizer;
import model.Pest;
import util.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * ==========================================================================
 * SOPHIE - the FarmAssist console assistant
 * ==========================================================================
 * THE PIPELINE THAT EVERY QUESTION GOES THROUGH
 *
 *   question
 *     |--> 0. Small talk         : greetings, thanks, "what can you do"
 *     |--> 1. Edit Distance      : fix the spelling
 *     |--> 1b. word splitter     : "lateblight" -> "late blight"
 *     |--> 1c. Aho-Corasick      : synonyms, "paddy" -> "rice"
 *     |--> 2. Aho-Corasick       : detect crops / diseases / fertilizers / symptoms
 *     |--> 3. Intent routing     : decide which feature answers
 *     |
 *     +--> FERTILIZER_BUDGET     : 6. Knapsack
 *     +--> CROP_FERTILIZER_MATCH : 7. Bipartite Matching
 *     +--> DISEASE_DIAGNOSIS     : 1. KMP  + 8. QuickSort
 *     +--> CROP / FERT INFO      : direct record lookup
 *     +--> GENERAL_SEARCH        : 2. Rabin-Karp + 1. KMP + 8. QuickSort
 *                                  then 5. Suffix Array + LCP for related reading
 *
 * All drawing goes through Theme, which owns the grid, the palette and the
 * glyphs, so every screen lines up on the same 78 columns.
 * ==========================================================================
 */
public class ConsoleChat {

    /** Indent of ordinary body text under a section heading. */
    private static final String PAD = "  ";
    /** Indent of a detail line inside a card, and the label column width. */
    private static final String DETAIL = "     ";
    private static final int LABEL_W = 12;

    private DataLoader data;
    private SmallTalk smallTalk;
    private SynonymMapper synonymMapper;
    private SpellCorrector spellCorrector;
    private EntityExtractor entityExtractor;
    private IntentDetector intentDetector;
    private SearchEngine searchEngine;
    private Diagnoser diagnoser;
    private Recommender recommender;
    private Planner planner;
    private AutocompleteEngine autocompleteEngine;
    private WeatherService weather;

    private List<String> currentSuggestions = new ArrayList<>();

    public static void main(String[] args) {
        String dataFolder = (args.length > 0) ? args[0] : "data";
        new ConsoleChat().start(dataFolder);
    }

    public void start(String dataFolder) {
        Centering.apply();                 // sit the whole chat in the middle of the window
        Trace.lineCounter = Centering::lineCount;   // let the trace space itself
        Trace.atBlankLine = Centering::lastLineWasBlank;
        System.out.println();
        System.out.println(Theme.banner());

        data = new DataLoader();
        data.loadAll(dataFolder);
        if (data.articles.isEmpty()) {
            System.out.println();
            System.out.println(PAD + Theme.alert(Theme.I_WARN + "  no data was loaded - the fields are empty."));
            System.out.println(PAD + Theme.stone("run the program from the FarmAssist folder, or pass the"));
            System.out.println(PAD + Theme.stone("data folder path as an argument.") + "\n");
            return;
        }

        smallTalk          = new SmallTalk(data);
        synonymMapper      = new SynonymMapper(data);
        spellCorrector     = new SpellCorrector(data, smallTalk.triggerWords());
        entityExtractor    = new EntityExtractor(data);
        intentDetector     = new IntentDetector();
        searchEngine       = new SearchEngine(data);
        diagnoser          = new Diagnoser(data);
        recommender        = new Recommender(data);
        planner            = new Planner(data);
        autocompleteEngine = new AutocompleteEngine(data);
        weather            = new WeatherService(dataFolder);

        System.out.println(PAD + Theme.stone("type ") + Theme.crop("help") + Theme.stone(" for examples and commands."));

        LiveInput input = new LiveInput(q -> autocompleteEngine.complete(q, 6));
        while (true) {
            blankLine();
            String line = input.readLine();
            if (line == null) break;
            if (line.isEmpty()) continue;

            if (line.matches("\\d+") && !currentSuggestions.isEmpty()) {
                int index = Integer.parseInt(line) - 1;
                if (index >= 0 && index < currentSuggestions.size()) {
                    String selected = currentSuggestions.get(index);
                    System.out.println();
                    say("Selected [" + line + "]: " + Theme.sun("\"" + selected + "\""));
                    line = selected;
                }
            }

            if (isCommand(line)) {
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println();
                    say("May your fields stay green. Goodbye.");
                    System.out.println("\n" + Theme.rule() + "\n");
                    break;
                }
                continue;
            }
            answer(line);
        }
        input.close();
    }

    /** The "what is loaded" summary printed at start up. */
    private void printKnowledgeBase() {
        System.out.println();
        System.out.println(Theme.section("knowledge base"));
        System.out.println();
        stat(Theme.I_CROP,    "Crops",       data.crops.size(),
             "search index",     searchEngine.indexSize() + " documents");
        stat(Theme.I_DISEASE, "Diseases",    data.diseases.size(),
             "spell dictionary", spellCorrector.dictionarySize() + " words");
        stat(Theme.I_BUG,     "Pests",       data.pests.size(),
             "pattern trie",     entityExtractor.patternCount() + " names");
        stat(Theme.I_FERT,    "Fertilizers", data.fertilizers.size(),
             "local names",      synonymMapper.size() + " synonyms");
        stat(Theme.I_DOC,     "Articles",    data.articles.size(),
             "crop facts",       data.crops.size() + " profiles");
    }

    /** One line of the start up summary: a count on the left, an index on the right. */
    private void stat(String icon, String label, int count, String rightLabel, String rightValue) {
        String left = PAD + Theme.leaf(icon) + "  " + Theme.crop(Theme.padRight(label, 13))
                    + Theme.bold(Theme.chalk(Theme.padLeft(String.valueOf(count), 4)));
        String right = Theme.stone(Theme.padRight(rightLabel, 18)) + Theme.water(rightValue);
        System.out.println(Theme.padRight(left, 32) + right);
    }

    // ====================================================================
    // MAIN PIPELINE
    // ====================================================================

    private void answer(String rawQuery) {
        System.out.println();

        // ---- STAGE 0 : EVERYDAY CONVERSATION ----------------------------
        String chat = smallTalk.reply(rawQuery);
        if (chat != null) {
            System.out.println();
            say(chat);
            return;
        }

        // ---- STAGE 0.2 : LIVE WEATHER -----------------------------------
        // before spell correction, because a town name is not in the
        // dictionary and would be "corrected" into a crop.
        if (isWeatherQuestion(rawQuery)) {
            showWeather(extractCity(rawQuery));
            return;
        }

        // ---- STAGE 0.5 : TRIE NEXT-WORD PREDICTION ----------------------
        String[] phraseWords = rawQuery.trim().split("\\s+");
        if (phraseWords.length == 1 && autocompleteEngine != null) {
            List<String> predicted = autocompleteEngine.predict(rawQuery, 5);
            if (!predicted.isEmpty()) {
                System.out.println();
                say("Next-word predictions for \"" + Theme.sun(rawQuery) + "\":");
                showSuggestions(predicted);
                return;
            }
        }

        // ---- STAGE 1 : EDIT DISTANCE + WORD SPLITTER --------------------
        SpellCorrector.Result sp = spellCorrector.correct(rawQuery);
        String query = sp.correctedQuery;
        if (!sp.corrections.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (SpellCorrector.Correction c : sp.corrections) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.wrong).append(" ").append(Theme.I_ARROW).append(" ").append(c.right);
            }
            System.out.println();
            say("I adjusted your words: " + Theme.sun(sb.toString()));
            note("reading it as \"" + query + "\"");
        }

        // ---- STAGE 1c : SYNONYMS ("paddy" means "rice") ------------------
        String mapped = synonymMapper.rewrite(query);
        if (!mapped.equalsIgnoreCase(query)) {
            System.out.println();
            say("I understood that as " + Theme.sun("\"" + mapped + "\""));
            query = mapped;
        }

        // ---- STAGE 2 : AHO-CORASICK -------------------------------------
        EntityExtractor.Entities entities = entityExtractor.extract(query);

        // ---- STAGE 3 : ROUTE --------------------------------------------
        IntentDetector.Intent intent = intentDetector.detect(query, entities);
        System.out.println();

        switch (intent) {
            case FERTILIZER_BUDGET:     answerBudget(query, entities);      break;
            case CROP_FERTILIZER_MATCH: answerMatching(entities);           break;
            case CROP_COMPARE:          answerCompare(entities);            break;
            case CROP_PLANNER:          answerPlanner(query, entities);     break;
            case DISEASE_DIAGNOSIS:     answerDiagnosis(query, entities);   break;
            case CROP_INFO:             answerCropInfo(query, entities);    break;
            case FERTILIZER_INFO:       answerFertilizerInfo(entities);     break;
            case PEST_INFO:             answerPestInfo(entities);           break;
            default:                    answerSearch(query, entities);      break;
        }
    }

    // ---------------- FEATURE : budget  ->  KNAPSACK ---------------------

    private void answerBudget(String query, EntityExtractor.Entities e) {
        int budget = IntentDetector.extractBudget(query);
        String crop = e.crops.isEmpty() ? null : e.crops.iterator().next();

        if (budget <= 0) {
            say("Tell me your budget in rupees, for example "
                + Theme.sun("suggest fertilizer for tomato under 3000") + ".");
            return;
        }

        Recommender.BudgetPlan plan = recommender.planWithinBudget(crop, budget);

        say("The best basket" + (crop != null ? " for " + Theme.sun(crop) : "")
            + " within " + Theme.sun("Rs " + budget) + ":");

        if (plan.chosen.isEmpty()) {
            System.out.println();
            System.out.println(PAD + Theme.alert("Nothing fits in this budget.")
                    + Theme.stone("  The cheapest bag costs Rs " + cheapest(plan.considered) + "."));
            return;
        }

        System.out.println();
        Theme.Table t = Theme.Table.of(Theme.I_FERT, "fertilizer plan")
                .col("fertilizer", 0,  false, Theme::crop)
                .col("npk",        10, false, Theme::stone)
                .col("cost",       10, true,  Theme::soil)
                .col("benefit",     9, true,  Theme::leaf);

        for (Fertilizer f : plan.chosen) {
            t.row(Theme.title(f.name), f.n + "-" + f.p + "-" + f.k,
                  "Rs " + f.cost, f.benefit + "/100");
        }
        t.total("TOTAL", "", "Rs " + plan.totalCost, String.valueOf(plan.totalBenefit));
        t.print();

        System.out.println();
        System.out.println(PAD + Theme.stone("money left  ")
                + Theme.bold(Theme.sun("Rs " + (budget - plan.totalCost))));
        System.out.println(PAD + Theme.ash(Theme.I_DOT + "  ") + Theme.stone(Theme.wrap(
                "chosen by 0/1 Knapsack, the highest total benefit that fits the budget",
                Theme.WIDTH - PAD.length() - 4, PAD + "   ")));
    }

    private int cheapest(List<Fertilizer> list) {
        int min = Integer.MAX_VALUE;
        for (Fertilizer f : list) min = Math.min(min, f.cost);
        return min == Integer.MAX_VALUE ? 0 : min;
    }

    // ------------- FEATURE : many crops  ->  BIPARTITE MATCHING ----------

    private void answerMatching(EntityExtractor.Entities e) {
        List<String> crops = new ArrayList<>(e.crops);
        Recommender.MatchPlan plan = recommender.matchCropsToFertilizers(crops);

        say("One bag for each field, and no bag given twice:");
        System.out.println();

        Theme.Table t = Theme.Table.of(Theme.I_LINK, "field by field plan")
                .col("crop",       20, false, Theme::chalk)
                .col("fertilizer",  0, false, Theme::crop)
                .col("npk",        10, false, Theme::stone);

        for (int i = 0; i < plan.crops.size(); i++) {
            int f = plan.assignment[i];
            if (f >= 0) {
                Fertilizer fert = plan.pool.get(f);
                t.row(Theme.title(plan.crops.get(i)), Theme.title(fert.name),
                      fert.n + "-" + fert.p + "-" + fert.k);
            } else {
                t.row(Theme.title(plan.crops.get(i)), "no free suitable bag", "");
            }
        }
        t.print();

        System.out.println();
        System.out.println(PAD + Theme.leaf(Theme.I_OK) + "  "
                + Theme.chalk("served " + plan.matchedCount + " of " + plan.crops.size() + " fields")
                + Theme.stone("   " + Theme.I_DOT + "  maximum bipartite matching"));
    }

    // ----------------- FEATURE : diagnosis  ->  KMP ----------------------

    private void answerDiagnosis(String query, EntityExtractor.Entities e) {
        boolean pestTalk = !e.pests.isEmpty() || hasPestWord(query);

        // "pest control in rice" with no specific symptom -> list what attacks it
        if (e.symptoms.isEmpty() && e.diseases.isEmpty() && e.pests.isEmpty()) {
            if (pestTalk && !e.crops.isEmpty()) {
                showCropProtection(e.crops);
                return;
            }
            say("Tell me what you see on the plant, for example "
                + Theme.sun("yellow leaves, brown spots, wilting") + ".");
            answerSearch(query, e);
            return;
        }

        List<Diagnoser.Suspect> suspects = diagnoser.diagnose(e.symptoms, e.crops, e.diseases);
        List<Diagnoser.PestSuspect> pestSuspects =
                diagnoser.diagnosePests(e.symptoms, e.crops, e.pests);

        if (suspects.isEmpty() && pestSuspects.isEmpty()) {
            say("I could not match those symptoms to any disease or pest in my records.");
            return;
        }

        // Did ANY symptom actually match a record? If not, we are only listing the
        // diseases and pests of that crop, so say that plainly instead of sounding certain.
        boolean anyEvidence = !e.diseases.isEmpty() || !e.pests.isEmpty();
        for (Diagnoser.Suspect s : suspects) {
            if (!s.matchedSymptoms.isEmpty()) { anyEvidence = true; break; }
        }
        for (Diagnoser.PestSuspect s : pestSuspects) {
            if (!s.matchedDamage.isEmpty()) { anyEvidence = true; break; }
        }

        if (anyEvidence) {
            if (!e.symptoms.isEmpty()) {
                say("Walking your field with " + Theme.sun(list(e.symptoms))
                    + (e.crops.isEmpty() ? "" : " on " + Theme.sun(list(e.crops))) + " ...");
            } else {
                say("Looking up " + Theme.sun(list(e.diseases.isEmpty() ? e.pests : e.diseases))
                    + " ...");
            }
            System.out.println();
            System.out.println(Theme.section("what is likely wrong"));
        } else {
            say("No record of mine lists " + Theme.sun(list(e.symptoms))
                + " exactly. These are the diseases and pests that attack "
                + Theme.sun(e.crops.isEmpty() ? "this crop" : list(e.crops))
                + " - check which one matches what you see:");
            System.out.println();
            System.out.println(Theme.section("possible, but unconfirmed"));
        }

        int top = suspects.isEmpty() ? 0 : suspects.get(0).score;
        int topPest = pestSuspects.isEmpty() ? 0 : pestSuspects.get(0).score;
        int topAll = Math.max(top, topPest);
        int shown = 0;
        for (Diagnoser.Suspect s : suspects) {
            if (shown++ >= 3) break;
            System.out.println();
            System.out.println(PAD + Theme.bold(Theme.alert(Theme.padRight(
                        shown + "  " + s.disease.name.toUpperCase(), 46)))
                    + Theme.meter(s.score, Math.max(topAll, 1), 10)
                    + Theme.stone("  score " + s.score));
            detail("attacks",   String.join(", ", s.disease.crops)
                    + (s.cropMatches ? Theme.leaf("   " + Theme.I_ARROW + " includes your crop") : ""),
                    Theme::chalk);
            detail("symptoms",  String.join(", ", s.disease.symptoms), Theme::stone);
            if (!s.matchedSymptoms.isEmpty())
                detail("matched", list(s.matchedSymptoms), Theme::sun);
            detail("treatment", s.disease.treatment, Theme::leaf);
        }

        if (!pestSuspects.isEmpty()) {
            System.out.println();
            System.out.println(Theme.section("could also be a pest"));
        }

        shown = 0;
        for (Diagnoser.PestSuspect s : pestSuspects) {
            if (shown++ >= 3) break;
            System.out.println();
            System.out.println(PAD + Theme.bold(Theme.sun(Theme.padRight(
                        shown + "  " + Theme.title(s.pest.name).toUpperCase(), 46)))
                    + Theme.meter(s.score, Math.max(topAll, 1), 10)
                    + Theme.stone("  score " + s.score));
            detail("attacks",   String.join(", ", s.pest.crops)
                    + (s.cropMatches ? Theme.leaf("   " + Theme.I_ARROW + " includes your crop") : ""),
                    Theme::chalk);
            detail("damage",    String.join(", ", s.pest.damage), Theme::stone);
            if (!s.matchedDamage.isEmpty())
                detail("matched", list(s.matchedDamage), Theme::sun);
            detail("control",   s.pest.control, Theme::leaf);
        }

        // also show reading material about it
        List<String> terms = new ArrayList<>();
        if (!suspects.isEmpty()) terms.add(suspects.get(0).disease.name);
        else if (!pestSuspects.isEmpty()) terms.add(pestSuspects.get(0).pest.name);
        terms.addAll(e.crops);
        List<SearchResult> docs = searchEngine.searchArticlesOnly(terms);
        if (!docs.isEmpty()) {
            System.out.println();
            System.out.println(PAD + Theme.stone("read more"));
            int n = Math.min(2, docs.size());
            for (int i = 0; i < n; i++) {
                System.out.println(DETAIL + Theme.ash("[" + docs.get(i).article.id + "]  ")
                        + Theme.crop(docs.get(i).article.title));
            }
        }
    }

    // ---------------- FEATURE : record lookups ---------------------------

    private void answerCropInfo(String query, EntityExtractor.Entities e) {
        String cropName = e.crops.iterator().next();
        Crop c = data.findCrop(cropName);
        if (c == null) { answerSearch(query, e); return; }

        say("Everything I know about " + Theme.sun(Theme.title(c.name)) + ":");
        System.out.println();
        System.out.println(Theme.section("crop profile"));
        System.out.println();
        record(c.pretty());

        List<String> terms = new ArrayList<>();
        terms.add(c.name);
        List<SearchResult> docs = searchEngine.searchArticlesOnly(terms);
        printTopDocuments(docs, 2);
        printRelated(docs);
    }

    private void answerFertilizerInfo(EntityExtractor.Entities e) {
        String name = e.fertilizers.iterator().next();
        Fertilizer f = data.findFertilizer(name);
        if (f == null) return;

        say("Everything I know about " + Theme.sun(Theme.title(f.name)) + ":");
        System.out.println();
        System.out.println(Theme.section("fertilizer profile"));
        System.out.println();
        record(f.pretty());
    }

    /**
     * One "label   value" line inside a card. The value is wrapped so the block
     * never spills past the right edge of the centred page.
     */
    private void detail(String label, String value, java.util.function.Function<String, String> ink) {
        int indentWidth = DETAIL.length() + LABEL_W;
        String indent = " ".repeat(indentWidth);
        String wrapped = Theme.wrap(value, Theme.WIDTH - indentWidth - 1, indent);
        System.out.println(DETAIL + Theme.stone(Theme.padRight(label, LABEL_W)) + ink.apply(wrapped));
    }

    /** Print a record block, colouring the label on the left of each colon. */
    private void record(String block) {
        for (String l : block.split("\n")) {
            int colon = l.indexOf(':');
            if (colon > 0) {
                String label = l.substring(0, colon).trim();
                String value = l.substring(colon + 1).trim();
                boolean isName = label.equalsIgnoreCase("crop")
                              || label.equalsIgnoreCase("fertilizer")
                              || label.equalsIgnoreCase("disease")
                              || label.equalsIgnoreCase("pest");
                detail(label.toLowerCase(), isName ? Theme.title(value) : value,
                       isName ? Theme::leaf : Theme::chalk);
            } else {
                System.out.println(DETAIL + l);
            }
        }
    }

    // ------------- FEATURE : compare crops  ->  side by side --------------

    private void answerCompare(EntityExtractor.Entities e) {
        List<String> names = new ArrayList<>(e.crops);
        while (names.size() > 2) names.remove(names.size() - 1);   // two columns fit
        Crop a = data.findCrop(names.get(0));
        Crop b = data.findCrop(names.get(1));
        if (a == null || b == null) { answerSearch("", e); return; }

        say("Side by side: " + Theme.sun(Theme.title(a.name))
                + " vs " + Theme.sun(Theme.title(b.name)) + ".");
        System.out.println();

        Theme.Table t = Theme.Table.of(Theme.I_CROP, "head to head")
                .col("",            0,  false, Theme::stone)
                .col(Theme.title(a.name), 28, false, Theme::chalk)
                .col(Theme.title(b.name), 28, false, Theme::chalk);
        t.row("Season",      a.season,       b.season);
        t.row("Soil",        a.soil,         b.soil);
        t.row("Water need",  a.waterNeed,    b.waterNeed);
        t.row("Duration",    a.duration,     b.duration);
        t.row("Temperature", a.temperature,  b.temperature);
        t.row("Rainfall",    a.rainfall,     b.rainfall);
        t.row("Spacing",     a.spacing,      b.spacing);
        t.row("Varieties",   a.varieties,    b.varieties);
        t.row("Yield",       a.yield,        b.yield);
        t.row("Nutrients",   "N" + a.n + " P" + a.p + " K" + a.k,
                             "N" + b.n + " P" + b.p + " K" + b.k);
        t.print();

        System.out.println();
        System.out.println(PAD + Theme.stone("tip  ") + Theme.sun("ask me about any one of them,"));
        System.out.println(PAD + Theme.stone("     ") + Theme.sun("e.g. \"varieties of " + a.name + "\""));
    }

    // ------------- FEATURE : crop planner  ->  rainfall / climate ----------

    private void answerPlanner(String query, EntityExtractor.Entities e) {
        Planner.Plan plan = planner.planFor(query);

        if (plan.crops.isEmpty()) {
            say("I could not match those conditions to any crop in my records.");
            System.out.println();
            System.out.println(PAD + Theme.stone("try  ") + Theme.sun("\"crops for 800 mm rainfall\""));
            System.out.println(PAD + Theme.stone("     ") + Theme.sun("\"best crops for a hot dry climate\""));
            return;
        }

        say("Crops that fit " + Theme.sun(plan.notes.isEmpty() ? "your conditions"
                : String.join(", ", plan.notes)) + ":");
        System.out.println();

        int n = Math.min(10, plan.crops.size());
        Theme.Table t = Theme.Table.of(Theme.I_CROP, "crops for your conditions")
                .col("crop",      0,  false, Theme::chalk)
                .col("water",     7,  false, Theme::water)
                .col("duration", 13, false, Theme::stone)
                .col("temp",      9, false, Theme::stone)
                .col("rainfall", 12, false, Theme::stone)
                .col("yield",    10, false, Theme::leaf);
        for (int i = 0; i < n; i++) {
            Crop c = plan.crops.get(i);
            t.row(c.name, c.waterNeed, c.duration, c.temperature, c.rainfall, c.yield);
        }
        t.print();

        System.out.println();
        System.out.println(PAD + Theme.stone("ask me about any of them, e.g. ")
                + Theme.sun("\"how to grow " + plan.crops.get(0).name + "\""));
    }

    // ------------- FEATURE : pest profile ---------------------------------

    private void answerPestInfo(EntityExtractor.Entities e) {
        String name = e.pests.iterator().next();
        model.Pest p = data.findPest(name);
        if (p == null) { answerSearch("", e); return; }

        say("Everything I know about " + Theme.sun(Theme.title(p.name)) + ":");
        System.out.println();
        System.out.println(Theme.section("pest profile"));
        System.out.println();
        record(p.pretty());

        List<String> terms = new ArrayList<>();
        terms.add(p.name);
        terms.addAll(e.crops);
        List<SearchResult> docs = searchEngine.searchArticlesOnly(terms);
        printTopDocuments(docs, 2);
        printRelated(docs);
    }

    /** "pest control in rice" - list every disease and pest that attacks the crop. */
    private void showCropProtection(Set<String> cropNames) {
        say("Here is what can attack " + Theme.sun(list(cropNames)) + " and how to protect it:");
        System.out.println();

        for (String name : cropNames) {
            List<String> diseases = new ArrayList<>();
            for (model.Disease d : data.diseases)
                if (d.crops.contains(name)) diseases.add(d.name);
            List<String> pests = new ArrayList<>();
            for (model.Pest p : data.pests)
                if (p.crops.contains(name)) pests.add(p.name);

            System.out.println(Theme.section(name + " protection"));
            System.out.println();
            System.out.println(PAD + Theme.crop(Theme.padRight("diseases", 10))
                    + (diseases.isEmpty() ? Theme.stone("none on record")
                                          : Theme.chalk(String.join(", ", diseases))));
            System.out.println(PAD + Theme.crop(Theme.padRight("pests", 10))
                    + (pests.isEmpty() ? Theme.stone("none on record")
                                       : Theme.chalk(String.join(", ", pests))));
            System.out.println();
            System.out.println(PAD + Theme.stone("name any one of them, or describe what you see,"));
            System.out.println(PAD + Theme.stone("and I will give the full treatment."));
        }
    }

    private static boolean hasPestWord(String query) {
        return hasAny(query.toLowerCase(), "pest", "insect", "bug", "worm", "borer", "moth",
                "hopper", "fly", "mite", "aphid", "weevil", "caterpillar", "maggot", "grub");
    }

    private static boolean hasAny(String text, String... words) {
        for (String w : words) if (text.contains(w)) return true;
        return false;
    }

    // ------------- FEATURE : general search  ->  RABIN-KARP --------------

    private void answerSearch(String query, EntityExtractor.Entities e) {
        List<String> terms = e.allTerms();
        if (terms.isEmpty()) {
            terms = SearchEngine.fallbackTerms(query);
            Trace.log("Term selection", "no entity detected, using plain words " + terms);
        }

        List<SearchResult> results = searchEngine.search(terms);

        // FALLBACK 1 : nothing matched, so look for the closest words that do
        //              exist in the index (Edit Distance again).
        if (results.isEmpty() && !terms.isEmpty()) {
            List<String> suggestions = searchEngine.suggestTerms(terms);
            if (!suggestions.isEmpty()) {
                results = searchEngine.search(suggestions);
                if (!results.isEmpty()) {
                    say("Nothing matches " + Theme.sun(list(terms))
                        + " in my records. The closest I have is "
                        + Theme.sun(list(suggestions)) + ":");
                    printTopDocuments(results, 3);
                    printRelated(results);
                    return;
                }
            }
        }

        // FALLBACK 2 : still nothing - never dead end, show what we DO cover.
        if (results.isEmpty()) {
            showTopicMenu();
            return;
        }

        say("I found " + Theme.sun(results.size() + " documents") + ". The best of them:");
        printTopDocuments(results, 3);
        printRelated(results);
    }

    /** Last resort - tell the farmer exactly what this system can answer. */
    private void showTopicMenu() {
        say("That one is not in my store yet. Here is what I do carry:");
        System.out.println();

        List<String> crops = searchEngine.sampleTopics();
        System.out.println(Theme.section("crops I know (" + crops.size() + ")"));
        System.out.println();
        System.out.println(PAD + Theme.crop(Theme.wrap(String.join(", ", crops),
                Theme.WIDTH - PAD.length() - 1, PAD)));

        System.out.println();
        System.out.println(Theme.section("I can also"));
        System.out.println();
        can(Theme.I_DISEASE, "name a disease from the symptoms you describe");
        can(Theme.I_WARN,    "name the pest from the damage you describe");
        can(Theme.I_FERT,    "pick the best fertilizers inside your budget");
        can(Theme.I_LINK,    "give one fertilizer to each of your crops");
        can(Theme.I_CROP,    "compare two crops side by side");
        can(Theme.I_CROP,    "plan crops for your rainfall and climate");
        can(Theme.I_DOC,     "search my " + data.articles.size() + " farming articles");

        System.out.println();
        System.out.println(PAD + Theme.stone("try  ") + Theme.sun("how to grow rice"));
        System.out.println(PAD + Theme.stone("     ") + Theme.sun("my tomato has yellow leaves"));
        System.out.println(PAD + Theme.stone("     ") + Theme.sun("suggest fertilizer for wheat under 2000"));
    }

    private void can(String icon, String what) {
        System.out.println(PAD + Theme.leaf(icon) + "  " + Theme.chalk(what));
    }

    private void printTopDocuments(List<SearchResult> results, int howMany) {
        int n = Math.min(howMany, results.size());
        if (n == 0) return;
        System.out.println();
        System.out.println(Theme.section("from the field notes"));
        for (int i = 0; i < n; i++) {
            SearchResult r = results.get(i);
            System.out.println();
            System.out.println(PAD + Theme.bold(Theme.leaf((i + 1) + "  " + r.article.title)));
            System.out.println(DETAIL + Theme.ash("[" + r.article.id + "]")
                    + Theme.stone("  score " + r.score + "  " + Theme.I_DOT
                                + "  matched \"" + r.matchedTerm + "\""));
            System.out.println(DETAIL + Theme.chalk(Theme.wrap(r.snippet,
                    Theme.WIDTH - DETAIL.length() - 1, DETAIL)));
        }
    }

    private void printRelated(List<SearchResult> results) {
        if (results.isEmpty()) return;
        List<SearchEngine.Related> related = searchEngine.relatedArticles(results.get(0).article, 3);
        if (related.isEmpty()) return;

        System.out.println();
        System.out.println(Theme.section("related reading"));
        System.out.println();
        for (SearchEngine.Related r : related) {
            System.out.println(PAD + Theme.leaf(Theme.I_WAVE) + "  " + Theme.ash("[" + r.article.id + "]  ")
                    + Theme.crop(r.article.title));
            System.out.println(DETAIL + "  " + Theme.stone("shares " + r.similarity + " characters  ")
                    + Theme.dim("\"" + trim(r.sharedText, 44) + "\""));
        }
    }

    // ====================================================================
    // COMMANDS
    // ====================================================================

    private boolean isCommand(String line) {
        String c = line.toLowerCase();

        if (c.equals("exit") || c.equals("quit")) return true;

        if (c.equals("help")) { help(); return true; }

        if (c.startsWith("city ") && c.length() > 5) {
            weather.setDefaultCity(line.substring(5).trim());
            System.out.println();
            say("I will use " + Theme.sun(Theme.title(weather.defaultCity())) + " for weather from now on.");
            return true;
        }

        if (c.equals("clear") || c.equals("cls")) {
            Theme.clear();
            System.out.println();
            System.out.println(Theme.banner());
            return true;
        }

        if (c.equals("trace on"))  { Trace.enabled = true;
            System.out.println(); say("Algorithm trace is " + Theme.leaf("ON") + "."); return true; }
        if (c.equals("trace off")) { Trace.enabled = false;
            System.out.println(); say("Algorithm trace is " + Theme.stone("OFF") + "."); return true; }

        if (c.equals("color off") || c.equals("colour off")) {
            Theme.setColours(false); System.out.println(); say("Colours are off."); return true; }
        if (c.equals("color on") || c.equals("colour on")) {
            Theme.setColours(true); System.out.println(); say("Colours are on."); return true; }
        if (c.equals("color basic") || c.equals("colour basic")) {
            Theme.setPalette(Theme.Palette.BASIC); System.out.println();
            say("Using the 16 colour palette."); return true; }

        if (c.equals("ascii on"))  { Theme.setUnicode(false); System.out.println();
            say("Drawing with plain ASCII."); return true; }
        if (c.equals("ascii off")) { Theme.setUnicode(true); System.out.println();
            say("Drawing with box characters."); return true; }

        if (c.equals("center off") || c.equals("centre off")) {
            Centering.off(); System.out.println(); say("Centring is off."); return true; }
        if (c.equals("center on") || c.equals("centre on")) {
            Centering.setWidth(Centering.terminalWidth()); System.out.println();
            say("Centring is on."); return true; }
        if (c.startsWith("width ")) {
            try {
                int w = Integer.parseInt(c.substring(6).trim());
                Centering.setWidth(w);
                System.out.println();
                say("Layout set for a " + Theme.sun(w + " column") + " window.");
            } catch (NumberFormatException ex) {
                System.out.println(); say("Give me a number, for example " + Theme.sun("width 120") + ".");
            }
            return true;
        }

        if (c.equals("algo demo") || c.equals("demo")) { AlgorithmDemo.runAll(); return true; }

        if (c.equals("list crops")) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_CROP, "crops in the store")
                    .col("crop",     0,  false, Theme::chalk)
                    .col("season",   12, false, Theme::crop)
                    .col("soil",     22, false, Theme::stone)
                    .col("duration", 16, false, Theme::stone);
            for (Crop x : data.crops)
                t.row(Theme.title(x.name), x.season, x.soil, x.duration);
            t.print();
            return true;
        }
        if (c.equals("list fertilizers")) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_FERT, "fertilizers in the shed")
                    .col("name",    0,  false, Theme::chalk)
                    .col("npk",     10, false, Theme::stone)
                    .col("cost",    10, true,  Theme::soil)
                    .col("benefit",  9, true,  Theme::leaf);
            for (Fertilizer f : data.fertilizers)
                t.row(Theme.title(f.name), f.n + "-" + f.p + "-" + f.k,
                      "Rs " + f.cost, String.valueOf(f.benefit));
            t.print();
            return true;
        }
        if (c.equals("list diseases")) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_DISEASE, "diseases I watch for")
                    .col("disease", 0,  false, Theme::chalk)
                    .col("attacks", 40, false, Theme::stone);
            for (model.Disease d : data.diseases)
                t.row(Theme.title(d.name), String.join(", ", d.crops));
            t.print();
            return true;
        }
        if (c.equals("list pests")) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_WARN, "pests I watch for")
                    .col("pest",    0,  false, Theme::chalk)
                    .col("attacks", 40, false, Theme::stone);
            for (model.Pest p : data.pests)
                t.row(Theme.title(p.name), String.join(", ", p.crops));
            t.print();
            return true;
        }
        if (c.equals("list articles")) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_DOC, "field notes")
                    .col("id",    6, false, Theme::ash)
                    .col("title", 0, false, Theme::chalk);
            for (model.Article a : data.articles) t.row(a.id, a.title);
            t.print();
            return true;
        }
        return false;
    }

    private void help() {
        System.out.println();
        System.out.println(Theme.section("in the field"));
        System.out.println();
        example(Theme.I_CROP,    "how to grow rice",            "full crop profile");
        example(Theme.I_CROP,    "compare rice and wheat",      "side by side facts");
        example(Theme.I_DISEASE, "my tomato has yellow leaves", "disease diagnosis");
        example(Theme.I_WARN,    "my brinjal has a pest",       "what attacks this crop");
        example(Theme.I_WARN,    "how to control whitefly",     "pest profile");

        System.out.println();
        System.out.println(Theme.section("planning"));
        System.out.println();
        example(Theme.I_CROP, "which crops suit low rainfall", "area planner");
        example(Theme.I_CROP, "crops for a hot climate",       "rainfall and temperature");
        example(Theme.I_WAVE, "weather in guntur",              "live weather and 5 day outlook");
        example(Theme.I_WAVE, "will it rain tomorrow",          "your town (set it with: city guntur)");

        System.out.println();
        System.out.println(Theme.section("at the shop"));
        System.out.println();
        example(Theme.I_FERT, "suggest fertilizer for tomato under 3000", "best basket in budget");
        example(Theme.I_LINK, "match fertilizers for rice cotton banana", "one bag per crop");
        example(Theme.I_FERT, "what is dap",                             "fertilizer profile");

        System.out.println();
        System.out.println(Theme.section("commands"));
        System.out.println();
        command("list crops | list fertilizers", "everything in the data files");
        command("list diseases | list pests | list articles", "");
        command("trace on | trace off",          "show the algorithm trace");
        command("algo demo",                     "run all 8 algorithms on tiny inputs");
        command("clear | help | exit",           "");
    }

    private void example(String icon, String q, String what) {
        System.out.println(PAD + Theme.leaf(icon) + "  " + Theme.sun(Theme.padRight(q, 43))
                + Theme.stone(what));
    }

    private void command(String c, String what) {
        System.out.println(PAD + Theme.crop(Theme.padRight(c, 40)) + Theme.stone(what));
    }

    // ====================================================================
    // LIVE WEATHER
    // ====================================================================

    private static final String[] WEATHER_WORDS = {
        "weather", "forecast", "mausam", "will it rain", "rain today", "rain tomorrow",
        "raining", "temperature today", "temperature in", "temperature at", "humidity",
        "how hot", "how cold", "climate in", "climate at", "outlook"
    };

    private boolean isWeatherQuestion(String q) {
        String c = q.toLowerCase();
        for (String w : WEATHER_WORDS) if (c.contains(w)) return true;
        return false;
    }

    /** "weather in guntur" -> "guntur"; nothing named -> the default town. */
    private String extractCity(String q) {
        String c = q.toLowerCase().replaceAll("[?.!,]", " ").trim();
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\b(?:in|at|for|of|near)\\s+([a-z][a-z .'-]*)$").matcher(c);
        if (m.find()) {
            String city = m.group(1).trim()
                    .replaceAll("\\b(today|tomorrow|now|this week|please)\\b", "").trim();
            if (!city.isEmpty()) return city;
        }
        return weather.defaultCity();
    }

    private void showWeather(String city) {
        WeatherService.Report r;
        try {
            r = weather.lookup(city);
        } catch (Exception e) {
            System.out.println();
            say("I could not fetch the weather: " + e.getMessage());
            return;
        }

        String where = r.city + (r.country.isEmpty() ? "" : ", " + r.country);
        System.out.println();
        say("Live weather for " + Theme.sun(where) + ":");
        System.out.println();

        System.out.println(Theme.panelTop(Theme.I_WAVE, "right now"));
        System.out.println(Theme.panelRow(""));
        System.out.println(Theme.panelRow(PAD + Theme.bold(Theme.chalk(String.format("%.0f", r.temp) + " C"))
                + Theme.stone("  feels like " + String.format("%.0f", r.feelsLike) + " C")
                + Theme.stone("   " + Theme.I_DOT + "   ") + Theme.leaf(r.condition)));
        System.out.println(Theme.panelRow(PAD
                + Theme.stone("humidity ") + Theme.water((int) r.humidity + "%")
                + Theme.stone("     wind ") + Theme.water(String.format("%.0f km/h", r.windMs * 3.6))
                + Theme.stone("     cloud ") + Theme.water(r.cloudPct + "%")
                + (r.rainMm > 0 ? Theme.stone("     rain ") + Theme.water(String.format("%.1f mm/h", r.rainMm)) : "")));
        System.out.println(Theme.panelRow(""));
        System.out.println(Theme.panelBottom());

        if (!r.days.isEmpty()) {
            System.out.println();
            Theme.Table t = Theme.Table.of(Theme.I_CAL, "next five days")
                    .col("day",    8, false, Theme::chalk)
                    .col("sky",    0, false, Theme::stone)
                    .col("low",    6, true,  Theme::water)
                    .col("high",   6, true,  Theme::sun)
                    .col("rain",   6, true,  Theme::water)
                    .col("chance", 7, true,  Theme::stone);
            for (WeatherService.Day d : r.days) {
                t.row(d.name, d.condition,
                      String.format("%.0f C", d.min), String.format("%.0f C", d.max),
                      d.rainMm > 0 ? String.format("%.0f mm", d.rainMm) : "-",
                      d.rainChance > 0 ? String.format("%.0f%%", d.rainChance) : "-");
            }
            t.print();
        }

        weatherAdvice(r);
    }

    /** Turn the readings into a few field decisions. */
    private void weatherAdvice(WeatherService.Report r) {
        List<String> tips = new ArrayList<>();
        WeatherService.Day today    = r.days.isEmpty()   ? null : r.days.get(0);
        WeatherService.Day tomorrow = r.days.size() > 1  ? r.days.get(1) : null;
        double rainSoon = Math.max(today == null ? 0 : today.rainChance,
                                   tomorrow == null ? 0 : tomorrow.rainChance);

        if (r.rainMm > 0 || rainSoon >= 60)
            tips.add("rain is likely - hold irrigation and do not spray, it will wash off");
        else if (rainSoon >= 30)
            tips.add("some chance of rain - spray early morning so it dries before any shower");
        else if (r.temp >= 32)
            tips.add("dry and hot - irrigate in the evening to cut evaporation loss");

        if (r.windMs * 3.6 >= 20)
            tips.add("wind is strong - pesticide drift will be high, postpone spraying");
        if (r.humidity >= 80 && r.temp >= 20)
            tips.add("warm and humid - fungal diseases spread fast, inspect leaves for blight and mildew");
        if (r.temp >= 38)
            tips.add("heat stress - give a light mulch and avoid transplanting today");
        if (r.temp <= 8)
            tips.add("cold - protect seedlings, frost damage possible at night");

        double weekRain = 0;
        for (WeatherService.Day d : r.days) weekRain += d.rainMm;
        if (weekRain >= 40)
            tips.add(String.format("about %.0f mm expected this week - good time for sowing rainfed crops", weekRain));

        if (!tips.isEmpty()) {
            System.out.println();
            System.out.println(PAD + Theme.crop("for your field"));
            for (String tip : tips)
                System.out.println(PAD + Theme.leaf(Theme.I_ARROW) + "  " + Theme.chalk(tip));
        }

        Planner.Plan plan = planner.planFor("temperature " + Math.round(r.temp) + " c");
        if (!plan.crops.isEmpty()) {
            StringBuilder names = new StringBuilder();
            for (int i = 0; i < Math.min(5, plan.crops.size()); i++) {
                if (names.length() > 0) names.append(", ");
                names.append(plan.crops.get(i).name);
            }
            System.out.println();
            System.out.println(PAD + Theme.stone("crops that like " + Math.round(r.temp) + " C: ")
                    + Theme.sun(names.toString()));
        }
    }

    // ====================================================================
    // SMALL PRINTING HELPERS
    // ====================================================================

    /** Sophie speaks. Long replies wrap under her name instead of past the edge. */
    private void say(String msg) {
        blankLine();
        String indent = Theme.voiceIndent();
        System.out.println(Theme.voice()
                + Theme.chalk(Theme.wrap(msg, Theme.WIDTH - Theme.visible(indent), indent)));
    }

    /** A quiet aside directly under whatever Sophie just said. */
    private void note(String msg) {
        String indent = Theme.voiceIndent();
        System.out.println(indent
                + Theme.stone(Theme.wrap(msg, Theme.WIDTH - Theme.visible(indent), indent)));
    }

    /**
     * Open a block with exactly one blank line above it. Callers that already
     * printed their own blank line cost nothing, so the spacing stays even no
     * matter which order the pipeline printed things in.
     */
    private void blankLine() {
        if (!Centering.lastLineWasBlank()) System.out.println();
    }

    /** "a, b, c" rather than the "[a, b, c]" of List.toString(). */
    private static String list(java.util.Collection<String> items) {
        return String.join(", ", items);
    }

    private static String trim(String s, int max) {
        s = s.replace('\n', ' ');
        return s.length() <= max ? s : s.substring(0, max) + Theme.ELLIPSIS;
    }

    private void showSuggestions(List<String> list) {
        if (list == null || list.isEmpty()) return;
        this.currentSuggestions = new ArrayList<>(list);
        System.out.println();
        System.out.println(Theme.section("suggested queries (type number to run)"));
        System.out.println();
        for (int i = 0; i < list.size(); i++) {
            System.out.println(PAD + Theme.sun("[" + (i + 1) + "] ") + Theme.chalk(list.get(i)));
        }
    }
}
