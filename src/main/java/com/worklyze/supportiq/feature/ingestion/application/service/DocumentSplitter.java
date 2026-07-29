package com.worklyze.supportiq.feature.ingestion.application.service;

import com.worklyze.supportiq.feature.ingestion.shared.KnowledgeChunk;
import com.worklyze.supportiq.feature.ingestion.shared.ParsedDocument;
import dev.langchain4j.data.segment.TextSegment;

import java.util.List;

public interface DocumentSplitter {
    List<TextSegment> split(ParsedDocument document);
}
