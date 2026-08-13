package engine;

import algo.EditDistance;
import algo.KMP;
import algo.RabinKarp;
import algo.RandomizedQuickSort;
import algo.SuffixArrayLCP;
import model.Article;
import model.Crop;
import model.Disease;
import model.Fertilizer;
import model.Pest;
import util.Trace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * STAGE 4-6 OF THE PIPELINE - the heart of the search.
 *
 *   ALGORITHM 2 : RABIN-KARP        -> count each term in every document
 *   ALGORITHM 1 : KMP               -> locate the term exactly, cut the snippet
 *   ALGORITHM 8 : RANDOMIZED QSORT  -> rank the documents by score
 *   ALGORITHM 5 : SUFFIX ARRAY+LCP  -> find related articles
 */
public class SearchEngine {

    private final DataLoader data;

    /**
     * EVERYTHING that can be searched. The articles are only part of it - each
     * crop, disease and fertilizer record is turned into a document too, so a
     * question like "paddy" finds the rice record even though no article uses
     * that word.
     */
    private final List<Article> index = new ArrayList<>();

    /** Every word that really exists in the index - used for "did you mean". */
    private final Set<String> vocabulary = new HashSet<>();

    public SearchEngine(DataLoader data) {
        this.data = data;
        buildIndex();
    }

    private static final int TITLE_WEIGHT = 5;   // a hit in the title matters more
    private static final int BODY_WEIGHT  = 1;

    private void buildIndex() {
        index.addAll(data.articles);

        for (Crop c : data.crops) {
            index.add(document("C-" + c.name, c.name + " - crop profile",
                    c.name + " is a " + c.season + " season crop. " + c.description
                    + " It grows in " + c.soil + " soil and needs " + c.waterNeed + " water. "
                    + "It needs " + c.n + " nitrogen " + c.p + " phosphorus " + c.k + " potassium. "
                    + "The crop takes " + c.duration + " from sowing to harvest. "
                    + "It prefers " + c.temperature + " and " + c.rainfall + " of rainfall a year. "
                    + "Recommended spacing is " + c.spacing + ". "
                    + "Popular varieties are " + c.varieties + ". "
                    + "Typical yield is " + c.yield + ". "
                    + "Common diseases of " + c.name + " are " + String.join(", ", c.diseases) + "."));
        }
        for (Disease d : data.diseases) {
            index.add(document("D-" + d.name, d.name + " - disease profile",
                    d.name + " attacks " + String.join(", ", d.crops)
                    + ". Symptoms are " + String.join(", ", d.symptoms)
                    + ". Treatment: " + d.treatment));
        }
        for (Fertilizer f : data.fertilizers) {
            index.add(document("F-" + f.name, f.name + " - fertilizer profile",
                    f.name + " is a fertilizer with npk " + f.n + "-" + f.p + "-" + f.k
                    + ". One bag costs " + f.cost + " rupees. It suits "
                    + String.join(", ", f.suited) + "."));
        }
        for (Pest p : data.pests) {
            index.add(document("P-" + p.name, p.name + " - pest profile",
                    p.name + " attacks " + String.join(", ", p.crops)
                    + ". The damage symptoms are " + String.join(", ", p.damage)
                    + ". Control: " + p.control));
        }

        for (Article a : index) {
            addWords(a.lowerTitle());
            addWords(a.lowerBody());
        }
    }

    private void addWords(String text) {
        for (String w : text.split("[^a-z0-9]+")) if (w.length() >= 4) vocabulary.add(w);
    }

    private static Article document(String id, String title, String body) {
        Article a = new Article();
        a.id = id; a.title = title; a.body = body;
        return a;
    }

    public int indexSize() { return index.size(); }

    /** Rank everything in the index for the given search terms. */
    public List<SearchResult> search(List<String> terms) {
        return search(terms, true);
    }

    /** Search only the real articles - used for the "read more" suggestions. */
    public List<SearchResult> searchArticlesOnly(List<String> terms) {
        return search(terms, false);
    }

    private List<SearchResult> search(List<String> terms, boolean includeProfiles) {
        List<SearchResult> results = new ArrayList<>();
        if (terms.isEmpty()) return results;

        int totalScans = 0;
        List<Article> corpus = includeProfiles ? index : data.articles;

        for (Article a : corpus) {
            int score = 0;
            String bestTerm = null;
            int bestTermScore = 0;

            for (String term : terms) {
                // ---------- ALGORITHM 2 : RABIN-KARP counts the occurrences ----------
                int titleHits = RabinKarp.count(a.lowerTitle(), term);
                int bodyHits  = RabinKarp.count(a.lowerBody(),  term);
                totalScans += 2;

                int termScore = titleHits * TITLE_WEIGHT + bodyHits * BODY_WEIGHT;
                score += termScore;
                if (termScore > bestTermScore) { bestTermScore = termScore; bestTerm = term; }
            }

            if (score > 0) {
                // ---------- ALGORITHM 1 : KMP finds the exact position ----------
                String snippet = snippetAround(a.body, bestTerm);
                results.add(new SearchResult(a, score, snippet, bestTerm));
            }
        }

        Trace.log("Rabin-Karp", "rolling hash scanned " + corpus.size()
                + " documents (" + totalScans + " passes), " + results.size() + " matched");

        if (!results.isEmpty()) {
            Trace.log("KMP", "found the exact position of the best term to cut the snippet");
        }

        // ---------- ALGORITHM 8 : RANDOMIZED QUICKSORT ranks the results ----------
        RandomizedQuickSort.sort(results, Comparator.comparingInt((SearchResult r) -> -r.score));
        Trace.log("Random QuickSort", "ranked " + results.size()
                + " results by relevance score (highest first)");

        return results;
    }

