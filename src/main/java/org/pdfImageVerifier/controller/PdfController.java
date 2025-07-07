package org.pdfImageVerifier.controller;

import org.pdfImageVerifier.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@RestController
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/compare-png")
    public ResponseEntity<byte[]> comparePdfAndImagePng(
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("startPage") int startPage,
            @RequestParam("endPage") int endPage,
            @RequestParam(value = "ignorePatterns", required = false) String ignorePatternStr,
            @RequestParam(value = "cashflow_details", defaultValue = "No") String cashFlowDetails) {

        try {
            Set<String> ignorePatterns = new HashSet<>();
            if (ignorePatternStr != null && !ignorePatternStr.isBlank()) {
                ignorePatterns = new HashSet<>(Arrays.asList(ignorePatternStr.split("\\s*,\\s*")));
            }

            File resultImage = pdfService.verifyImageTextInPdfWithRangeAsPng(
                    imageFile, pdfFile, startPage, endPage, ignorePatterns, cashFlowDetails);

            byte[] imageBytes = Files.readAllBytes(resultImage.toPath());
            resultImage.delete();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentDisposition(ContentDisposition.attachment().filename("comparison-result.png").build());

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
