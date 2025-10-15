package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.WebSocketMessage;
import org.ww2.dto.AiRating;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerMessageService {

    private final ChatService chatService;
    private final ChatAssignmentService chatAssignmentService;
    private final MessageProcessingService messageProcessingService;
    private final ChatManagementService chatManagementService;
    private final AiRatingService aiRatingService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles customer message processing
     */
    public void handleCustomerMessage(String chatId, String message, ChatRequest request) {
        // Save user message
        chatService.saveUserMessage(chatId, message);
        
        // Send user message back to client
        WebSocketMessage userMessage = WebSocketMessage.createUserMessage(chatId, message);
        messagingTemplate.convertAndSend("/topic/chat", userMessage);
        
        // Check if chat is assigned to support - if yes, don't use AI
        if (chatManagementService.isChatAssignedToSupport(chatId)) {
            log.info("Chat {} is assigned to support, skipping AI response", chatId);
            return; // Don't use AI when support is active
        }
        
        // Process message and get AI response
        String aiResponse = messageProcessingService.processCustomerMessage(chatId, message, request);
        
        if (aiResponse == null) {
            return; // No response needed
        }
        
        // Check if AI response indicates need for human support
        if (messageProcessingService.shouldEscalateToSupport(aiResponse)) {
            handleEscalation(chatId);
            return;
        }
        
        // Evaluate AI response and save with rating
        AiRating rating = aiRatingService.evaluateAiResponse(message, aiResponse);
        saveAndSendAiResponseWithRating(chatId, aiResponse, rating);
        
        // Note: Low ratings are now only used for visual indicators in support dashboard
        // No automatic escalation - AI continues to work normally
    }

    /**
     * Handles chat escalation
     */
    private void handleEscalation(String chatId) {
        var newAssignment = chatAssignmentService.assignChatToSupport(chatId);
        if (newAssignment != null) {
            WebSocketMessage escalationMessage = WebSocketMessage.createEscalationMessage(chatId,
                "Your request has been escalated to our support team. A support agent will join shortly.");
            messagingTemplate.convertAndSend("/topic/chat", escalationMessage);
            log.info("Chat {} escalated to support", chatId);
        } else {
            log.warn("Failed to escalate chat {} - no support agents available", chatId);
        }
    }

    /**
     * Saves AI message and sends it to client with rating
     */
    private void saveAndSendAiResponseWithRating(String chatId, String aiResponse, AiRating rating) {
        // Save AI message
        chatService.saveAiMessage(chatId, aiResponse);
        
        // Send AI response to client with rating
        WebSocketMessage aiMessage = aiRatingService.createAiMessageWithRating(chatId, aiResponse, rating);
        messagingTemplate.convertAndSend("/topic/chat", aiMessage);
        
        log.info("AI response sent for chat {} with rating {}/100", chatId, rating.getScore());
    }
}
