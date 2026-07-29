package com.worklyze.supportiq.feature.ingestion.adapter;

import com.worklyze.supportiq.feature.ingestion.domain.usecases.PdfIngestionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @io.swagger.v3.oas.annotations.Operation(summary = "Ingest PDF file")
    public ResponseEntity<Void> ingest(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MULTIPART_FORM_DATA_VALUE
                    )
            )
            @RequestParam("file") MultipartFile file
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            pdfIngestionUseCase.execute(file.getOriginalFilename(), inputStream);
            return ResponseEntity.noContent().build();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar arquivo: " + file.getOriginalFilename(), e);
        }
    }
}