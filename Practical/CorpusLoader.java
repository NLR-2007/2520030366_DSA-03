import java.io.*;
import java.util.*;

public class CorpusLoader {

    static class Article {
        int id;
        String title;
        String content;
        int wordCount;

        public Article(int id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;

            // Count words
            this.wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
        }

        public void display() {
            System.out.println("-------------------------------------------");
            System.out.println("Article ID : " + id);
            System.out.println("Title      : " + title);
            System.out.println("Word Count : " + wordCount);
            System.out.println("Content    : ");
            System.out.println(content);
            System.out.println("-------------------------------------------");
            System.out.println();
        }
    }

    public static void main(String[] args) {

        ArrayList<Article> repository = new ArrayList<>();

        String[] files = {"a1.txt", "a2.txt"};

        int id = 101;

        int totalWords = 0;

        for (String fileName : files) {

            try {

                File file = new File(fileName);
                if (!file.exists()) {
                    file = new File("corpus/" + fileName);
                }

                BufferedReader br = new BufferedReader(new FileReader(file));

                // Read title
                String title = br.readLine();

                // Skip blank line if present
                br.mark(1000);
                String firstLine = br.readLine();
                if (firstLine != null && !firstLine.trim().isEmpty()) {
                    br.reset();
                }

                StringBuilder content = new StringBuilder();

                String line;

                while ((line = br.readLine()) != null) {
                    if (content.length() > 0) {
                        content.append("\n");
                    }
                    content.append(line);
                }

                Article article = new Article(id, title != null ? title.trim() : "", content.toString().trim());

                repository.add(article);

                totalWords += article.wordCount;

                id++;

                br.close();

            } catch (IOException e) {
                System.out.println("Cannot read file : " + fileName);
            }
        }

        // Display Repository
        System.out.println("======================================");
        System.out.println("      TEXTHACK ARTICLE REPOSITORY");
        System.out.println("======================================");
        System.out.println();

        for (Article article : repository) {
            article.display();
        }

        System.out.println("Repository Statistics");
        System.out.println("----------------------");
        System.out.println("Total Articles Loaded : " + repository.size());
        System.out.println("Total Words           : " + totalWords);
    }
}
