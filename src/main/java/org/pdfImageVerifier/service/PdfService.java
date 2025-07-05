package org.pdfImageVerifier.service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import net.sourceforge.tess4j.util.LoadLibs;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfService {

    private final ITesseract tesseract;
    private final LevenshteinDistance levenshtein;

    public PdfService() {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(LoadLibs.extractTessResources("tessdata").getAbsolutePath());
        this.tesseract.setLanguage("eng");
        this.levenshtein = new LevenshteinDistance();
    }

    public String extractTextFromImage(MultipartFile imageFile) throws Exception {
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        return tesseract.doOCR(image);
    }

    private boolean fuzzyMatch(String word, Set<String> pageWords) {
        word = word.toLowerCase().replaceAll("\\W", "");
        if (pageWords.contains(word)) return true;
        for (String pw : pageWords) {
            int threshold = word.length() <= 4 ? 1 : word.length() <= 6 ? 2 : 3;
            if (levenshtein.apply(word, pw) <= threshold) return true;
        }
        return false;
    }

    public File verifyImageTextInPdfWithRange(MultipartFile imageFile, MultipartFile pdfFile, int startPage, int endPage) throws Exception {
        BufferedImage image = ImageIO.read(imageFile.getInputStream());
        List<Word> imageWords = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);


        File resultPdf = File.createTempFile("comparison_result", ".pdf");
        try (InputStream pdfInput = pdfFile.getInputStream();
             PDDocument pdfDoc = PDDocument.load(pdfInput);
             PDDocument outputDoc = new PDDocument()) {

            PDFRenderer renderer = new PDFRenderer(pdfDoc);
            for (int i = startPage - 1; i < Math.min(endPage, pdfDoc.getNumberOfPages()); i++) {
                BufferedImage pdfPageImage = renderer.renderImageWithDPI(i, 300);

                String pageText = tesseract.doOCR(pdfPageImage);
                Set<String> pageWords = Arrays.stream(pageText.split("\\W+"))
                        .map(w -> w.toLowerCase().replaceAll("\\W", ""))
                        .collect(Collectors.toSet());

                PDPage newPage = new PDPage(PDRectangle.LETTER);
                outputDoc.addPage(newPage);

                PDImageXObject pdImage = PDImageXObject.createFromFileByContent(convertToFile(pdfPageImage), outputDoc);

                try (PDPageContentStream content = new PDPageContentStream(outputDoc, newPage)) {
                    content.drawImage(pdImage, 0, 0, PDRectangle.LETTER.getWidth(), PDRectangle.LETTER.getHeight());
                    for (Word word : imageWords) {
                        String clean = word.getText().toLowerCase().replaceAll("\\W", "");
                        boolean matched = fuzzyMatch(clean, pageWords);
                        if (!matched) {
                            content.setStrokingColor(Color.RED);
                            content.setLineWidth(1f);
                            content.addRect((float) word.getBoundingBox().getX(),
                                    (float) (PDRectangle.LETTER.getHeight() - word.getBoundingBox().getY() - word.getBoundingBox().getHeight()),
                                    (float) word.getBoundingBox().getWidth(),
                                    (float) word.getBoundingBox().getHeight());
                            content.stroke();
                        }
                    }
                }
            }
            outputDoc.save(resultPdf);
        }
        return resultPdf;
    }

    private File convertToFile(BufferedImage image) throws IOException {
        File tempFile = File.createTempFile("page_image", ".png");
        ImageIO.write(image, "png", tempFile);
        return tempFile;
    }

//    public File verifyImageTextInPdfAndGenerateReport(MultipartFile imageFile, MultipartFile pdfFile) throws Exception {
//        String imageText = extractTextFromImage(imageFile).replaceAll("[^\\p{L}\\p{N}]+", " ").toLowerCase();
//        List<String> imageWords = Arrays.asList(imageText.split("\\s+"));
//
//        File tempPdf = File.createTempFile("comparison-result", ".pdf");
//        try (InputStream pdfInput = pdfFile.getInputStream();
//             PDDocument pdfDocument = PDDocument.load(pdfInput);
//             PDDocument outputDoc = new PDDocument()) {
//
//            PDFRenderer renderer = new PDFRenderer(pdfDocument);
//            Tesseract tesseract = new Tesseract();
//            tesseract.setDatapath(LoadLibs.extractTessResources("tessdata").getAbsolutePath());
//            tesseract.setLanguage("eng");
//            LevenshteinDistance levenshtein = new LevenshteinDistance();
//
//            for (int i = 0; i < pdfDocument.getNumberOfPages(); i++) {
//                BufferedImage pageImage = renderer.renderImageWithDPI(i, 300);
//                String pdfPageText = tesseract.doOCR(pageImage).replaceAll("[^\\p{L}\\p{N}]+", " ").toLowerCase();
//
//                Set<String> pdfWords = new HashSet<>(Arrays.asList(pdfPageText.split("\\s+")));
//
//                PDPage newPage = new PDPage(PDRectangle.LETTER);
//                outputDoc.addPage(newPage);
//
//                try (PDPageContentStream content = new PDPageContentStream(outputDoc, newPage)) {
//                    content.setFont(PDType1Font.HELVETICA, 12);
//                    content.beginText();
//                    content.newLineAtOffset(50, 700);
//
//                    int lineLength = 0;
//                    for (String word : imageWords) {
//                        boolean matched = pdfWords.stream().anyMatch(w -> levenshtein.apply(w, word) <= 1);
//
//                        if (lineLength + word.length() > 80) {
//                            content.newLineAtOffset(0, -15);
//                            lineLength = 0;
//                        }
//
//                        content.setNonStrokingColor(matched ? Color.GREEN : Color.RED);
//                        content.showText(word + " ");
//                        lineLength += word.length() + 1;
//                    }
//
//                    content.endText();
//                }
//            }
//
//            outputDoc.save(tempPdf);
//        }
//
//        return tempPdf;
//    }


}