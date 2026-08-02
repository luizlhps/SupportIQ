package com.worklyze.supportiq.config;

import com.worklyze.supportiq.config.ai.AiModelRegistry;
import com.worklyze.supportiq.config.ai.AiProvider;
import com.worklyze.supportiq.config.ai.VectorStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.EnumMap;
import java.util.Map;

@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    /**
     * Uma tabela pgvector por provider: cada provider tem uma dimensao de
     * embedding diferente e nao faz sentido misturar vetores de modelos
     * distintos no mesmo indice.
     * <p>
     * Falhas ao inicializar um provider especifico (ex: modelo indisponivel na
     * API remota) sao logadas e o provider e pulado - a aplicacao sobe com os
     * providers que funcionam.
     */
    @Bean
    public VectorStoreRegistry vectorStoreRegistry(
            DataSource dataSource,
            AiModelRegistry aiModelRegistry
    ) {
        Map<AiProvider, EmbeddingStore<TextSegment>> stores = new EnumMap<>(AiProvider.class);

        for (AiProvider provider : AiProvider.values()) {

            if (!aiModelRegistry.isAvailable(provider)) {
                continue;
            }

            try {
                EmbeddingModel model = aiModelRegistry.embedding(provider);

                EmbeddingStore<TextSegment> store = PgVectorEmbeddingStore.datasourceBuilder()
                        .datasource(dataSource)
                        .table("knowledge_embedding_" + provider.name().toLowerCase())
                        .dimension(model.dimension())
                        .createTable(true)
                        .build();

                stores.put(provider, store);
                log.info("Vector store inicializado para provider {}", provider);

            } catch (Exception ex) {
                log.warn("Vector store desabilitado para provider {}: {}", provider, ex.getMessage());
            }
        }

        return new VectorStoreRegistry(stores);
    }
}