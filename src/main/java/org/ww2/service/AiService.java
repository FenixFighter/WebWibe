package org.ww2.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ww2.dto.AiResponseWithSuggestions;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiService {

    private final WebClient webClient;
    
    @Autowired
    private VectorSearchService vectorSearchService;

    public AiService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://45.145.191.148:4000/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer sk-DaEm7ghnNnWtYjrc3eiEug")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    public AiResponseWithSuggestions processQuestionWithDataSource(String question, String category) {
        System.out.println("=== AI Service: processQuestionWithDataSource (Few-Shot Learning) ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);

        // Генерируем few-shot промпт с примерами
        String fewShotPrompt = vectorSearchService.generateFewShotPrompt(question, category);
        System.out.println("Few-shot prompt: " + fewShotPrompt);

        String result = callAiApi(fewShotPrompt);
        System.out.println("AI response: " + result);

        // Генерируем предложения на основе найденных похожих вопросов
        List<String> suggestions = generateSuggestionsFromSimilarQuestions(question, category);
        
        // Получаем категорию и подкатегорию
        String[] categoryInfo = vectorSearchService.getCategoryAndSubcategory(question, category);
        String foundCategory = categoryInfo[0];
        String foundSubcategory = categoryInfo[1];
        
        return new AiResponseWithSuggestions(result, suggestions, foundCategory, foundSubcategory);
    }

    private String callAiApi(String prompt) {
        System.out.println("=== AI Service: callAiApi ===");
        System.out.println("Making request to SciBox API...");

        try {
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
                    "temperature": 0.3,
                    "max_tokens": 1000,
                    "top_p": 0.9,
                    "frequency_penalty": 0.1,
                    "presence_penalty": 0.1
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
            return getFallbackResponse(prompt);
        }
    }

    private String getFallbackResponse(String prompt) {
        System.out.println("Using fallback response for prompt: " + prompt);

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
            return "Credit Cards";
        }

        if (prompt.contains("template responses")) {
            String[] lines = prompt.split("\n");
            for (String line : lines) {
                if (line.trim().length() > 10 && !line.contains("---")) {
                    return line.trim();
                }
            }
        }

        return "Обратитесь в отделение банка ВТБ (Беларусь) для получения подробной информации по вашему вопросу.";
    }

    
    private List<String> generateSuggestionsFromSimilarQuestions(String question, String category) {
        System.out.println("=== Generating suggestions from similar questions ===");
        
        // Находим похожие вопросы из векторной базы знаний
        List<org.ww2.entity.KnowledgeVector> similarExamples = 
            vectorSearchService.findSimilarQuestions(question, category, 3);
        
        List<String> suggestions = new ArrayList<>();
        for (org.ww2.entity.KnowledgeVector example : similarExamples) {
            suggestions.add(example.getQuestion());
        }
        
        System.out.println("Generated suggestions: " + suggestions);
        return suggestions;
    }
}
