package org.ww2.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.ww2.dto.QuestionRequest;
import org.ww2.dto.QuestionResponse;
import org.ww2.entity.TemplateAnswer;
import org.ww2.repository.TemplateAnswerRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionProcessingService {
    
    private final TemplateAnswerRepository templateAnswerRepository;
    private final AiService aiService;
    
    /**
     * Обрабатывает вопрос и возвращает релевантный ответ
     */
    public QuestionResponse processQuestion(QuestionRequest request) {
        String category = request.getCategory();
        String question = request.getMessage();
        
        System.out.println("=== Question Processing Started ===");
        System.out.println("Question: " + question);
        System.out.println("Category: " + category);
        
        // Этап 2.1: Получаем все сущности с указанной категорией
        List<TemplateAnswer> categoryAnswers = templateAnswerRepository.findByCategory(category);
        System.out.println("Found " + categoryAnswers.size() + " answers for category: " + category);
        
        if (categoryAnswers.isEmpty()) {
            System.out.println("No answers found for category: " + category);
            return new QuestionResponse("No answers found for the specified category.");
        }
        
        // Этап 2.2: Составляем список уникальных подкатегорий
        List<String> subcategories = templateAnswerRepository.findDistinctSubcategoriesByCategory(category);
        System.out.println("Found subcategories: " + subcategories);
        
        if (subcategories.isEmpty()) {
            // Если подкатегорий нет, возвращаем первый доступный ответ
            System.out.println("No subcategories found, returning first answer");
            return new QuestionResponse(categoryAnswers.get(0).getMessage());
        }
        
        // Этап 2.3: Определяем наиболее подходящую подкатегорию с помощью ИИ
        System.out.println("Calling AI to find best subcategory...");
        String bestSubcategory = aiService.findBestSubcategory(question, subcategories);
        System.out.println("AI returned subcategory: '" + bestSubcategory + "'");
        
        if (bestSubcategory == null || bestSubcategory.isEmpty()) {
            System.out.println("AI returned empty subcategory");
            return new QuestionResponse("Could not determine the best subcategory.");
        }
        
        // Получаем все ответы с определенной категорией и подкатегорией
        List<TemplateAnswer> filteredAnswers = templateAnswerRepository
            .findByCategoryAndSubcategory(category, bestSubcategory);
        System.out.println("Found " + filteredAnswers.size() + " answers for category: " + category + ", subcategory: " + bestSubcategory);
        
        if (filteredAnswers.isEmpty()) {
            System.out.println("No answers found for determined subcategory: " + bestSubcategory);
            return new QuestionResponse("No answers found for the determined subcategory.");
        }
        
        // Этап 2.4: Извлекаем сообщения и находим наиболее релевантный ответ
        List<String> templateMessages = filteredAnswers.stream()
            .map(TemplateAnswer::getMessage)
            .collect(Collectors.toList());
        System.out.println("Template messages to send to AI: " + templateMessages);

        System.out.println("Calling AI to find best answer...");
        String bestAnswer = aiService.findBestAnswer(question, templateMessages);
        System.out.println("AI returned answer: '" + bestAnswer + "'");

        // Проверяем, что ответ не пустой
        if (bestAnswer == null || bestAnswer.trim().isEmpty() ||
            bestAnswer.equals("No suitable answer found.")) {
            System.out.println("AI returned empty or unsuitable answer");
            return new QuestionResponse("No suitable answer found for your question.");
        }

        System.out.println("=== Question Processing Completed Successfully ===");
        return new QuestionResponse(bestAnswer);
    }
}
