package org.pdfImageVerifier.service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private final Tesseract tesseract;
    private final LevenshteinDistance levenshtein;

    public PdfService() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(LoadLibs.extractTessResources("tessdata").getAbsolutePath());
        this.tesseract.setLanguage("eng");
        this.levenshtein = new LevenshteinDistance();
    }

    public File verifyImageTextInPdfWithRangeAsPng(MultipartFile imageFile, MultipartFile pdfFile,
                                                   int startPage, int endPage,
                                                   Set<String> ignorePatterns,
                                                   String cashFlowDetails) throws Exception {

        // Step 1: OCR on uploaded image
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        List<Word> imageWords = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

        // Step 2: Extract OCR words from PDF pages
        Set<String> pdfWords = new HashSet<>();
        try (InputStream pdfInput = pdfFile.getInputStream(); PDDocument pdfDoc = PDDocument.load(pdfInput)) {
            PDFRenderer renderer = new PDFRenderer(pdfDoc);
            for (int i = startPage - 1; i < Math.min(endPage, pdfDoc.getNumberOfPages()); i++) {
                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);
                String ocrText = tesseract.doOCR(pageImage);
                Set<String> words = Arrays.stream(ocrText.split("\\W+"))
                        .map(this::cleanWord)
                        .filter(w -> !w.isBlank())
                        .filter(w -> !shouldIgnore(w, ignorePatterns))
                        .collect(Collectors.toSet());
                pdfWords.addAll(words);
            }
        }

        // Step 3: Draw rectangles around unmatched words in image
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.setStroke(new BasicStroke(2f));

        for (Word word : imageWords) {
            String clean = cleanWord(word.getText());
            if (clean.isBlank() || shouldIgnore(clean, ignorePatterns)) continue;

            boolean matched = fuzzyMatch(clean, pdfWords, cashFlowDetails);
            if (!matched) {
                Rectangle rect = word.getBoundingBox();
                graphics.drawRect(rect.x, rect.y, rect.width, rect.height);
            }
        }

        graphics.dispose();

        // Step 4: Save result image as PNG
        File resultImage = File.createTempFile("comparison_result", ".png");
        ImageIO.write(image, "png", resultImage);
        return resultImage;
    }

    private String cleanWord(String word) {
        return word.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private boolean shouldIgnore(String word, Set<String> ignorePatterns) {
        for (String pattern : ignorePatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(word).matches()) return true;
        }
        return false;
    }

    private boolean fuzzyMatch(String word, Set<String> pageWords, String cashFlowDetails) {
        if (pageWords.contains(word)) return true;
        if (cashFlowDetails.equalsIgnoreCase("Yes")) {
            String[] splitWord = word.split("'");
            int firstDigitAfterColon = Character.getNumericValue(splitWord[1].charAt(0));
            int valueToComapreWithPDF = Integer.parseInt(splitWord[0]);
            if (firstDigitAfterColon >= 5) valueToComapreWithPDF++;
            // numeric
            for (String pdfWord : pageWords) {
                if (pdfWord.equals(String.valueOf(valueToComapreWithPDF))) return true;
            }
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
