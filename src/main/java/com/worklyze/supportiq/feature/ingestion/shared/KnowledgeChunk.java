package com.worklyze.supportiq.feature.ingestion.shared;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record KnowledgeChunk(

        UUID id,

        String title,

        String content,

        Integer page,

        List<KnowledgeImage> images,

        Map<String, Object> metadata

) {}