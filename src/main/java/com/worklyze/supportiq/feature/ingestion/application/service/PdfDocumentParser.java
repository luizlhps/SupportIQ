package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.DocumentParser;
import com.worklyze.supportiq.feature.ingestion.shared.IngestionExceptionCode;
import com.worklyze.supportiq.feature.ingestion.shared.KnowledgeImage;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedPage;
import com.worklyze.supportiq.shared.exceptions.InternalException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PdfDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parse(String fileName, InputStream inputStream) {

        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            List<ParsedPage> pages = new ArrayList<>();

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {

                PDPage page = document.getPage(pageNumber - 1);

                String text = extractText(document, pageNumber);

                List<KnowledgeImage> images = extractImages(page, pageNumber);

                pages.add(new ParsedPage(
                        pageNumber,
                        text,
                        images
                ));
            }

            return new ParsedDocument(fileName, pages);

        } catch (IOException e) {
            throw new InternalException(
                    IngestionExceptionCode.PDF_PROCESSING_ERROR.getMessage().formatted(fileName),
                    IngestionExceptionCode.PDF_PROCESSING_ERROR.getCode(),
                    e
            );
        }
    }

    private String extractText(PDDocument document, int pageNumber) throws IOException {

        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);

        String text = stripper.getText(document);

        return text == null ? "" : text.trim();
    }

    private List<KnowledgeImage> extractImages(PDPage page, int pageNumber) throws IOException {

        List<KnowledgeImage> images = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        collectImages(page.getResources(), images, visited, pageNumber);

        return images;
    }

    private void collectImages(
            PDResources resources,
            List<KnowledgeImage> images,
            Set<String> visited,
            int pageNumber
    ) throws IOException {

        if (resources == null) {
            return;
        }

        for (COSName name : resources.getXObjectNames()) {

            PDXObject object = resources.getXObject(name);

            if (object instanceof PDImageXObject imageObject) {

                // Deduplica imagens compartilhadas entre XObjects na mesma página.
                String key = System.identityHashCode(imageObject.getCOSObject()) + "-" + name.getName();
                if (!visited.add(key)) {
                    continue;
                }

                BufferedImage bufferedImage = imageObject.getImage();
                if (bufferedImage == null) {
                    continue;
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();

                boolean written = ImageIO.write(bufferedImage, "png", output);
                if (!written || output.size() == 0) {
                    // Sem writer PNG compatível para este BufferedImage: pula em vez
                    // de salvar arquivo vazio.
                    continue;
                }

                // Prefixa com página para evitar colisão de nomes ao armazenar.
                images.add(new KnowledgeImage(
                        "p" + pageNumber + "_" + name.getName() + ".png",
                        output.toByteArray()
                ));

            } else if (object instanceof PDFormXObject formObject) {
                // Varre Form XObjects aninhados (imagens embutidas em formulários).
                collectImages(formObject.getResources(), images, visited, pageNumber);
            }
        }
    }
}