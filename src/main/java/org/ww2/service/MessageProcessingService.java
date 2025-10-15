package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.QuestionRequest;
import org.ww2.dto.QuestionResponse;
import org.ww2.entity.ChatMessage;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageProcessingService {

    private final ChatService chatService;
    private final AiService aiService;
    private final QuestionProcessingService questionProcessingService;
    private final ChatAssignmentService chatAssignmentService;

    /**
     * Processes customer message and returns AI response
     */
    public String processCustomerMessage(String chatId, String message, ChatRequest request) {
        // Check if chat is assigned to support - if yes, don't use AI
        var assignment = chatAssignmentService.getChatAssignment(chatId);
        if (assignment.isPresent()) {
            log.info("Chat {} is assigned to support, skipping AI response", chatId);
            return null; // Don't use AI when support is active
        }

        // Get AI response using QuestionProcessingService if category is provided
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            return processStructuredQuestion(message, request.getCategory());
        } else {
            return processGeneralQuestion(chatId, message);
        }
    }

    /**
     * Processes structured question with category
     */
    private String processStructuredQuestion(String message, String category) {
        QuestionRequest questionRequest = new QuestionRequest();
        questionRequest.setCategory(category);
        questionRequest.setMessage(message);
        
        QuestionResponse questionResponse = questionProcessingService.processQuestion(questionRequest);
        return questionResponse.getAnswer();
    }

    /**
     * Processes general question with chat history
     */
    private String processGeneralQuestion(String chatId, String message) {
        List<ChatMessage> chatHistory = chatService.getChatHistory(chatId);
        String historyContext = buildHistoryContext(chatHistory);
        return aiService.processQuestion(message, historyContext);
    }

    /**
     * Builds context from chat history
     */
    private String buildHistoryContext(List<ChatMessage> chatHistory) {
        if (chatHistory.isEmpty()) {
            return "";
        }
        
        return chatHistory.stream()
                .map(msg -> msg.getSenderType() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
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
