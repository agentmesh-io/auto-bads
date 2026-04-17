package com.therighthandapp.autobads.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Core AI Configuration - Sets up Spring AI ChatModel and ChatClient with Ollama
 */
@Configuration
public class AiConfiguration {

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    @Value("${spring.ai.ollama.chat.model:phi3:mini}")
    private String model;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ChatModel chatModel() {
        OllamaApi ollamaApi = new OllamaApi(baseUrl);
        OllamaOptions options = OllamaOptions.create()
            .withModel(model)
            .withTemperature(0.7);
        return new OllamaChatModel(ollamaApi, options);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

