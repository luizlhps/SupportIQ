package com.worklyze.supportiq.feature.ingestion.domain.usecases;

import org.apache.tika.exception.TikaException;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface PdfIngestionUseCase {
    void execute(String fileName, InputStream inputStream) ;
}
