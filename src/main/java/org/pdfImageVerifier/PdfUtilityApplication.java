package org.pdfImageVerifier;

import org.pdfImageVerifier.util.ImageIOInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PdfUtilityApplication {
    public static void main(String[] args) {
        ImageIOInitializer.registerJPEG2000();
        SpringApplication.run(PdfUtilityApplication.class, args);
    }
}

