package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedPage;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.document.splitter.DocumentBySentenceSplitter;
import dev.langchain4j.data.segment.TextSegment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LangChainDocumentSplitter
        implements DocumentSplitter {

    // Contexto por chunk. ~2000 chars ≈ 500 tokens: bom para embeddings modernos
    // (text-embedding-3-*, BGE, Cohere) sem estourar limites.
    private static final int MAX_CHUNK_CHARS = 2000;
    private static final int MAX_CHUNK_OVERLAP_CHARS = 400;

    private final DocumentByParagraphSplitter splitter;
    private final ImageStorageService imageStorageService;


    public LangChainDocumentSplitter(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
        // Sub-splitter por sentença evita exceção quando um parágrafo excede o
        // tamanho máximo do segmento (comum em PDFs técnicos/jurídicos).
        this.splitter =
                new DocumentByParagraphSplitter(
                        MAX_CHUNK_CHARS,
                        MAX_CHUNK_OVERLAP_CHARS,
                        new DocumentBySentenceSplitter(
                                MAX_CHUNK_CHARS,
                                MAX_CHUNK_OVERLAP_CHARS
                        )
                );
    }


    @Override
    public List<TextSegment> split(
            ParsedDocument document
    ) {

        List<TextSegment> segments = new ArrayList<>();

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

            if (page.hasText()) {
                Document langDocument = Document.from(
                        page.text(),
                        Metadata.from(metadata)
                );

                segments.addAll(splitter.split(langDocument));
            } else if (page.hasImages()) {
                // Página só com imagens (PDF escaneado / diagramas): cria um
                // segmento sintético para que as imagens fiquem indexadas e
                // recuperáveis via busca vetorial, evitando arquivos órfãos.
                String placeholder = "[Página "
                        + page.pageNumber()
                        + " de "
                        + document.fileName()
                        + " contém apenas imagens.]";

                segments.add(TextSegment.from(placeholder, Metadata.from(metadata)));
            }
        }


        return segments;
    }

}
