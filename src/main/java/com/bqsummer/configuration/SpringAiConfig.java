package com.bqsummer.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.model.SimpleApiKey;

/**
 * Spring AI 配置类
 * 
 * 配置统一的 AI 模型调用基础设施，包括：
 * 1. ChatClient 全局配置
 * 2. 超时和重试策略
 * 3. 默认模型参数
 * 
 * @author Boboa Boot Team
 * @date 2025-10-21
 */
@Configuration
public class SpringAiConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String defaultApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String defaultBaseUrl;

    @Value("${spring.ai.chat.timeout:30}")
    private int chatTimeoutSeconds;

    @Value("${spring.ai.chat.max-retries:1}")
    private int maxRetries;

    /**
     * 配置默认的 ChatClient Builder
     * 用于创建动态的 ChatClient 实例
     * 
     * @return ChatClient.Builder
     */
    @Bean
    public ChatClient.Builder chatClientBuilder() {
        // 创建默认的 OpenAI ChatModel
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(defaultBaseUrl)
                .apiKey(new SimpleApiKey(defaultApiKey))
                .build();
        
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model("gpt-3.5-turbo")
                .temperature(0.7)
                .maxTokens(2000)
                .build();
        
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
        
        return ChatClient.builder(chatModel);
    }

    /**
     * 获取配置的超时时间
     * 
     * @return 超时时间（毫秒）
     */
    public int getChatTimeoutMillis() {
        return chatTimeoutSeconds * 1000;
    }

    /**
     * 获取配置的最大重试次数
     * 
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }
}
