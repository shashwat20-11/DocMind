package com.DocMind.config;

import java.io.File;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class VectorStoreConfig {

    private final AppProperties appProperties;

    

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // Built-in Spring AI class:
        SimpleVectorStore simpleVectorStore = new SimpleVectorStore(embeddingModel);

        // Load existing vectors from disk if the JSON file already exists
        File file = new File(appProperties.getRag().getVectorStorePath());
        if (file.exists()) {
            simpleVectorStore.load(file);
        }

        return simpleVectorStore;
    }
}