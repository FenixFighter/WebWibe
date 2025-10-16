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

    public AiResponseWithSuggestions processCustomerMessage(String chatId, String message, ChatRequest request) {

        var assignment = chatAssignmentService.getChatAssignment(chatId);
        if (assignment.isPresent()) {
            log.info("Chat {} is assigned to support, skipping AI response", chatId);
            return null; 
        }

        String category = request.getCategory();
        if (category == null || category.trim().isEmpty()) {
            category = null; 
        }

        log.info("Processing question with data source search and suggestions, category: {}", category);
        return processQuestionWithDataSource(message, category);
    }

    private AiResponseWithSuggestions processQuestionWithDataSource(String message, String category) {
        return aiService.processQuestionWithDataSource(message, category);
    }

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
