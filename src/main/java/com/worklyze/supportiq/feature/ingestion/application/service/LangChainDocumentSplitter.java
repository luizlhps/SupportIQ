package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedPage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LangChainDocumentSplitter
        implements DocumentSplitter {


    private final DocumentByParagraphSplitter splitter;
    private final ImageStorageService imageStorageService;


    public LangChainDocumentSplitter(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
        this.splitter =
                new DocumentByParagraphSplitter(
                        1000,
                        200
                );
    }


    @Override
    public List<TextSegment> split(
            ParsedDocument document
    ) {

        List<TextSegment> segments =
                new ArrayList<>();

        for (ParsedPage page : document.pages()) {


            Map<String, Object> metadata = new HashMap<>(
                    Map.of(
                            "fileName",
                            document.fileName(),

                            "page",
                            page.pageNumber()
                    )
            );

            if (page.hasImages()) {
                List<String> imagePaths = imageStorageService.store(
                        document.fileName(),
                        page.pageNumber(),
                        page.images()
                );
                metadata.put("images", String.join(",", imagePaths));
                metadata.put("imageCount", page.images().size());
            }

            Document langDocument =
                    Document.from(
                            page.text(),
                            Metadata.from(metadata)
                    );


            segments.addAll(
                    splitter.split(langDocument)
            );
        }


        return segments;
    }

}