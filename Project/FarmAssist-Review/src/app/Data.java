package app;

import algo.AhoCorasick;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the three data files and builds the Aho-Corasick automaton.
 *
 * The files are plain text with a vertical bar between the columns. A bar was
 * used instead of a comma because the symptom and crop columns already have
 * commas inside them, and this way the parser stays a single split().
 */
public class Data {

    /** One row of diseases.txt. */
    public static class Disease {
        public String name;
        public List<String> crops = new ArrayList<>();
        public List<String> symptoms = new ArrayList<>();
        public String treatment;

        /** Everything KMP searches through, as one lowercase string. */
        public String symptomText() {
            return String.join(" , ", symptoms).toLowerCase();
        }
    }

    /** One row of articles.txt. */
    public static class Article {
        public String id;
        public String title;
        public String body;

        public String searchText() {
            return (title + " " + body).toLowerCase();
        }
    }

    public List<Disease> diseases = new ArrayList<>();
    public List<Article> articles = new ArrayList<>();
    public AhoCorasick keywords = new AhoCorasick();

    /**
     * Every word the spell checker may correct a typo into. Aho-Corasick keeps
     * its patterns inside the trie where they cannot be walked, and edit
     * distance needs a plain list to measure against, so the words are kept
     * here as well.
     *
     * A pattern like "brown spots" is stored whole and also as "brown" and
     * "spots" on their own. The farmer types one word at a time, so "sopts"
     * has to find "spots"; putting the single word back into the sentence
     * rebuilds "brown spots", which the automaton then matches as a phrase.
     */
    public List<String> vocabulary = new ArrayList<>();

    public void loadAll(String folder) {
        if (folder == null || folder.trim().isEmpty()) folder = "data";
        folder = folder.trim();

        loadKeywords(folder + "/keywords.txt");
        loadDiseases(folder + "/diseases.txt");
        loadArticles(folder + "/articles.txt");

        try {
            keywords.build();
        } catch (RuntimeException e) {
            System.out.println(Ui.paint("  could not build the keyword automaton", Ui.DISEASE)
                    + Ui.muted(" : " + e.getMessage()));
        }
    }

    private void loadKeywords(String path) {
        for (String[] parts : readRows(path, 2)) {
            String word = parts[0].toLowerCase();
            if (word.isEmpty()) continue;

            keywords.addPattern(word, parts[1].toUpperCase());
            remember(word);

            // a multi word pattern also contributes its own words
            if (word.indexOf(' ') >= 0) {
                for (String piece : word.split("\\s+")) {
                    if (piece.length() >= 3) remember(piece);
                }
            }
        }
    }

    private void remember(String word) {
        if (!vocabulary.contains(word)) vocabulary.add(word);
    }

    private void loadDiseases(String path) {
        for (String[] parts : readRows(path, 4)) {
            Disease d = new Disease();
            d.name = parts[0].toLowerCase();
            d.crops = splitList(parts[1]);
            d.symptoms = splitList(parts[2]);
            d.treatment = parts[3];
            diseases.add(d);
        }
    }

    private void loadArticles(String path) {
        for (String[] parts : readRows(path, 3)) {
            Article a = new Article();
            a.id = parts[0];
            a.title = parts[1];
            a.body = parts[2];
            articles.add(a);
        }
    }

    /**
     * Reads a bar separated file and hands back the rows that have at least
     * the number of columns we expect. Comments and blank lines are skipped.
     */
    private List<String[]> readRows(String path, int columns) {
        List<String[]> rows = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(path), StandardCharsets.UTF_8))) {

            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("\\|");
                if (parts.length < columns) {
                    System.out.println(Ui.muted("  skipped " + path + " line " + lineNo
                            + " (wanted " + columns + " columns, found " + parts.length + ")"));
                    continue;
                }
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                }
                rows.add(parts);
            }
        } catch (IOException e) {
            // a missing or unreadable file is reported and the rest still loads
            System.out.println(Ui.paint("  could not read " + path, Ui.DISEASE)
                    + Ui.muted(" : " + e.getMessage()));
        } catch (RuntimeException e) {
            System.out.println(Ui.paint("  " + path + " is not readable as data", Ui.DISEASE)
                    + Ui.muted(" : " + e));
        }
        return rows;
    }

    /** "a, b, c" to a list, dropping anything empty. */
    private List<String> splitList(String field) {
        List<String> out = new ArrayList<>();
        for (String piece : field.split(",")) {
            piece = piece.trim();
            if (!piece.isEmpty()) out.add(piece);
        }
        return out;
    }
}
