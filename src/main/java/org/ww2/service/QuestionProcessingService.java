package org.ww2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ww2.dto.QuestionRequest;
import org.ww2.dto.QuestionResponse;
import org.ww2.dto.AiResponseWithSuggestions;

@Service
@RequiredArgsConstructor
public class QuestionProcessingService {
    
    private final AiService aiService;
    
    /**
     * Обрабатывает вопрос и возвращает релевантный ответ (новая реализация)
     */
    public QuestionResponse processQuestion(QuestionRequest request) {
        String category = request.getCategory();
        String question = request.getMessage();
        
        System.out.println("=== Question Processing Started (NEW IMPLEMENTATION) ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);
        
        // Используем новую реализацию с поиском по [DATA_SOURCE]
        AiResponseWithSuggestions aiResponse = aiService.processQuestionWithDataSource(question, category);
        
        System.out.println("AI returned answer: '" + aiResponse.getAnswer() + "'");
        System.out.println("AI returned suggestions: " + aiResponse.getSuggestions());
        
        // Проверяем, что ответ не пустой
        if (aiResponse.getAnswer() == null || aiResponse.getAnswer().trim().isEmpty()) {
            System.out.println("AI returned empty answer");
            return new QuestionResponse("No suitable answer found for your question.");
        }

        System.out.println("=== Question Processing Completed Successfully ===");
        return new QuestionResponse(aiResponse.getAnswer());
    }
}
