package org.ww2.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.ww2.dto.ChatRequest;
import org.ww2.dto.QuestionRequest;
import org.ww2.dto.QuestionResponse;
import org.ww2.dto.WebSocketMessage;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.ChatMessage;
import org.ww2.entity.User;
import org.ww2.service.AiService;
import org.ww2.service.AuthService;
import org.ww2.service.ChatAssignmentService;
import org.ww2.service.ChatService;
import org.ww2.service.QuestionProcessingService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final AiService aiService;
    private final QuestionProcessingService questionProcessingService;
    private final AuthService authService;
    private final ChatAssignmentService chatAssignmentService;
    private final ObjectMapper objectMapper;
    
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatRequest request) {
        try {
            log.info("Received message: {}", request);
            
            String chatId = request.getChatId();
            String message = request.getMessage();
            String token = request.getToken();
            
            log.info("Processing message - ChatId: {}, Message: {}, Token: {}", chatId, message, token);
            
            // Create chat if it doesn't exist
            if (chatId == null || chatId.trim().isEmpty()) {
                chatId = java.util.UUID.randomUUID().toString();
                chatService.createChat(chatId, request.getCustomerName(), request.getCustomerEmail());
                
                // Send chat created notification
                WebSocketMessage chatCreated = WebSocketMessage.createChatCreated(chatId);
                messagingTemplate.convertAndSend("/topic/chat", chatCreated);
            } else {
                // Update existing chat with customer info if provided
                if (request.getCustomerName() != null || request.getCustomerEmail() != null) {
                    chatService.createChat(chatId, request.getCustomerName(), request.getCustomerEmail());
                }
            }
            
            // Check if chat is assigned to support
            var assignment = chatAssignmentService.getChatAssignment(chatId);
            boolean isSupportChat = assignment.isPresent();
            
            // Also check if this is a support user sending a message (by token)
            boolean isSupportUser = false;
            if (token != null && !token.trim().isEmpty()) {
                User user = authService.getUserByToken(token);
                isSupportUser = (user != null && user.getRole().equals(User.UserRole.SUPPORT));
                log.info("Token check - User: {}, Role: {}, IsSupportUser: {}", 
                    user != null ? user.getUsername() : "null", 
                    user != null ? user.getRole() : "null", 
                    isSupportUser);
            }
            
            log.info("Chat routing - isSupportChat: {}, isSupportUser: {}", isSupportChat, isSupportUser);
            
            if (isSupportUser) {
                log.info("Routing to support handler - Support user message");
                // Handle support user message
                handleSupportMessage(chatId, message, token, assignment.orElse(null));
            } else {
                // This is a customer message - always handle as customer
                log.info("Routing to customer handler - Customer message");
                handleCustomerMessage(chatId, message, request);
                
                // Notify support about new chat activity
                notifySupportAboutChat(chatId, message);
            }
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            WebSocketMessage errorMessage = WebSocketMessage.createError("Error processing message: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
        }
    }
    
    private void handleCustomerMessage(String chatId, String message, ChatRequest request) {
        // Save user message
        chatService.saveUserMessage(chatId, message);
        
        // Send user message back to client
        WebSocketMessage userMessage = WebSocketMessage.createUserMessage(chatId, message);
        messagingTemplate.convertAndSend("/topic/chat", userMessage);
        
        // Check if chat is assigned to support - if yes, don't use AI
        var assignment = chatAssignmentService.getChatAssignment(chatId);
        if (assignment.isPresent()) {
            log.info("Chat {} is assigned to support, skipping AI response", chatId);
            return; // Don't use AI when support is active
        }
        
        // Get AI response using QuestionProcessingService if category is provided
        String aiResponse;
        if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
            // Use structured question processing
            QuestionRequest questionRequest = new QuestionRequest();
            questionRequest.setCategory(request.getCategory());
            questionRequest.setMessage(message);
            
            QuestionResponse questionResponse = questionProcessingService.processQuestion(questionRequest);
            aiResponse = questionResponse.getAnswer();
        } else {
            // Use general AI processing with chat history
            List<ChatMessage> chatHistory = chatService.getChatHistory(chatId);
            String historyContext = buildHistoryContext(chatHistory);
            aiResponse = aiService.processQuestion(message, historyContext);
        }
        
        // Check if AI response indicates need for human support
        if (shouldEscalateToSupport(aiResponse)) {
            // Assign chat to support
            var newAssignment = chatAssignmentService.assignChatToSupport(chatId);
            if (newAssignment != null) {
                WebSocketMessage escalationMessage = WebSocketMessage.createEscalationMessage(chatId, 
                    "Your request has been escalated to our support team. A support agent will join shortly.");
                messagingTemplate.convertAndSend("/topic/chat", escalationMessage);
                return;
            }
        }
        
        // Save AI message
        chatService.saveAiMessage(chatId, aiResponse);
        
        // Send AI response to client
        WebSocketMessage aiMessage = WebSocketMessage.createAiMessage(chatId, aiResponse);
        messagingTemplate.convertAndSend("/topic/chat", aiMessage);
    }
    
    private void handleSupportMessage(String chatId, String message, String token, ChatAssignment assignment) {
        // Verify support user token (only if token is provided)
        User user = null;
        if (token != null && !token.trim().isEmpty()) {
            user = authService.getUserByToken(token);
            if (user == null || !user.getRole().equals(User.UserRole.SUPPORT)) {
                WebSocketMessage errorMessage = WebSocketMessage.createError("Unauthorized access");
                messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
                return;
            }
        }
        
        // If no assignment exists and we have a support user, create assignment
        if (assignment == null && user != null) {
            assignment = chatAssignmentService.assignChatToSupport(chatId);
        }
        
        // Save support message
        String username = (user != null) ? user.getUsername() : "Support";
        chatService.saveSupportMessage(chatId, message, username);
        
        // Send support message to customer
        WebSocketMessage supportMessage = WebSocketMessage.createSupportMessage(chatId, message, username);
        messagingTemplate.convertAndSend("/topic/chat", supportMessage);
    }
    
    
    private void notifySupportAboutChat(String chatId, String message) {
        // Уведомляем техподдержку о новой активности в чате
        WebSocketMessage notification = new WebSocketMessage("CHAT_ACTIVITY", chatId, "SYSTEM", 
            "New message in chat: " + message.substring(0, Math.min(message.length(), 50)) + "...", 
            System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/support/activity", notification);
    }
    
    private boolean shouldEscalateToSupport(String aiResponse) {
        // Более простая логика для эскалации
        String lowerResponse = aiResponse.toLowerCase();
        return lowerResponse.contains("no answers found") ||
               lowerResponse.contains("cannot help") || 
               lowerResponse.contains("don't understand") ||
               lowerResponse.contains("contact support") ||
               lowerResponse.contains("human assistance") ||
               lowerResponse.contains("escalate");
    }
    
    @MessageMapping("/chat.join")
    public void joinChat(@Payload ChatRequest request) {
        try {
            String chatId = request.getChatId();
            
            if (chatId == null || chatId.trim().isEmpty()) {
                chatId = java.util.UUID.randomUUID().toString();
            }
            
            // Create or get existing chat
            chatService.createChat(chatId);
            
            // Send chat created notification
            WebSocketMessage chatCreated = WebSocketMessage.createChatCreated(chatId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, chatCreated);
            
            // Send chat history if exists
            List<ChatMessage> chatHistory = chatService.getChatHistory(chatId);
            if (!chatHistory.isEmpty()) {
                for (ChatMessage message : chatHistory) {
                    WebSocketMessage wsMessage;
                    if (message.getSenderType() == ChatMessage.SenderType.USER) {
                        wsMessage = WebSocketMessage.createUserMessage(chatId, message.getContent());
                    } else {
                        wsMessage = WebSocketMessage.createAiMessage(chatId, message.getContent());
                    }
                    messagingTemplate.convertAndSend("/topic/chat/" + chatId, wsMessage);
                }
            }
            
        } catch (Exception e) {
            log.error("Error joining chat", e);
            WebSocketMessage errorMessage = WebSocketMessage.createError("Error joining chat: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
        }
    }
    
    @MessageMapping("/chat.escalate")
    public void escalateChat(@Payload ChatRequest request) {
        try {
            String chatId = request.getChatId();
            if (chatId == null || chatId.trim().isEmpty()) {
                WebSocketMessage errorMessage = WebSocketMessage.createError("Chat ID is required for escalation.");
                messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
                return;
            }
            
            var assignment = chatAssignmentService.assignChatToSupport(chatId);
            if (assignment != null) {
                WebSocketMessage escalationMessage = WebSocketMessage.createEscalationMessage(chatId,
                    "Your request has been escalated to our support team. A support agent will join shortly.");
                messagingTemplate.convertAndSend("/topic/chat", escalationMessage);
                notifySupportAboutChat(chatId, "Customer requested escalation.");
            } else {
                WebSocketMessage errorMessage = WebSocketMessage.createError("No support agents available for escalation.");
                messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
            }
        } catch (Exception e) {
            log.error("Error escalating chat", e);
            WebSocketMessage errorMessage = WebSocketMessage.createError("Error escalating chat: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
        }
    }
    
    @MessageMapping("/chat.release")
    public void releaseChat(@Payload ChatRequest request) {
        try {
            String chatId = request.getChatId();
            String token = request.getToken();
            
            if (chatId == null || chatId.trim().isEmpty()) {
                WebSocketMessage errorMessage = WebSocketMessage.createError("Chat ID is required for release.");
                messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
                return;
            }
            
            // Verify support user token
            if (token != null && !token.trim().isEmpty()) {
                User user = authService.getUserByToken(token);
                if (user != null && user.getRole().equals(User.UserRole.SUPPORT)) {
                    // Release chat from support - resolve assignment
                    chatAssignmentService.resolveChat(chatId);
                    
                    WebSocketMessage releaseMessage = WebSocketMessage.createEscalationMessage(chatId,
                        "Support agent has left the chat. AI assistant is now available.");
                    messagingTemplate.convertAndSend("/topic/chat", releaseMessage);
                    
                    log.info("Chat {} released from support by user {}", chatId, user.getUsername());
                } else {
                    WebSocketMessage errorMessage = WebSocketMessage.createError("Unauthorized to release chat.");
                    messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
                }
            } else {
                WebSocketMessage errorMessage = WebSocketMessage.createError("Token required for chat release.");
                messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
            }
        } catch (Exception e) {
            log.error("Error releasing chat", e);
            WebSocketMessage errorMessage = WebSocketMessage.createError("Error releasing chat: " + e.getMessage());
            messagingTemplate.convertAndSend("/topic/chat/error", errorMessage);
        }
    }
    
    private String buildHistoryContext(List<ChatMessage> chatHistory) {
        if (chatHistory.isEmpty()) {
            return "";
        }
        
        return chatHistory.stream()
                .map(msg -> msg.getSenderType() + ": " + msg.getContent())
                .collect(Collectors.joining("\n"));
    }
}
