package com.worklyze.supportiq.feature.ingestion.shared;

import java.util.List;

public record ParsedDocument(
        String fileName,
        List<ParsedPage> pages
) {
}