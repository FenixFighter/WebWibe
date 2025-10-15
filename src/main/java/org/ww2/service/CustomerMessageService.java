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
    private final MessageProcessingService messageProcessingService;
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
        
        // Process message and get AI response
        String aiResponse = messageProcessingService.processCustomerMessage(chatId, message, request);
        
        if (aiResponse == null) {
            return; // No response needed
        }
        
        // AI ALWAYS responds - no escalation, no exceptions
        // Evaluate AI response and save with rating
        AiRating rating = aiRatingService.evaluateAiResponse(message, aiResponse);
        saveAndSendAiResponseWithRating(chatId, aiResponse, rating);
        
        // Low ratings are only used for visual indicators in support dashboard
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
