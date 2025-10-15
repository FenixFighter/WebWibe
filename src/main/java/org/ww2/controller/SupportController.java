package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ww2.dto.AuthResponse;
import org.ww2.entity.ChatAssignment;
import org.ww2.entity.ChatMessage;
import org.ww2.service.AuthService;
import org.ww2.service.ChatAssignmentService;
import org.ww2.service.ChatService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {
    
    private final AuthService authService;
    private final ChatAssignmentService chatAssignmentService;
    private final ChatService chatService;
    
    @GetMapping("/all-chats")
    public ResponseEntity<?> getAllChats(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            var user = authService.getUserByToken(cleanToken);
            
            if (user == null || !user.getRole().equals(org.ww2.entity.User.UserRole.SUPPORT)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            // Получаем все чаты из базы данных
            List<org.ww2.entity.Chat> allChats = chatService.getAllChats();
            
            List<ChatInfo> chatInfos = allChats.stream()
                .map(chat -> {
                    ChatInfo info = new ChatInfo();
                    info.setChatId(chat.getChatId());
                    info.setStatus("ACTIVE");
                    info.setAssignedAt(chat.getCreatedAt());
                    info.setCustomerName(chat.getCustomerName());
                    info.setCustomerEmail(chat.getCustomerEmail());
                    return info;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(chatInfos);
        } catch (Exception e) {
            log.error("Error getting all chats", e);
            return ResponseEntity.status(500).body("Error loading chats");
        }
    }
    
    @GetMapping("/assigned-chats")
    public ResponseEntity<?> getAssignedChats(@RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            var user = authService.getUserByToken(cleanToken);
            
            if (user == null || !user.getRole().equals(org.ww2.entity.User.UserRole.SUPPORT)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<ChatAssignment> assignments = chatAssignmentService.getAssignedChats(user.getId());
            
            List<ChatInfo> chatInfos = assignments.stream()
                .map(assignment -> {
                    ChatInfo info = new ChatInfo();
                    info.setChatId(assignment.getChatId());
                    info.setStatus(assignment.getStatus().name());
                    info.setAssignedAt(assignment.getAssignedAt());
                    return info;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(chatInfos);
        } catch (Exception e) {
            log.error("Error getting assigned chats", e);
            return ResponseEntity.status(500).body("Error loading chats");
        }
    }
    
    @GetMapping("/chat-history/{chatId}")
    public ResponseEntity<?> getChatHistory(@PathVariable String chatId, 
                                          @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            var user = authService.getUserByToken(cleanToken);
            
            if (user == null || !user.getRole().equals(org.ww2.entity.User.UserRole.SUPPORT)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            // Support can view any chat history (no assignment check needed)
            List<ChatMessage> messages = chatService.getChatHistory(chatId);
            
            List<MessageInfo> messageInfos = messages.stream()
                .map(message -> {
                    MessageInfo info = new MessageInfo();
                    info.setContent(message.getContent());
                    info.setSenderType(message.getSenderType().name());
                    info.setCreatedAt(message.getCreatedAt());
                    return info;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(messageInfos);
        } catch (Exception e) {
            log.error("Error getting chat history", e);
            return ResponseEntity.status(500).body("Error loading chat history");
        }
    }
    
    @PostMapping("/assign-chat/{chatId}")
    public ResponseEntity<?> assignChat(@PathVariable String chatId,
                                      @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            var user = authService.getUserByToken(cleanToken);
            
            if (user == null || !user.getRole().equals(org.ww2.entity.User.UserRole.SUPPORT)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            var assignment = chatAssignmentService.assignChatToSupport(chatId);
            if (assignment != null) {
                return ResponseEntity.ok("Chat assigned successfully");
            } else {
                return ResponseEntity.status(400).body("No available support staff");
            }
        } catch (Exception e) {
            log.error("Error assigning chat", e);
            return ResponseEntity.status(500).body("Error assigning chat");
        }
    }
    
    @PostMapping("/resolve-chat/{chatId}")
    public ResponseEntity<?> resolveChat(@PathVariable String chatId,
                                       @RequestHeader("Authorization") String token) {
        try {
            String cleanToken = token.replace("Bearer ", "");
            var user = authService.getUserByToken(cleanToken);
            
            if (user == null || !user.getRole().equals(org.ww2.entity.User.UserRole.SUPPORT)) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            chatAssignmentService.resolveChat(chatId);
            return ResponseEntity.ok("Chat resolved successfully");
        } catch (Exception e) {
            log.error("Error resolving chat", e);
            return ResponseEntity.status(500).body("Error resolving chat");
        }
    }
    
    // DTOs for response
    public static class ChatInfo {
        private String chatId;
        private String status;
        private java.time.LocalDateTime assignedAt;
        private String customerName;
        private String customerEmail;
        
        // Getters and setters
        public String getChatId() { return chatId; }
        public void setChatId(String chatId) { this.chatId = chatId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getAssignedAt() { return assignedAt; }
        public void setAssignedAt(java.time.LocalDateTime assignedAt) { this.assignedAt = assignedAt; }
        public String getCustomerName() { return customerName; }
        public void setCustomerName(String customerName) { this.customerName = customerName; }
        public String getCustomerEmail() { return customerEmail; }
        public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    }
    
    public static class MessageInfo {
        private String content;
        private String senderType;
        private java.time.LocalDateTime createdAt;
        
        // Getters and setters
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getSenderType() { return senderType; }
        public void setSenderType(String senderType) { this.senderType = senderType; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
