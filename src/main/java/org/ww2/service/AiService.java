package org.ww2.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.ww2.dto.AiResponseWithSuggestions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {
    
    private final WebClient webClient;
    
    public AiService() {
        this.webClient = WebClient.builder()
            .baseUrl("http://45.145.191.148:4000/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer sk-DaEm7ghnNnWtYjrc3eiEug")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
    
    
    
    /**
     * Новая реализация: обрабатывает вопрос с поиском по системным меткам [DATA_SOURCE]
     * @param question - вопрос пользователя
     * @param category - категория (может быть null, используется как направление для AI)
     * @return структурированный ответ AI с подсказками
     */
    public AiResponseWithSuggestions processQuestionWithDataSource(String question, String category) {
        System.out.println("=== AI Service: processQuestionWithDataSource ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);
        
        // Строим промпт для поиска ответа и подсказок по системным меткам
        String searchPrompt = buildDataSourceSearchWithSuggestionsPrompt(question, category);
        System.out.println("Data source search with suggestions prompt: " + searchPrompt);
        
        String result = callAiApi(searchPrompt);
        System.out.println("AI response: " + result);
        
        // Парсим ответ для извлечения основного ответа и подсказок
        return parseAiResponseWithSuggestions(result);
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
    
    
    /**
     * Строит промпт для поиска по системным меткам [DATA_SOURCE]
     */
    private String buildDataSourceSearchPrompt(String question, String category) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI assistant with access to a knowledge base containing banking information.\n");
        prompt.append("Your task is to search through your internal knowledge base for the most relevant answer.\n\n");
        
        prompt.append("IMPORTANT: Look for responses that contain the special marker [DATA_SOURCE] in your knowledge base.\n");
        prompt.append("These are verified, high-quality responses that should be prioritized.\n\n");
        
        if (category != null && !category.trim().isEmpty()) {
            prompt.append("Question category/direction: ").append(category).append("\n");
            prompt.append("Use this as a hint for the type of banking service the user is asking about.\n\n");
        }
        
        prompt.append("User question: ").append(question).append("\n\n");
        
        prompt.append("Instructions:\n");
        prompt.append("1. Search your knowledge base for responses marked with [DATA_SOURCE]\n");
        prompt.append("2. Find the most relevant answer based on the user's question\n");
        prompt.append("3. If you find a relevant [DATA_SOURCE] response, use it as your answer\n");
        prompt.append("4. If no [DATA_SOURCE] response is relevant, provide a general helpful response\n");
        prompt.append("5. Always be professional and accurate in your banking advice\n\n");
        
        prompt.append("Please provide the most appropriate response from your knowledge base.");
        
        return prompt.toString();
    }
    
    /**
     * Строит промпт для поиска ответа и подсказок по системным меткам [DATA_SOURCE]
     */
    private String buildDataSourceSearchWithSuggestionsPrompt(String question, String category) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an AI assistant with access to a knowledge base containing banking information.\n");
        prompt.append("Your task is to search through your internal knowledge base for the most relevant answer AND related suggestions.\n\n");
        
        prompt.append("IMPORTANT: Look for responses that contain the special marker [DATA_SOURCE] in your knowledge base.\n");
        prompt.append("These are verified, high-quality responses that should be prioritized.\n\n");
        
        if (category != null && !category.trim().isEmpty()) {
            prompt.append("Question category/direction: ").append(category).append("\n");
            prompt.append("Use this as a hint for the type of banking service the user is asking about.\n\n");
        }
        
        prompt.append("User question: ").append(question).append("\n\n");
        
        prompt.append("Instructions:\n");
        prompt.append("1. Search your knowledge base for responses marked with [DATA_SOURCE]\n");
        prompt.append("2. Find the most relevant answer based on the user's question\n");
        prompt.append("3. Also find related questions/suggestions marked with [DATA_SOURCE]\n");
        prompt.append("4. If you find relevant [DATA_SOURCE] responses, use them as your answer and suggestions\n");
        prompt.append("5. If no [DATA_SOURCE] responses are relevant, provide general helpful response and suggestions\n");
        prompt.append("6. Always be professional and accurate in your banking advice\n\n");
        
        prompt.append("Please provide your response in the following format:\n");
        prompt.append("ANSWER: [Your main answer here]\n");
        prompt.append("SUGGESTIONS: [Related question 1] | [Related question 2] | [Related question 3]\n\n");
        prompt.append("Note: Only include suggestions that are marked with [DATA_SOURCE] in your knowledge base.");
        
        return prompt.toString();
    }
    
    /**
     * Парсит ответ AI для извлечения основного ответа и подсказок
     */
    private AiResponseWithSuggestions parseAiResponseWithSuggestions(String aiResponse) {
        System.out.println("=== Parsing AI Response with Suggestions ===");
        System.out.println("Raw AI response: " + aiResponse);
        
        String answer = "";
        List<String> suggestions = new ArrayList<>();
        
        try {
            // Ищем основной ответ
            Pattern answerPattern = Pattern.compile("ANSWER:\\s*(.+?)(?=SUGGESTIONS:|$)", Pattern.DOTALL);
            Matcher answerMatcher = answerPattern.matcher(aiResponse);
            if (answerMatcher.find()) {
                answer = answerMatcher.group(1).trim();
            } else {
                // Если формат не найден, используем весь ответ как основной ответ
                answer = aiResponse.trim();
            }
            
            // Ищем подсказки
            Pattern suggestionsPattern = Pattern.compile("SUGGESTIONS:\\s*(.+?)$", Pattern.DOTALL);
            Matcher suggestionsMatcher = suggestionsPattern.matcher(aiResponse);
            if (suggestionsMatcher.find()) {
                String suggestionsText = suggestionsMatcher.group(1).trim();
                // Разделяем подсказки по символу |
                String[] suggestionArray = suggestionsText.split("\\|");
                for (String suggestion : suggestionArray) {
                    String trimmedSuggestion = suggestion.trim();
                    if (!trimmedSuggestion.isEmpty()) {
                        suggestions.add(trimmedSuggestion);
                    }
                }
            }
            
            System.out.println("Parsed answer: " + answer);
            System.out.println("Parsed suggestions: " + suggestions);
            
        } catch (Exception e) {
            System.out.println("Error parsing AI response: " + e.getMessage());
            // Fallback: используем весь ответ как основной ответ
            answer = aiResponse.trim();
        }
        
        return new AiResponseWithSuggestions(answer, suggestions);
    }
}