    /**
     * Uses KMP to find where the term sits inside the article body and returns
     * a readable window of text around that position.
     */
    private String snippetAround(String body, String term) {
        if (term == null) return firstSentence(body);

        int pos = KMP.search(body.toLowerCase(), term);      // <-- KMP
        if (pos < 0) return firstSentence(body);

        int start = Math.max(0, pos - 70);
        int end   = Math.min(body.length(), pos + 150);

        while (start > 0 && body.charAt(start) != ' ') start--;
        while (end < body.length() && body.charAt(end) != ' ') end++;

        String s = body.substring(start, end).trim();
        if (start > 0) s = "..." + s;
        if (end < body.length()) s = s + "...";
        return s;
    }

    private String firstSentence(String body) {
        int dot = body.indexOf('.');
        return dot > 0 ? body.substring(0, dot + 1) : body;
    }

    // ------------------------------------------------------------------------
    // RELATED ARTICLES  ->  ALGORITHM 5 : SUFFIX ARRAY + LCP
    // ------------------------------------------------------------------------

    public static class Related {
        public Article article;
        public int similarity;      // length of the longest common substring
        public String sharedText;
        Related(Article a, int s, String t) { article = a; similarity = s; sharedText = t; }
    }

    /**
     * Similarity between two articles = length of their LONGEST COMMON SUBSTRING,
     * computed with a suffix array plus its LCP array.
     */
    public List<Related> relatedArticles(Article base, int howMany) {
        List<Related> list = new ArrayList<>();

        for (Article other : data.articles) {   // only real articles are worth reading
            if (other.id.equals(base.id)) continue;

            String shared = SuffixArrayLCP.longestCommonSubstring(
                    base.lowerBody(), other.lowerBody());      // <-- suffix array + LCP

            if (shared.trim().length() >= 15) {   // ignore tiny meaningless fragments
                list.add(new Related(other, shared.trim().length(), shared.trim()));
            }
        }

        Trace.log("Suffix Array+LCP", "suffix array of \"" + base.id + "\" vs "
                + (data.articles.size() - 1) + " documents, longest shared phrase");

        // rank the related articles too - QuickSort again
        RandomizedQuickSort.sort(list, Comparator.comparingInt((Related r) -> -r.similarity));

        return list.size() > howMany ? list.subList(0, howMany) : list;
    }

    // ------------------------------------------------------------------------
    // "DID YOU MEAN"  ->  ALGORITHM 4 : EDIT DISTANCE, second use
    // ------------------------------------------------------------------------

    /**
     * The search found nothing. Before giving up we take every word of the
     * question and look for the closest word that REALLY EXISTS in our index.
     * "brinjaal" has no record, but "brinjal" might be two edits from "banana"
     * so we only accept a genuinely close word.
     */
    public List<String> suggestTerms(List<String> terms) {
        List<String> suggestions = new ArrayList<>();

        for (String term : terms) {
            if (term.length() < 4 || vocabulary.contains(term)) continue;

            int maxDistance = (term.length() <= 6) ? 1 : 2;
            String near = EditDistance.bestMatch(term, vocabulary, maxDistance);

            if (near != null && !near.equals(term) && !suggestions.contains(near)) {
                suggestions.add(near);
                Trace.log("Edit Distance", "no record for \"" + term
                        + "\", closest word in the index is \"" + near + "\"");
            }
        }
        return suggestions;
    }

    /** A few topics the farmer can actually ask about - used as a last resort. */
    public List<String> sampleTopics() {
        List<String> topics = new ArrayList<>();
        for (Crop c : data.crops) topics.add(c.name);
        return topics;
    }

    /** When nothing was detected, fall back to the plain words of the question. */
    public static List<String> fallbackTerms(String query) {
        List<String> terms = new ArrayList<>();
        for (String w : query.toLowerCase().split("[^a-z0-9]+")) {
            if (w.length() >= 4 && !isStopWord(w)) terms.add(w);
        }
        return terms;
    }

    private static boolean isStopWord(String w) {
        String[] stop = {"what", "which", "when", "where", "this", "that", "with",
                         "have", "does", "will", "from", "your", "tell", "give",
                         "about", "please", "should", "would", "there", "their"};
        for (String s : stop) if (s.equals(w)) return true;
        return false;
    }
}
