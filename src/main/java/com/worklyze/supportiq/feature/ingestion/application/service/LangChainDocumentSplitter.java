package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedPage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LangChainDocumentSplitter
        implements DocumentSplitter {


    private final DocumentByParagraphSplitter splitter;


    public LangChainDocumentSplitter() {
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


            Document langDocument =
                    Document.from(
                            page.text(),
                            Metadata.from(
                                    Map.of(
                                            "fileName",
                                            document.fileName(),

                                            "page",
                                            page.pageNumber()
                                    )
                            )
                    );


            segments.addAll(
                    splitter.split(langDocument)
            );
        }


        return segments;
    }
}