package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.AiResponseWithSuggestions;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingService {

    private final AiService aiService;
    private final ChatAssignmentService chatAssignmentService;

    /**
     * Processes customer message and returns AI response with suggestions
     */
    public AiResponseWithSuggestions processCustomerMessage(String chatId, String message, ChatRequest request) {
        // Check if chat is assigned to support - if yes, don't use AI
        var assignment = chatAssignmentService.getChatAssignment(chatId);
        if (assignment.isPresent()) {
            log.info("Chat {} is assigned to support, skipping AI response", chatId);
            return null; // Don't use AI when support is active
        }

        // NEW IMPLEMENTATION: Use data source search with suggestions for all questions
        String category = request.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = null; // Will be passed as null to AI for general search
        }
        
        log.info("Processing question with data source search and suggestions, category: {}", category);
        return processQuestionWithDataSource(message, category);
    }

    /**
     * Processes question with data source search and suggestions (main implementation)
     */
    private AiResponseWithSuggestions processQuestionWithDataSource(String message, String category) {
        return aiService.processQuestionWithDataSource(message, category);
    }

    /**
     * Checks if AI response indicates need for human support
     */
    public boolean shouldEscalateToSupport(String aiResponse) {
        if (aiResponse == null) {
            return false;
        }
        
        String lowerResponse = aiResponse.toLowerCase();
        return lowerResponse.contains("no answers found") ||
               lowerResponse.contains("cannot help") ||
               lowerResponse.contains("don't understand") ||
               lowerResponse.contains("contact support") ||
               lowerResponse.contains("human assistance") ||
               lowerResponse.contains("escalate");
    }
}
