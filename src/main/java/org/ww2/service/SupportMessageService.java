package org.ww2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.ww2.dto.WebSocketMessage;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.User;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupportMessageService {

    private final ChatService chatService;
    private final ChatAssignmentService chatAssignmentService;
    private final AuthService authService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Handles support user message
     */
    public void handleSupportMessage(String chatId, String message, String token, ChatAssignment assignment) {
        // Verify support user token (only if token is provided)
        User user = null;
        if (token != null && !token.trim().isEmpty()) {
            user = authService.getUserByToken(token);
            if (user == null || !user.getRole().equals(User.UserRole.SUPPORT)) {
                sendErrorMessage("Unauthorized access");
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
        
        log.info("Support message sent by {} for chat {}", username, chatId);
    }

    /**
     * Handles chat escalation
     */
    public void handleChatEscalation(String chatId) {
        var assignment = chatAssignmentService.assignChatToSupport(chatId);
        if (assignment != null) {
            WebSocketMessage escalationMessage = WebSocketMessage.createEscalationMessage(chatId,
                "Your request has been escalated to our support team. A support agent will join shortly.");
            messagingTemplate.convertAndSend("/topic/chat", escalationMessage);
            log.info("Chat {} escalated to support", chatId);
        } else {
            sendErrorMessage("No support agents available for escalation.");
        }
    }

    /**
     * Handles chat release
     */
    public void handleChatRelease(String chatId, String token) {
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
                sendErrorMessage("Unauthorized to release chat.");
            }
        } else {
            sendErrorMessage("Token required for chat release.");
        }
    }

    /**
     * Sends error message to client
     */
    private void sendErrorMessage(String errorMessage) {
        WebSocketMessage error = WebSocketMessage.createError(errorMessage);
        messagingTemplate.convertAndSend("/topic/chat/error", error);
    }
}
