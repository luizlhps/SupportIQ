package com.worklyze.supportiq.config.ai;

import com.worklyze.supportiq.shared.exceptions.BadRequestException;
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
            throw new BadRequestException(
                    AiExceptionCode.VECTOR_STORE_NOT_CONFIGURED.getMessage().formatted(provider),
                    AiExceptionCode.VECTOR_STORE_NOT_CONFIGURED.getCode()
            );
        }

        return store;
    }
}
