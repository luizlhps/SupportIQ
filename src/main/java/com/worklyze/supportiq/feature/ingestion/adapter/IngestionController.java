package com.worklyze.supportiq.feature.ingestion.adapter;

import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.feature.ingestion.domain.usecases.PdfIngestionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;


@RestController
@RequestMapping("/v1/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final PdfIngestionUseCase pdfIngestionUseCase;

    @PostMapping(value = "/pdf", consumes = MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Ingest PDF file",
            description = "Faz o upload de um PDF, extrai texto/imagens, gera embeddings "
                    + "e indexa no vector store correspondente ao provider selecionado. "
                    + "Se o mesmo fileName ja tiver sido ingerido, os embeddings e imagens "
                    + "antigos sao removidos antes."
    )
    public ResponseEntity<Void> ingest(
            @Parameter(
                    description = "Arquivo PDF a ser ingerido.",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MULTIPART_FORM_DATA_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file,

            @Parameter(
                    description = "Provider de IA a ser usado para gerar os embeddings. "
                            + "Se omitido, usa o default configurado em supportiq.ai.default-provider.",
                    schema = @Schema(implementation = AiProvider.class)
            )
            @RequestParam(value = "provider", required = false) AiProvider provider
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            pdfIngestionUseCase.execute(provider, file.getOriginalFilename(), inputStream);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar arquivo: " + file.getOriginalFilename(), e);
        }
    }
}