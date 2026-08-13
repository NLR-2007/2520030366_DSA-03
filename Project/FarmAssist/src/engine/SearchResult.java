package engine;

import model.Article;

/** One ranked document produced by the search engine. */
public class SearchResult {
    public Article article;
    public int score;          // relevance, computed from Rabin-Karp hit counts
    public String snippet;     // text around the position that KMP located
    public String matchedTerm;

    public SearchResult(Article article, int score, String snippet, String matchedTerm) {
        this.article = article;
        this.score = score;
        this.snippet = snippet;
        this.matchedTerm = matchedTerm;
    }
}
