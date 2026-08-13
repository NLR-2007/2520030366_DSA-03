import java.io.*;
import java.util.*;

public class PatternSearch {

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

    // Naive Pattern Matching Algorithm
    public static List<Integer> naiveSearch(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n < m) return positions;

        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();

        for (int i = 0; i <= n - m; i++) {
            int j;
            for (j = 0; j < m; j++) {
                if (lowerText.charAt(i + j) != lowerPattern.charAt(j)) {
                    break;
                }
            }
            if (j == m) {
                positions.add(i);
            }
        }
        return positions;
    }

    // Helper method to compute LPS array for KMP
    public static int[] computeLPSArray(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        lps[0] = 0;

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    // Knuth-Morris-Pratt (KMP) Pattern Matching Algorithm
    public static List<Integer> kmpSearch(String text, String pattern) {
        List<Integer> positions = new ArrayList<>();
        int n = text.length();
        int m = pattern.length();

        if (m == 0 || n < m) return positions;

        String lowerText = text.toLowerCase();
        String lowerPattern = pattern.toLowerCase();

        int[] lps = computeLPSArray(lowerPattern);

        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < n) {
            if (lowerPattern.charAt(j) == lowerText.charAt(i)) {
                i++;
                j++;
            }
            if (j == m) {
                positions.add(i - j);
                j = lps[j - 1];
            } else if (i < n && lowerPattern.charAt(j) != lowerText.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
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
        System.out.println("       TEXTHACK PATTERN SEARCH");
        System.out.println("=====================================");
        System.out.print("Enter keyword to search : ");
        String keyword = sc.nextLine().trim();
        System.out.println();

        // Naive Search Output
        System.out.println("=====================================");
        System.out.println("       NAIVE PATTERN MATCHING");
        System.out.println("=====================================");

        for (Article article : repository) {
            List<Integer> naiveMatches = naiveSearch(article.content, keyword);
            if (!naiveMatches.isEmpty()) {
                System.out.println("Article ID : " + article.id);
                System.out.println("Title     : " + article.title);
                for (int pos : naiveMatches) {
                    System.out.println("Pattern found at position : " + pos);
                }
                System.out.println("Total occurrences : " + naiveMatches.size());
            }
        }

        // KMP Search Output
        System.out.println("=====================================");
        System.out.println("       KMP PATTERN MATCHING");
        System.out.println("=====================================");

        for (Article article : repository) {
            List<Integer> kmpMatches = kmpSearch(article.content, keyword);
            if (!kmpMatches.isEmpty()) {
                System.out.println("Article ID : " + article.id);
                System.out.println("Title     : " + article.title);
                for (int pos : kmpMatches) {
                    System.out.println("Pattern found at position : " + pos);
                }
                System.out.println("Total occurrences : " + kmpMatches.size());
            }
        }

        sc.close();
    }
}
