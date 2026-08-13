package model;

/** One row of data/articles.csv - these are the documents FarmAssist searches. */
public class Article {
    public String id;
    public String title;
    public String body;

    public String lowerTitle() { return title.toLowerCase(); }
    public String lowerBody()  { return body.toLowerCase();  }
}
