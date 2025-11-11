package com.therighthandapp.autobads.core;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Core AI Configuration - Sets up Spring AI ChatModel and ChatClient
 */
@Configuration
public class AiConfiguration {

    @Value("${spring.ai.openai.api-key:dummy-key}")
    private String apiKey;

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ChatModel chatModel() {
        OpenAiApi openAiApi = new OpenAiApi(apiKey);
        return new OpenAiChatModel(openAiApi);
    }

    @Bean
    @Primary
    @ConditionalOnMissingBean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

