import java.io.*;
import java.util.*;

public class QueryProcessor {

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
            System.out.println("----------------------------------------");
            System.out.println("Article ID : " + id);
            System.out.println("Title      : " + title);
            System.out.println("Word Count : " + wordCount);
            System.out.println("Content    : ");
            System.out.println(content);
            System.out.println("----------------------------------------");
        }
    }

    public static void main(String[] args) {

        ArrayList<Article> repository = new ArrayList<>();

        String[] files = {"a1.txt", "a2.txt", "a3.txt"};

        int id = 101;

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

                id++;

                br.close();

            } catch (IOException e) {
                System.out.println("Cannot read file : " + fileName);
            }
        }

        Scanner sc = new Scanner(System.in);

        System.out.println("=====================================");
        System.out.println("      TEXTHACK QUERY PROCESSOR");
        System.out.println("=====================================");
        System.out.print("Enter keyword to search : ");
        String keyword = sc.nextLine().trim();

        System.out.println();
        System.out.println("Matching Articles");

        boolean found = false;

        for (Article article : repository) {

            // Search keyword in title or content (case-insensitive)
            if (article.title.toLowerCase().contains(keyword.toLowerCase()) || 
                article.content.toLowerCase().contains(keyword.toLowerCase())) {

                article.display();
                found = true;
            }
        }

        if (!found) {
            System.out.println("No matching articles found.");
        }

        sc.close();
    }
}
