package com.worklyze.supportiq.feature.ingestion.shared;

import org.apache.tika.exception.TikaException;

import java.io.InputStream;

public interface DocumentParser {

    ParsedDocument parse(
            String fileName,
            InputStream inputStream) throws TikaException;
}