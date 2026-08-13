import java.io.*;
import java.util.*;

public class RabinKarpSearch {

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
    }

    // Rabin-Karp Algorithm for Pattern Matching
    public static List<Integer> rabinKarpSearch(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n < m) return positions;

        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();

        int d = 256; // Number of characters in input alphabet
        int q = 101; // A prime number
        int h = 1;

        // The value of h would be "pow(d, m-1) % q"
        for (int i = 0; i < m - 1; i++) {
            h = (h * d) % q;
        }

        int p = 0; // Hash value for pattern
        int t = 0; // Hash value for text window

        // Calculate initial hash values
        for (int i = 0; i < m; i++) {
            p = (d * p + lowerPattern.charAt(i)) % q;
            t = (d * t + lowerText.charAt(i)) % q;
        }

        // Slide the pattern over text
        for (int i = 0; i <= n - m; i++) {
            // If hash values match, check characters one by one
            if (p == t) {
                boolean match = true;
                for (int j = 0; j < m; j++) {
                    if (lowerText.charAt(i + j) != lowerPattern.charAt(j)) {
                        match = false;
                        break;
                    }
                }
                if (match) {
                    positions.add(i);
                }
            }

            // Calculate hash value for next window
            if (i < n - m) {
                t = (d * (t - lowerText.charAt(i) * h) + lowerText.charAt(i + m)) % q;

                // Handle negative values
                if (t < 0) {
                    t = t + q;
                }
            }
        }

        return positions;
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
        System.out.println("       TEXTHACK RABIN-KARP SEARCH");
        System.out.println("=====================================");
        System.out.print("Enter keyword to search : ");
        String keyword = sc.nextLine().trim();
        System.out.println();

        System.out.println("=====================================");
        System.out.println("       RABIN-KARP PATTERN SEARCH");
        System.out.println("=====================================");

        boolean found = false;
        for (int k = 0; k < repository.size(); k++) {
            Article article = repository.get(k);
            List<Integer> matches = rabinKarpSearch(article.content, keyword);

            if (!matches.isEmpty()) {
                found = true;
                System.out.println("Article ID : " + article.id);
                System.out.println("Title     : " + article.title);
                for (int pos : matches) {
                    System.out.println("Pattern found at position : " + pos);
                }
                System.out.println("Total occurrences : " + matches.size());
                System.out.println("----------------------------------------");
            }
        }

        if (!found) {
            System.out.println("No pattern matches found.");
        }

        sc.close();
    }
}
