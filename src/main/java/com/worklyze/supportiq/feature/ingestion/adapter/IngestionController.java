package com.worklyze.supportiq.feature.ingestion.adapter;

import com.worklyze.supportiq.feature.ingestion.domain.usecases.PdfIngestionUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Controller
@RequiredArgsConstructor
public class IngestionController {

    private final PdfIngestionUseCase pdfIngestionUseCase;

    @PostMapping(value = "/ingest", consumes = { "multipart/form-data" })
    public ResponseEntity<Void> ingest(@RequestParam("file") MultipartFile file) throws IOException {

        try (InputStream inputStream = file.getInputStream()) {
            pdfIngestionUseCase.execute(file.getOriginalFilename(), inputStream);
        }

        return ResponseEntity.noContent().build();
    }
}
