package com.worklyze.supportiq.config.ai;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Map;

public class VectorStoreRegistry {

    private final Map<AiProvider, EmbeddingStore<TextSegment>> stores;

    public VectorStoreRegistry(Map<AiProvider, EmbeddingStore<TextSegment>> stores) {
        this.stores = stores;
    }

    public EmbeddingStore<TextSegment> store(AiProvider provider) {
        EmbeddingStore<TextSegment> store = stores.get(provider);

        if (store == null) {
            throw new IllegalStateException(
                    "Vector store nao configurado para o provider: " + provider
            );
        }

        return store;
    }
}
