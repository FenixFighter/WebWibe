package org.ww2.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.ww2.entity.User;
import org.ww2.service.SupportService;
import org.ww2.service.SupportService.ChatInfo;
import org.ww2.service.SupportService.MessageInfo;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
@Slf4j
public class SupportController {
    
    private final SupportService supportService;
    
    @GetMapping("/all-chats")
    public ResponseEntity<?> getAllChats(@RequestHeader("Authorization") String token) {
        try {
            User user = supportService.validateSupportUser(token);
            if (user == null) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<ChatInfo> chatInfos = supportService.getAllChats();
            return ResponseEntity.ok(chatInfos);
        } catch (Exception e) {
            log.error("Error getting all chats", e);
            return ResponseEntity.status(500).body("Error loading chats");
        }
    }
    
    @GetMapping("/assigned-chats")
    public ResponseEntity<?> getAssignedChats(@RequestHeader("Authorization") String token) {
        try {
            User user = supportService.validateSupportUser(token);
            if (user == null) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<ChatInfo> chatInfos = supportService.getAssignedChats(user.getId());
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
            User user = supportService.validateSupportUser(token);
            if (user == null) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            List<MessageInfo> messageInfos = supportService.getChatHistory(chatId);
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
            User user = supportService.validateSupportUser(token);
            if (user == null) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            var assignment = supportService.assignChatToSupport(chatId);
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
            User user = supportService.validateSupportUser(token);
            if (user == null) {
                return ResponseEntity.status(403).body("Access denied");
            }
            
            supportService.resolveChat(chatId);
            return ResponseEntity.ok("Chat resolved successfully");
        } catch (Exception e) {
            log.error("Error resolving chat", e);
            return ResponseEntity.status(500).body("Error resolving chat");
        }
    }
}
