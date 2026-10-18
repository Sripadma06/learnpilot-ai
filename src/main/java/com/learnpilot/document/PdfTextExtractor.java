package com.learnpilot.document;

import com.learnpilot.learning.Passage;
import com.learnpilot.learning.TextChunker;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Extracts text page by page with Apache PDFBox and chunks it for retrieval and quiz generation. */
@Component
public class PdfTextExtractor {
    public record Result(int pageCount, List<Passage> passages) {}

    public Result extract(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            int pages = doc.getNumberOfPages();
            List<Passage> passages = new ArrayList<>();
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            // Lower tolerances so words are not glued together ("Optionalin", "onNULL").
            stripper.setSpacingTolerance(0.3f);
            stripper.setAverageCharTolerance(0.3f);
            for (int p = 1; p <= pages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                passages.addAll(TextChunker.chunkPage(p, stripper.getText(doc)));
            }
            return new Result(pages, passages);
        }
    }
}
