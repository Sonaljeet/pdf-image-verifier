import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.util.Set;

public class Test {

    private final Tesseract tesseract;
    private static LevenshteinDistance levenshtein;

    public Test() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(LoadLibs.extractTessResources("tessdata").getAbsolutePath());
        this.tesseract.setLanguage("eng");
        this.levenshtein = new LevenshteinDistance();
    }

    public static void main(String[] args) {
        String word = "1'000";
        Set<String> s = Set.of("dcddd","1","abcdef");
        int numericThreshold = 3;
        System.out.println(fuzzyMatch(word, s, numericThreshold));
    }

    private static boolean fuzzyMatch(String word, Set<String> pageWords, int numericThreshold) {
        if (pageWords.contains(word)) return true;

        String [] splitWord = word.split("'");
        int firstDigitAfterColon = Character.getNumericValue(splitWord[1].charAt(0));
        int valueToComapreWithPDF = Integer.parseInt(splitWord[0]);
        if(firstDigitAfterColon>=5) valueToComapreWithPDF++;
          // numeric
            for (String pdfWord : pageWords) {
                if(pdfWord.equals(String.valueOf(valueToComapreWithPDF))) return true;
            }

        // Fuzzy match for non-numeric words
        for (String pw : pageWords) {
            int threshold = word.length() <= 4 ? 1 : word.length() <= 6 ? 2 : 3;
            if (levenshtein.apply(word, pw) <= threshold) {
                return true;
            }
        }
        return false;
    }
}
Set<String> words = Arrays.stream(ocrText.split("[^\\w']+"))
    .map(this::cleanWord)
    .filter(w -> !w.isBlank())
    .filter(w -> !shouldIgnore(w, ignorePatterns))
    .collect(Collectors.toSet());

private String cleanWord(String word) {
    return word.toLowerCase().replaceAll("[^a-z0-9']", "");
}

