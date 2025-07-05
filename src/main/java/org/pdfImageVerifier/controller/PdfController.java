package org.pdfImageVerifier.controller;


import org.pdfImageVerifier.service.PdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class PdfController {

    @Autowired
    private PdfService pdfService;

    @PostMapping("/compare-image-text")
    public ResponseEntity<Resource> verifyImageText(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam("startPage") int startPage,
            @RequestParam("endPage") int endPage) throws Exception {

        File resultPdf = pdfService.verifyImageTextInPdfWithRange(imageFile, pdfFile, startPage, endPage);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(resultPdf));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=comparison-result.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(resultPdf.length())
                .body(resource);
    }
}