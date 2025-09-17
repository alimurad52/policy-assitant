package com.example.policyassistant.service;

import com.example.policyassistant.fragment.DocumentFragment;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfExtractionService {

    public List<DocumentFragment> extractFragments(Path pdfPath) throws IOException {
        List<DocumentFragment> fragments = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();
            for (int page = 1; page <= pageCount; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String pageText = stripper.getText(document);
                String[] lines = pageText.split("\\r?\\n");
                int lineNumber = 1;
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        DocumentFragment fragment = new DocumentFragment();
                        fragment.setPageNumber(page);
                        fragment.setLineStart(lineNumber);
                        fragment.setLineEnd(lineNumber);
                        fragment.setText(trimmed);
                        fragments.add(fragment);
                    }
                    lineNumber++;
                }
            }
        }
        return fragments;
    }
}
