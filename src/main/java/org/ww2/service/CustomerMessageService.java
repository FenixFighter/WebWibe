package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.WebSocketMessage;
import org.ww2.dto.AiRating;
import org.ww2.dto.AiResponseWithSuggestions;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerMessageService {

    private final ChatService chatService;
    private final MessageProcessingService messageProcessingService;
    private final AiRatingService aiRatingService;
    private final SimpMessagingTemplate messagingTemplate;

    public void handleCustomerMessage(String chatId, String message, ChatRequest request) {
        chatService.saveUserMessage(chatId, message);

        WebSocketMessage userMessage = WebSocketMessage.createUserMessage(chatId, message);
        messagingTemplate.convertAndSend("/topic/chat", userMessage);

        AiResponseWithSuggestions aiResponse = messageProcessingService.processCustomerMessage(chatId, message, request);

        if (aiResponse == null) {
            return;
        }

        AiRating rating = aiRatingService.evaluateAiResponse(message, aiResponse.getAnswer());
        saveAndSendAiResponseWithRatingAndSuggestions(chatId, aiResponse, rating);

    }

    private void saveAndSendAiResponseWithRatingAndSuggestions(String chatId, AiResponseWithSuggestions aiResponse, AiRating rating) {
        chatService.saveAiMessage(chatId, aiResponse.getAnswer());

        WebSocketMessage aiMessage = new WebSocketMessage();
        aiMessage.setType("MESSAGE");
        aiMessage.setChatId(chatId);
        aiMessage.setContent(aiResponse.getAnswer());
        aiMessage.setSender("AI");
        aiMessage.setTimestamp(System.currentTimeMillis());
        aiMessage.setRating(rating);
        aiMessage.setSuggestions(aiResponse.getSuggestions());

        messagingTemplate.convertAndSend("/topic/chat", aiMessage);

        log.info("AI response with suggestions sent for chat {} with rating {}/100", chatId, rating.getScore());
    }
}
