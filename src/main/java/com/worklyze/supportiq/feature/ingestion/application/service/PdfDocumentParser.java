package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.DocumentParser;
import com.worklyze.supportiq.feature.ingestion.shared.KnowledgeImage;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedPage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfDocumentParser implements DocumentParser {

    @Override
    public ParsedDocument parse(String fileName, InputStream inputStream) {

        try (PDDocument document = Loader.loadPDF(inputStream.readAllBytes())) {

            List<ParsedPage> pages = new ArrayList<>();

            for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {

                PDPage page = document.getPage(pageNumber - 1);

                String text = extractText(document, pageNumber);

                List<KnowledgeImage> images = extractImages(page);

                pages.add(new ParsedPage(
                        pageNumber,
                        text,
                        images
                ));
            }

            return new ParsedDocument(fileName, pages);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar PDF: " + fileName, e);
        }
    }

    private String extractText(PDDocument document, int pageNumber) throws IOException {

        PDFTextStripper stripper = new PDFTextStripper();

        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);

        String text = stripper.getText(document);

        return text == null ? "" : text.trim();
    }

    private List<KnowledgeImage> extractImages(PDPage page) throws IOException {

        List<KnowledgeImage> images = new ArrayList<>();

        PDResources resources = page.getResources();

        for (COSName name : resources.getXObjectNames()) {

            PDXObject object = resources.getXObject(name);

            if (object instanceof PDImageXObject imageObject) {

                BufferedImage bufferedImage = imageObject.getImage();

                ByteArrayOutputStream output = new ByteArrayOutputStream();

                ImageIO.write(bufferedImage, "png", output);

                images.add(new KnowledgeImage(
                        name.getName() + ".png",
                        output.toByteArray()
                ));
            }
        }

        return images;
    }
}