package ui;

import engine.DataLoader;
import engine.Diagnoser;
import engine.EntityExtractor;
import engine.IntentDetector;
import engine.Recommender;
import engine.SearchEngine;
import engine.SearchResult;
import engine.SmallTalk;
import engine.SpellCorrector;
import engine.SynonymMapper;
import model.Crop;
import model.Fertilizer;
import util.Trace;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * ==========================================================================
 * FARMASSIST - console chat bot
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
 * ==========================================================================
 */
public class ConsoleChat {

    private DataLoader data;
    private SmallTalk smallTalk;
    private SynonymMapper synonymMapper;
    private SpellCorrector spellCorrector;
    private EntityExtractor entityExtractor;
    private IntentDetector intentDetector;
    private SearchEngine searchEngine;
    private Diagnoser diagnoser;
    private Recommender recommender;

    public static void main(String[] args) {
        String dataFolder = (args.length > 0) ? args[0] : "data";
        new ConsoleChat().start(dataFolder);
    }

    public void start(String dataFolder) {
        Centering.apply();                 // sit the whole chat in the middle of the window
        System.out.println(Theme.banner());

        System.out.println("\n  " + Theme.crop("Ploughing through the knowledge base ..."));
        data = new DataLoader();
        data.loadAll(dataFolder);
        if (data.articles.isEmpty()) {
            System.out.println("\n  " + Theme.alert(Theme.WARNING + " The fields are empty - no data was loaded."));
            System.out.println("  Run the program from the FarmAssist folder, or pass the");
            System.out.println("  data folder path as an argument.\n");
            return;
        }

        smallTalk       = new SmallTalk(data);
        synonymMapper   = new SynonymMapper(data);
        spellCorrector  = new SpellCorrector(data, smallTalk.triggerWords());
        entityExtractor = new EntityExtractor(data);
        intentDetector  = new IntentDetector();
        searchEngine    = new SearchEngine(data);
        diagnoser       = new Diagnoser(data);
        recommender     = new Recommender(data);

        printHarvestReport();
        help();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.print(Theme.prompt());
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine().trim();
            if (line.isEmpty()) continue;

            if (isCommand(line)) {
                if (line.equalsIgnoreCase("exit") || line.equalsIgnoreCase("quit")) {
                    System.out.println();
                    say("May your fields stay green. Goodbye.");
                    System.out.println("\n" + Theme.fence() + "\n");
                    break;
                }
                continue;
            }
            answer(line);
        }
        sc.close();
    }

    /** The "what is in the barn" summary printed at start up. */
    private void printHarvestReport() {
        System.out.println();
        System.out.println(Theme.header(Theme.SCROLL, "WHAT IS IN THE BARN"));
        System.out.println();
        barnLine(Theme.SPROUT, "Crops",         data.crops.size());
        barnLine(Theme.WARNING, "Diseases",     data.diseases.size());
        barnLine(Theme.SACK, "Fertilizers",     data.fertilizers.size());
        barnLine(Theme.SCROLL, "Articles",      data.articles.size());
        System.out.println();
        System.out.println("   " + Theme.stone("search index      ") + Theme.water(
                searchEngine.indexSize() + " documents"));
        System.out.println("   " + Theme.stone("spell dictionary  ") + Theme.water(
                spellCorrector.dictionarySize() + " words"));
        System.out.println("   " + Theme.stone("pattern trie      ") + Theme.water(
                entityExtractor.patternCount() + " crop, disease, fertilizer and symptom names"));
        System.out.println("   " + Theme.stone("local names       ") + Theme.water(
                synonymMapper.size() + " synonyms"));
    }

    private void barnLine(String icon, String label, int count) {
        System.out.println("   " + Theme.leaf(icon) + "  " + Theme.crop(Theme.padRight(label, 14))
                + Theme.bold(Theme.sun(Theme.padLeft(String.valueOf(count), 4))));
    }

    // ====================================================================
    // MAIN PIPELINE
    // ====================================================================

    private void answer(String rawQuery) {
        System.out.println();

        // ---- STAGE 0 : EVERYDAY CONVERSATION ----------------------------
        String chat = smallTalk.reply(rawQuery);
        if (chat != null) { System.out.println(); say(chat); return; }

        // ---- STAGE 1 : EDIT DISTANCE + WORD SPLITTER --------------------
        SpellCorrector.Result sp = spellCorrector.correct(rawQuery);
        String query = sp.correctedQuery;
        if (!sp.corrections.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (SpellCorrector.Correction c : sp.corrections) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(c.wrong).append(" -> ").append(c.right);
            }
            System.out.println();
            say("I adjusted your words: " + Theme.sun(sb.toString()));
            System.out.println("              " + Theme.stone("reading it as \"" + query + "\""));
        }

        // ---- STAGE 1c : SYNONYMS ("paddy" means "rice") ------------------
        String mapped = synonymMapper.rewrite(query);
        if (!mapped.equalsIgnoreCase(query)) {
            System.out.println();
            say("I understood that as: " + Theme.sun("\"" + mapped + "\""));
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
            case DISEASE_DIAGNOSIS:     answerDiagnosis(query, entities);   break;
            case CROP_INFO:             answerCropInfo(query, entities);    break;
            case FERTILIZER_INFO:       answerFertilizerInfo(entities);     break;
            default:                    answerSearch(query, entities);      break;
        }
    }

    // ---------------- FEATURE : budget  ->  KNAPSACK ---------------------

    private void answerBudget(String query, EntityExtractor.Entities e) {
        int budget = IntentDetector.extractBudget(query);
        String crop = e.crops.isEmpty() ? null : e.crops.iterator().next();

        if (budget <= 0) {
            say("Tell me your budget in rupees, for example: "
                + Theme.sun("suggest fertilizer for tomato under 3000"));
            return;
        }

        Recommender.BudgetPlan plan = recommender.planWithinBudget(crop, budget);

        say("Here is the best load for your bullock cart"
            + (crop != null ? " for " + Theme.sun(crop) : "")
            + ", within " + Theme.sun("Rs " + budget) + ":");

        if (plan.chosen.isEmpty()) {
            System.out.println("   " + Theme.alert("Nothing fits in this budget.")
                    + " The cheapest bag costs Rs " + cheapest(plan.considered) + ".");
            return;
        }

        System.out.println();
        System.out.println(Theme.header(Theme.SACK, "FERTILIZER PLAN"));
        System.out.println("   " + Theme.stone(Theme.padRight("FERTILIZER", 32)
                + Theme.padRight("NPK", 11) + Theme.padLeft("COST", 9)
                + Theme.padLeft("BENEFIT", 10)));
        System.out.println("   " + Theme.rule(62));
        for (Fertilizer f : plan.chosen) {
            System.out.println("   " + Theme.crop(Theme.padRight(f.name, 32))
                    + Theme.padRight(f.n + "-" + f.p + "-" + f.k, 11)
                    + Theme.soil(Theme.padLeft("Rs " + f.cost, 9))
                    + Theme.leaf(Theme.padLeft(f.benefit + "/100", 10)));
        }
        System.out.println("   " + Theme.rule(62));
        System.out.println("   " + Theme.bold(Theme.padRight("TOTAL", 32) + Theme.padRight("", 11))
                + Theme.bold(Theme.soil(Theme.padLeft("Rs " + plan.totalCost, 9)))
                + Theme.bold(Theme.leaf(Theme.padLeft(String.valueOf(plan.totalBenefit), 10))));

        System.out.println("\n   " + Theme.sun("Money left in your pocket : Rs ")
                + Theme.bold(Theme.sun(String.valueOf(budget - plan.totalCost))));
        System.out.println("   " + Theme.stone(
                "Chosen by 0/1 Knapsack - the highest total benefit that fits the budget."));
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
        System.out.println(Theme.header(Theme.LINK, "FIELD BY FIELD PLAN"));
        System.out.println("   " + Theme.stone(Theme.padRight("CROP", 22)
                + Theme.padRight("FERTILIZER", 32) + "NPK"));
        System.out.println("   " + Theme.rule(62));

        for (int i = 0; i < plan.crops.size(); i++) {
            int f = plan.assignment[i];
            String crop = Theme.crop(Theme.padRight(plan.crops.get(i), 18)) + Theme.leaf("--> ");
            if (f >= 0) {
                Fertilizer fert = plan.pool.get(f);
                System.out.println("   " + crop + Theme.soil(Theme.padRight(fert.name, 32))
                        + (fert.n + "-" + fert.p + "-" + fert.k));
            } else {
                System.out.println("   " + crop + Theme.alert("no free suitable bag"));
            }
        }
        System.out.println("\n   " + Theme.sun("Served " + plan.matchedCount + " of "
                + plan.crops.size() + " fields")
                + Theme.stone("  (maximum bipartite matching)"));
    }

    // ----------------- FEATURE : diagnosis  ->  KMP ----------------------

    private void answerDiagnosis(String query, EntityExtractor.Entities e) {
        if (e.symptoms.isEmpty() && e.diseases.isEmpty()) {
            say("Tell me what you see on the plant - for example "
                + Theme.sun("yellow leaves, brown spots, wilting") + ".");
            answerSearch(query, e);
            return;
        }

        List<Diagnoser.Suspect> suspects = diagnoser.diagnose(e.symptoms, e.crops, e.diseases);

        if (suspects.isEmpty()) {
            say("I could not match those symptoms to any disease in my records.");
            return;
        }

        // Did ANY symptom actually match a record? If not, we are only listing the
        // diseases of that crop, so say that plainly instead of sounding certain.
        boolean anyEvidence = false;
        for (Diagnoser.Suspect s : suspects) {
            if (!s.matchedSymptoms.isEmpty()) { anyEvidence = true; break; }
        }

        if (anyEvidence) {
            say("Walking your field with those symptoms " + Theme.sun(e.symptoms.toString())
                + (e.crops.isEmpty() ? "" : " on " + Theme.sun(e.crops.toString())) + " ...");
            System.out.println();
            System.out.println(Theme.header(Theme.WARNING, "WHAT IS LIKELY WRONG"));
        } else {
            say("No record of mine lists " + Theme.sun(e.symptoms.toString())
                + " exactly. These are the diseases that attack "
                + Theme.sun(e.crops.isEmpty() ? "this crop" : e.crops.toString())
                + " - check which one matches what you see:");
            System.out.println();
            System.out.println(Theme.header(Theme.WARNING, "POSSIBLE, BUT UNCONFIRMED"));
        }

        int shown = 0;
        for (Diagnoser.Suspect s : suspects) {
            if (shown++ >= 3) break;
            System.out.println("\n   " + Theme.bold(Theme.alert(shown + ". "
                    + s.disease.name.toUpperCase()))
                    + Theme.stone("   match score " + s.score));
            field("attacks",   String.join(", ", s.disease.crops)
                    + (s.cropMatches ? "   <-- includes your crop" : ""), 1);
            field("symptoms",  String.join(", ", s.disease.symptoms), 0);
            field("matched",   s.matchedSymptoms.toString(), 2);
            field("treatment", s.disease.treatment, 3);
        }

        // also show reading material about it
        List<String> terms = new ArrayList<>();
        terms.add(suspects.get(0).disease.name);
        terms.addAll(e.crops);
        List<SearchResult> docs = searchEngine.searchArticlesOnly(terms);
        if (!docs.isEmpty()) {
            System.out.println("\n   " + Theme.crop("Read more:"));
            int n = Math.min(2, docs.size());
            for (int i = 0; i < n; i++) {
                System.out.println("      " + Theme.stone("[" + docs.get(i).article.id + "] ")
                        + docs.get(i).article.title);
            }
        }
    }

    // ---------------- FEATURE : record lookups ---------------------------

    private void answerCropInfo(String query, EntityExtractor.Entities e) {
        String cropName = e.crops.iterator().next();
        Crop c = data.findCrop(cropName);
        if (c == null) { answerSearch(query, e); return; }

        say("Everything I know about " + Theme.sun(c.name) + ":");
        System.out.println();
        System.out.println(Theme.header(Theme.SPROUT, "CROP PROFILE"));
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

        say("Everything I know about " + Theme.sun(f.name) + ":");
        System.out.println();
        System.out.println(Theme.header(Theme.SACK, "FERTILIZER PROFILE"));
        System.out.println();
        record(f.pretty());
    }

    /**
     * One "label   value" line of a disease card. The value is wrapped so the
     * block never spills past the right edge of the centred page.
     * colour: 0 plain, 1 crop green, 2 sun yellow, 3 leaf green.
     */
    private void field(String label, String value, int colour) {
        String indent = "                ";                    // 6 spaces + 10 label
        String wrapped = wrap(value, Centering.CONTENT_WIDTH - 18, indent);
        String painted;
        switch (colour) {
            case 1:  painted = Theme.crop(wrapped); break;
            case 2:  painted = Theme.sun(wrapped);  break;
            case 3:  painted = Theme.leaf(wrapped); break;
            default: painted = wrapped;
        }
        System.out.println("      " + Theme.stone(Theme.padRight(label, 10)) + painted);
    }

    /** Print a record block, colouring the label on the left of each colon. */
    private void record(String block) {
        for (String l : block.split("\n")) {
            int colon = l.indexOf(':');
            if (colon > 0) {
                String label = l.substring(0, colon + 1);
                String value = l.substring(colon + 1).trim();
                String indent = Theme.padRight("", 3 + label.length() + 1);
                System.out.println("   " + Theme.stone(label) + " "
                        + Theme.crop(wrap(value, Centering.CONTENT_WIDTH - label.length() - 4, indent)));
            } else {
                System.out.println("   " + l);
            }
        }
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
                    say("Nothing grows under " + Theme.sun(terms.toString())
                        + " in my records. The closest I have is "
                        + Theme.sun(suggestions.toString()) + ":");
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

        say("I dug up " + Theme.sun(results.size() + " documents") + ". The best of the harvest:");
        printTopDocuments(results, 3);
        printRelated(results);
    }

    /** Last resort - tell the farmer exactly what this system can answer. */
    private void showTopicMenu() {
        say("That seed is not in my store yet. Here is what I do carry:");
        System.out.println();

        List<String> crops = searchEngine.sampleTopics();
        System.out.println(Theme.header(Theme.SPROUT, "CROPS I KNOW (" + crops.size() + ")"));
        System.out.println();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < crops.size(); i++) {
            sb.append(crops.get(i));
            if (i < crops.size() - 1) sb.append(", ");
            if (sb.length() > 62) { System.out.println("      " + Theme.crop(sb.toString())); sb = new StringBuilder(); }
        }
        if (sb.length() > 0) System.out.println("      " + Theme.crop(sb.toString()));

        System.out.println("\n" + Theme.header(Theme.LENS, "I CAN ALSO"));
        System.out.println("      " + Theme.leaf(Theme.WARNING) + "  name a disease from the symptoms you describe");
        System.out.println("      " + Theme.leaf(Theme.SACK) + "  pick the best fertilizers inside your budget");
        System.out.println("      " + Theme.leaf(Theme.LINK) + "  give one fertilizer to each of your crops");
        System.out.println("      " + Theme.leaf(Theme.SCROLL) + "  search my " + data.articles.size() + " farming articles");

        System.out.println("\n   " + Theme.stone("try:  ") + Theme.sun("how to grow rice"));
        System.out.println("   " + Theme.stone("      ") + Theme.sun("my tomato has yellow leaves"));
        System.out.println("   " + Theme.stone("      ") + Theme.sun("suggest fertilizer for wheat under 2000"));
    }

    private void printTopDocuments(List<SearchResult> results, int howMany) {
        int n = Math.min(howMany, results.size());
        if (n > 0) System.out.println("\n" + Theme.header(Theme.LENS, "FROM THE FIELD NOTES"));
        for (int i = 0; i < n; i++) {
            SearchResult r = results.get(i);
            System.out.println("\n   " + Theme.bold(Theme.leaf((i + 1) + ". " + r.article.title)));
            System.out.println("      " + Theme.stone("[" + r.article.id + "]  score " + r.score
                    + "  best term \"" + r.matchedTerm + "\""));
            System.out.println("      " + Theme.crop(wrap(r.snippet, 68, "      ")));
        }
    }

    private void printRelated(List<SearchResult> results) {
        if (results.isEmpty()) return;
        List<SearchEngine.Related> related = searchEngine.relatedArticles(results.get(0).article, 3);
        if (related.isEmpty()) return;

        System.out.println("\n" + Theme.header(Theme.DROP, "GROWING NEARBY"));
        for (SearchEngine.Related r : related) {
            System.out.println("      " + Theme.stone("[" + r.article.id + "] ") + Theme.crop(r.article.title));
            System.out.println("            " + Theme.stone("shares " + r.similarity + " characters: ")
                    + Theme.dim("\"" + trim(r.sharedText, 52) + "\""));
        }
    }

    // ====================================================================
    // COMMANDS
    // ====================================================================

    private boolean isCommand(String line) {
        String c = line.toLowerCase();

        if (c.equals("exit") || c.equals("quit")) return true;

        if (c.equals("help")) { help(); return true; }

        if (c.equals("trace on"))  { Trace.enabled = true;
            System.out.println(); say("Algorithm trace is " + Theme.leaf("ON") + "."); return true; }
        if (c.equals("trace off")) { Trace.enabled = false;
            System.out.println(); say("Algorithm trace is " + Theme.stone("OFF") + "."); return true; }

        if (c.equals("color off") || c.equals("colour off")) {
            Theme.setColours(false); System.out.println(); say("Colours are off."); return true; }
        if (c.equals("color on") || c.equals("colour on")) {
            Theme.setColours(true); System.out.println(); say("Colours are on."); return true; }

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
            System.out.println("\n" + Theme.header(Theme.SPROUT, "CROPS IN THE STORE"));
            System.out.println();
            for (Crop x : data.crops) {
                System.out.println("   " + Theme.leaf(Theme.SPROUT) + " "
                        + Theme.crop(Theme.padRight(x.name, 20))
                        + Theme.stone(x.season + ", " + x.soil.toLowerCase()));
            }
            return true;
        }
        if (c.equals("list fertilizers")) {
            System.out.println("\n" + Theme.header(Theme.SACK, "FERTILIZERS IN THE SHED"));
            System.out.println();
            System.out.println("   " + Theme.stone(Theme.padRight("NAME", 32)
                    + Theme.padRight("NPK", 11) + Theme.padLeft("COST", 9)
                    + Theme.padLeft("BENEFIT", 10)));
            for (Fertilizer f : data.fertilizers) {
                System.out.println("   " + Theme.crop(Theme.padRight(f.name, 32))
                        + Theme.padRight(f.n + "-" + f.p + "-" + f.k, 11)
                        + Theme.soil(Theme.padLeft("Rs " + f.cost, 9))
                        + Theme.leaf(Theme.padLeft(String.valueOf(f.benefit), 10)));
            }
            return true;
        }
        if (c.equals("list diseases")) {
            System.out.println("\n" + Theme.header(Theme.WARNING, "DISEASES I WATCH FOR"));
            System.out.println();
            for (model.Disease d : data.diseases) {
                System.out.println("   " + Theme.alert(Theme.WARNING) + " "
                        + Theme.crop(Theme.padRight(d.name, 26))
                        + Theme.stone(String.join(", ", d.crops)));
            }
            return true;
        }
        if (c.equals("list articles")) {
            System.out.println("\n" + Theme.header(Theme.SCROLL, "FIELD NOTES"));
            System.out.println();
            for (model.Article a : data.articles) {
                System.out.println("   " + Theme.stone("[" + a.id + "] ") + Theme.crop(a.title));
            }
            return true;
        }
        return false;
    }

    private void help() {
        System.out.println();
        System.out.println(Theme.header(Theme.DROP, "JUST TALKING"));
        System.out.println("      " + Theme.sun("hi") + Theme.stone("  |  ") + Theme.sun("how are you")
                + Theme.stone("  |  ") + Theme.sun("what can you do")
                + Theme.stone("  |  ") + Theme.sun("thanks"));

        System.out.println("\n" + Theme.header(Theme.SPROUT, "IN THE FIELD"));
        example("how to grow rice", "full crop profile");
        example("paddy", "local and Hindi names work too");
        example("my tomato has yellow leaves", "disease diagnosis");
        example("lateblight", "joined words are split");
        example("bhindi me yellow vein", "mixed language");

        System.out.println("\n" + Theme.header(Theme.SACK, "AT THE SHOP"));
        example("suggest fertilizer for tomato under 3000", "best basket in budget");
        example("match fertilizers for rice cotton banana", "one bag per crop");
        example("what is dap", "fertilizer profile");

        System.out.println("\n" + Theme.header(Theme.SCROLL, "COMMANDS"));
        System.out.println("      " + Theme.crop("list crops | list fertilizers | list diseases | list articles"));
        System.out.println("      " + Theme.crop("trace on | trace off") + Theme.stone("     show the algorithm trace"));
        System.out.println("      " + Theme.crop("color on | color off") + Theme.stone("     if your terminal shows odd symbols"));
        System.out.println("      " + Theme.crop("width 120 | center off") + Theme.stone("   move the page in the window"));
        System.out.println("      " + Theme.crop("algo demo") + Theme.stone("                run all 8 algorithms on tiny inputs"));
        System.out.println("      " + Theme.crop("help | exit"));
        System.out.println();
        System.out.println(Theme.furrow());
    }

    private void example(String q, String what) {
        System.out.println("      " + Theme.sun(Theme.padRight(q, 44)) + Theme.stone(what));
    }

    // ====================================================================
    // SMALL PRINTING HELPERS
    // ====================================================================

    private void say(String msg) {
        System.out.println(Theme.voice() + msg);
    }

    private static String trim(String s, int max) {
        s = s.replace('\n', ' ');
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /** Wrap long text so the console output stays readable. */
    private static String wrap(String text, int width, String indent) {
        StringBuilder out = new StringBuilder();
        int lineLen = 0;
        for (String word : text.split("\\s+")) {
            if (lineLen + word.length() > width) {
                out.append("\n").append(indent);
                lineLen = 0;
            }
            out.append(word).append(' ');
            lineLen += word.length() + 1;
        }
        return out.toString().trim();
    }
}
