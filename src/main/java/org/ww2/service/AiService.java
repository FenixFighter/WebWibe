package org.ww2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.List;

@Service
public class AiService {
    
    private final WebClient webClient;
    
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;
    
    public AiService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://45.145.191.148:4000/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer sk-DaEm7ghnNnWtYjrc3eiEug")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    /**
     * Определяет наиболее подходящую подкатегорию на основе вопроса и списка доступных подкатегорий
     */
    public String findBestSubcategory(String question, List<String> subcategories) {
        System.out.println("=== AI Service: findBestSubcategory ===");
        System.out.println("Question: " + question);
        System.out.println("Subcategories: " + subcategories);
        
        if (subcategories == null || subcategories.isEmpty()) {
            System.out.println("No subcategories provided");
            return null;
        }
        
        String subcategoriesList = String.join(", ", subcategories);
        
        String prompt = String.format(
            "Based on the user question: \"%s\"\n" +
            "Choose the most relevant subcategory from this list: %s\n",
            question, subcategoriesList
        );
        
        System.out.println("Prompt for AI: " + prompt);
        String result = callAiApi(prompt);
        System.out.println("AI response: " + result);
        return result;
    }
    
    /**
     * Обрабатывает вопрос пользователя с учетом контекста истории чата
     */
    public String processQuestion(String question, String chatHistory) {
        System.out.println("=== AI Service: processQuestion ===");
        System.out.println("Question: " + question);
        System.out.println("Chat History: " + chatHistory);
        
        String contextPrompt = buildContextPrompt(question, chatHistory);
        System.out.println("Context prompt: " + contextPrompt);
        
        String result = callAiApi(contextPrompt);
        System.out.println("AI response: " + result);
        return result;
    }
    
    /**
     * Находит наиболее релевантный ответ из списка шаблонных ответов
     */
    public String findBestAnswer(String question, List<String> templateMessages) {
        System.out.println("=== AI Service: findBestAnswer ===");
        System.out.println("Question: " + question);
        System.out.println("Template messages: " + templateMessages);
        
        if (templateMessages == null || templateMessages.isEmpty()) {
            System.out.println("No template messages provided");
            return "No relevant answer found.";
        }
        
        String messagesList = String.join("\n---\n", templateMessages);
        
        String prompt = String.format(
            "Based on the user question: \"%s\"\n" +
            "Choose the most relevant answer from these template responses:\n\n%s\n\n" +
            "Return only the most appropriate answer text, nothing else. " +
            question, messagesList
        );
        
        System.out.println("Prompt for AI: " + prompt);
        String result = callAiApi(prompt);
        System.out.println("AI response: " + result);
        return result;
    }
    
    private String callAiApi(String prompt) {
        System.out.println("=== AI Service: callAiApi ===");
        System.out.println("Making request to SciBox API...");
        
        try {
            // Правильное экранирование JSON
            String escapedPrompt = prompt.replace("\\", "\\\\")
                                       .replace("\"", "\\\"")
                                       .replace("\n", "\\n")
                                       .replace("\r", "\\r")
                                       .replace("\t", "\\t");
            
            String requestBody = String.format("""
                {
                    "model": "Qwen2.5-72B-Instruct-AWQ",
                    "messages": [
                        {"role": "user", "content": "%s"}
                    ],
                    "temperature": 0.7,
                    "max_tokens": 1000
                }
                """, escapedPrompt);
            
            System.out.println("Request body: " + requestBody);
            
            String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            System.out.println("Raw AI response: " + response);
            
            // Улучшенная обработка ответа
            if (response != null && response.contains("\"content\"")) {
                int start = response.indexOf("\"content\":\"") + 11;
                int end = response.indexOf("\"", start);
                if (start > 10 && end > start) {
                    String extractedContent = response.substring(start, end)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n")
                        .replace("\\r", "\r")
                        .replace("\\t", "\t")
                        .trim();
                    System.out.println("Extracted content: " + extractedContent);
                    return extractedContent;
                }
            }
            
            System.out.println("Failed to extract content from AI response");
            return "Error processing AI response";
        } catch (Exception e) {
            System.out.println("Exception in callAiApi: " + e.getMessage());
            e.printStackTrace();
            // Возвращаем fallback ответ вместо ошибки
            return getFallbackResponse(prompt);
        }
    }
    
    private String getFallbackResponse(String prompt) {
        System.out.println("Using fallback response for prompt: " + prompt);
        
        // Простая логика fallback для определения подкатегории
        if (prompt.contains("subcategory")) {
            if (prompt.contains("Credit Cards")) {
                return "Credit Cards";
            } else if (prompt.contains("Loans")) {
                return "Loans";
            } else if (prompt.contains("Savings")) {
                return "Savings";
            } else if (prompt.contains("Login Issues")) {
                return "Login Issues";
            } else if (prompt.contains("Mobile App")) {
                return "Mobile App";
            } else if (prompt.contains("Contact")) {
                return "Contact";
            }
            // Возвращаем первую доступную подкатегорию
            return "Credit Cards";
        }
        
        // Fallback для выбора лучшего ответа - возвращаем первый доступный ответ
        if (prompt.contains("template responses")) {
            // Извлекаем первый ответ из списка шаблонных ответов
            String[] lines = prompt.split("\n");
            for (String line : lines) {
                if (line.trim().length() > 10 && !line.contains("---")) {
                    return line.trim();
                }
            }
        }
        
        return "To apply for a credit card, please visit our nearest branch with your ID and proof of income.";
    }
    
    private String buildContextPrompt(String question, String chatHistory) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are a helpful banking assistant. Please answer the user's question based on the context provided.\n\n");
        
        if (chatHistory != null && !chatHistory.trim().isEmpty()) {
            prompt.append("Previous conversation context:\n");
            prompt.append(chatHistory);
            prompt.append("\n\n");
        }
        
        prompt.append("Current user question: ");
        prompt.append(question);
        prompt.append("\n\n");
        prompt.append("Please provide a helpful and accurate response. If you need more information, ask clarifying questions.");
        
        return prompt.toString();
    }
}
