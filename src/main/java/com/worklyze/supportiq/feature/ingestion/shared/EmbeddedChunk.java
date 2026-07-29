package com.worklyze.supportiq.feature.ingestion.shared;

public record EmbeddedChunk(

        KnowledgeChunk chunk,

        float[] embedding

) {}