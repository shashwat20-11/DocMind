//package com.DocMind.config;
//
//import org.modelmapper.ModelMapper;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
//import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
//import org.springframework.ai.chat.memory.ChatMemory;
//import org.springframework.ai.chat.model.ChatModel;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import io.swagger.v3.oas.models.Components;
//import io.swagger.v3.oas.models.OpenAPI;
//import io.swagger.v3.oas.models.info.Contact;
//import io.swagger.v3.oas.models.info.Info;
//import io.swagger.v3.oas.models.security.SecurityRequirement;
//import io.swagger.v3.oas.models.security.SecurityScheme;
//
//@Configuration
//public class ProjectConfig {
//
//    @Bean
//    public ChatClient chatClient(ChatModel chatModel, ChatMemory memory) {
//        return ChatClient.builder(chatModel)
//                .defaultSystem("""
//                        You are DocMind, an intelligent, versatile, and friendly AI document intelligence assistant.
//
//                        Your Capabilities:
//                        1. Document-Grounded Q&A: When context from the user's uploaded documents is provided, prioritize and base your answer directly on that context, citing document names and page numbers when available.
//                        2. General Knowledge & Conversation: If the user engages in general conversation (greetings, chit-chat, programming questions, math, explanations, summaries, or general knowledge) that may not be present in the uploaded documents, answer helpfully, accurately, and naturally.
//                        3. Hybrid Synthesis: If the document context partially covers a topic, synthesize the document facts with your broader knowledge to give a complete, high-quality answer.
//                        4. Tone & Format: Always be warm, professional, clear, and structured. Use Markdown (headings, bullet points, bold text, code blocks) to make responses easy to read.
//
//                        Document Context is :
//                        {doc_context}
//
//                        Produce markdown output.
//                        """)
//                .defaultAdvisors(
//                        new SimpleLoggerAdvisor(),
//                        MessageChatMemoryAdvisor.builder(memory).build()
//                )
//                .build();
//    }
//
//    @Bean
//    public OpenAPI openAPI() {
//        String securitySchemeName = "bearerAuth";
//        return new OpenAPI()
//                .info(new Info()
//                        .title("DocMind - AI Intelligence & RAG Backend")
//                        .description("REST API for DocMind: Multi-format document ingestion, semantic search via vector embeddings with MySQL, and hybrid conversational Q&A powered by Spring AI and OpenAI.")
//                        .version("1.0.0")
//                        .contact(new Contact()
//                                .name("Shashwat")
//                                .email("abc@gmail.com")
//                                .url("http://localhost:3888/swagger-ui/index.html")))
//                .components(new Components()
//                        .addSecuritySchemes(securitySchemeName,
//                                new SecurityScheme()
//                                        .name(securitySchemeName)
//                                        .type(SecurityScheme.Type.HTTP)
//                                        .scheme("bearer")
//                                        .bearerFormat("JWT")))
//                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
//    }
//
//    @Bean
//    public ModelMapper modelMapper() {
//        return new ModelMapper();
//    }
//}



package com.DocMind.config;

import org.modelmapper.ModelMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class ProjectConfig {

	@Bean
    @Primary
    public ChatModel chatModel(
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model:gemini-flash-lite-latest}") String model,
            @Value("${spring.ai.openai.chat.options.temperature:0.2}") Double temp) {
        return new GeminiChatModel(apiKey, model, temp);
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory memory) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are DocMind, an intelligent, versatile, and friendly AI document intelligence assistant.

                        Your Capabilities:
                        1. Document-Grounded Q&A: When context from the user's uploaded documents is provided, prioritize and base your answer directly on that context, citing document names and page numbers when available.
                        2. General Knowledge & Conversation: If the user engages in general conversation (greetings, chit-chat, programming questions, math, explanations, summaries, or general knowledge) that may not be present in the uploaded documents, answer helpfully, accurately, and naturally.
                        3. Hybrid Synthesis: If the document context partially covers a topic, synthesize the document facts with your broader knowledge to give a complete, high-quality answer.
                        4. Tone & Format: Always be warm, professional, clear, and structured. Use Markdown (headings, bullet points, bold text, code blocks) to make responses easy to read.

                        Document Context is :
                        {doc_context}

                        Produce markdown output.
                        """)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),
                        MessageChatMemoryAdvisor.builder(memory).build()
                )
                .build();
    }

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("DocMind - AI Intelligence & RAG Backend")
                        .description("REST API for DocMind: Multi-format document ingestion, semantic search via vector embeddings with MySQL, and hybrid conversational Q&A powered by Spring AI and OpenAI.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Shashwat")
                                .email("abc@gmail.com")
                                .url("http://localhost:3888/swagger-ui/index.html")))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }

    @Bean
    public ModelMapper modelMapper() {
        return new ModelMapper();
    }
}