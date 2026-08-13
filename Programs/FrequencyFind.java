import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FrequencyFind {

    public static void main(String[] args) {

        String fileName = "similarity_find.txt";
        String searchText = "Spring Boot";

        int frequency = 0;

        try (BufferedReader br =
                     new BufferedReader(new FileReader(fileName))) {

            String line;

            while ((line = br.readLine()) != null) {

                int index = 0;

                while ((index = line.indexOf(searchText, index)) != -1) {
                    frequency++;
                    index = index + searchText.length();
                }
            }

            System.out.println("Text: " + searchText);
            System.out.println("Frequency: " + frequency);

        } catch (IOException e) {
            System.out.println("File error: " + e.getMessage());
        }
    }
}