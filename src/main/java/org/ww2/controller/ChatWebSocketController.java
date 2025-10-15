package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.WebSocketMessage;
import org.ww2.service.ChatManagementService;
import org.ww2.service.CustomerMessageService;
import org.ww2.service.SupportMessageService;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatManagementService chatManagementService;
    private final CustomerMessageService customerMessageService;
    private final SupportMessageService supportMessageService;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatRequest request) {
        try {
            log.info("Received message: {}", request);
            
            // Create or update chat
            String chatId = chatManagementService.createOrUpdateChat(request);
            
            // Determine message routing
            boolean isSupportUser = chatManagementService.isSupportUser(request.getToken());
            
            log.info("Chat routing - isSupportUser: {}", isSupportUser);
            
            if (isSupportUser) {
                log.info("Routing to support handler - Support user message");
                handleSupportMessage(chatId, request);
            } else {
                log.info("Routing to customer handler - Customer message");
                handleCustomerMessage(chatId, request);
            }
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            sendErrorMessage("Error processing message: " + e.getMessage());
        }
    }
    
    private void handleCustomerMessage(String chatId, ChatRequest request) {
        customerMessageService.handleCustomerMessage(chatId, request.getMessage(), request);
        
        // Notify support about new chat activity
        chatManagementService.notifySupportAboutChat(chatId, request.getMessage());
    }
    
    private void handleSupportMessage(String chatId, ChatRequest request) {
        var assignment = chatManagementService.getChatAssignment(chatId);
        supportMessageService.handleSupportMessage(chatId, request.getMessage(), request.getToken(), assignment.orElse(null));
    }
    
    
    
    @MessageMapping("/chat.join")
    public void joinChat(@Payload ChatRequest request) {
        try {
            String chatId = chatManagementService.createOrUpdateChat(request);
            
            // Send chat history if exists
            sendChatHistory(chatId);
            
        } catch (Exception e) {
            log.error("Error joining chat", e);
            sendErrorMessage("Error joining chat: " + e.getMessage());
        }
    }
    
    @MessageMapping("/chat.escalate")
    public void escalateChat(@Payload ChatRequest request) {
        try {
            String chatId = request.getChatId();
            if (chatId == null || chatId.trim().isEmpty()) {
                sendErrorMessage("Chat ID is required for escalation.");
                return;
            }
            
            supportMessageService.handleChatEscalation(chatId);
            chatManagementService.notifySupportAboutChat(chatId, "Customer requested escalation.");
            
        } catch (Exception e) {
            log.error("Error escalating chat", e);
            sendErrorMessage("Error escalating chat: " + e.getMessage());
        }
    }
    
    @MessageMapping("/chat.release")
    public void releaseChat(@Payload ChatRequest request) {
        try {
            String chatId = request.getChatId();
            if (chatId == null || chatId.trim().isEmpty()) {
                sendErrorMessage("Chat ID is required for release.");
                return;
            }
            
            supportMessageService.handleChatRelease(chatId, request.getToken());
            
        } catch (Exception e) {
            log.error("Error releasing chat", e);
            sendErrorMessage("Error releasing chat: " + e.getMessage());
        }
    }
    
    /**
     * Sends chat history to client
     */
    private void sendChatHistory(String chatId) {
        // This method can be implemented if needed for specific chat history loading
        // For now, it's handled by the general message flow
    }
    
    /**
     * Sends error message to client
     */
    private void sendErrorMessage(String errorMessage) {
        WebSocketMessage error = WebSocketMessage.createError(errorMessage);
        messagingTemplate.convertAndSend("/topic/chat/error", error);
    }
}
