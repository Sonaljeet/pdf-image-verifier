package org.pdfImageVerifier.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleAllExceptions(Exception ex, Model model) {
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        String errorDetails = sw.toString();

        model.addAttribute("verificationResult",
                "<strong style='color:red;'>❌ An unexpected error occurred:</strong><br><pre style='color:#c00;background:#f8f8f8;padding:10px;border:1px solid #ddd;overflow:auto;'>"
                        + errorDetails + "</pre>");

        return "index";
    }
}
